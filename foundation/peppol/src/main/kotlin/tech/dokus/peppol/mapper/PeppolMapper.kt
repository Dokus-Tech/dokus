package tech.dokus.peppol.mapper

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.database.entity.InvoiceEntity
import tech.dokus.database.entity.InvoiceItemEntity
import tech.dokus.domain.model.FinancialLineItemDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraftDto
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.VatBreakdownEntryDto
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.model.PeppolInvoiceData
import tech.dokus.peppol.model.PeppolLineItem
import tech.dokus.peppol.model.PeppolParty
import tech.dokus.peppol.model.PeppolPaymentInfo
import tech.dokus.peppol.model.PeppolReceivedDocument
import tech.dokus.peppol.model.PeppolSendRequest
import kotlin.math.roundToInt

/**
 * Maps between domain models and provider-agnostic Peppol models.
 *
 * This mapper handles conversion from Dokus domain models (Invoice, Client, etc.)
 * to the generic Peppol format that providers can then convert to their specific format.
 */
class PeppolMapper {
    private val logger = loggerFor()

    /**
     * Convert a domain invoice to a Peppol send request.
     * NOTE: recipientPeppolId should be resolved via PeppolRecipientResolver before calling this.
     */
    fun toSendRequest(
        invoice: InvoiceEntity,
        contact: ContactDto,
        tenant: Tenant,
        tenantSettings: TenantSettings,
        peppolSettings: PeppolSettingsDto,
        companyAddress: Address?,
        recipientPeppolId: String
    ): PeppolSendRequest {
        return PeppolSendRequest(
            recipientPeppolId = recipientPeppolId,
            documentType = PeppolDocumentType.Invoice,
            invoice = PeppolInvoiceData(
                invoiceNumber = invoice.invoiceNumber.value,
                issueDate = invoice.issueDate,
                dueDate = invoice.dueDate,
                seller = toSellerParty(tenant, tenantSettings, companyAddress),
                buyer = toBuyerParty(contact),
                lineItems = invoice.items.mapIndexed { index, item ->
                    toLineItem(item, index + 1)
                },
                currencyCode = invoice.currency.dbValue,
                note = invoice.notes,
                paymentInfo = toPaymentInfo(tenantSettings)
            )
        )
    }

    /**
     * Convert contact to Peppol buyer party.
     * Uses the first/default address from contact.addresses list.
     */
    private fun toBuyerParty(contact: ContactDto): PeppolParty {
        // Get first address (default) from contact's addresses list
        val address = contact.addresses.firstOrNull()?.address
        return PeppolParty(
            name = contact.name.value,
            vatNumber = contact.vatNumber?.value,
            streetName = address?.streetLine1,
            cityName = address?.city,
            postalZone = address?.postalCode,
            countryCode = address?.country,
            contactEmail = contact.email?.value,
            contactName = contact.contactPerson,
            companyNumber = contact.companyNumber
        )
    }

    /**
     * Convert tenant settings to Peppol seller party.
     */
    private fun toSellerParty(tenant: Tenant, settings: TenantSettings, companyAddress: Address?): PeppolParty {
        return PeppolParty(
            name = settings.companyName ?: tenant.displayName.value,
            vatNumber = tenant.vatNumber?.value,
            streetName = companyAddress?.streetLine1,
            cityName = companyAddress?.city,
            postalZone = companyAddress?.postalCode,
            countryCode = companyAddress?.country  // Already ISO-2 string
        )
    }

    /**
     * Convert invoice item to Peppol line item.
     */
    private fun toLineItem(item: InvoiceItemEntity, lineNumber: Int): PeppolLineItem {
        val vatCategory = mapVatRateToCategory(item.vatRate)
        val vatPercent = item.vatRate.toPercentDouble()

        return PeppolLineItem(
            id = lineNumber.toString(),
            name = item.description,
            description = item.description,
            quantity = item.quantity,
            unitCode = "C62", // Unit. Use HUR for hours, DAY for days
            unitPrice = item.unitPrice.toDouble(),
            lineTotal = item.lineTotal.toDouble(),
            taxCategory = vatCategory,
            taxPercent = vatPercent
        )
    }

    /**
     * Convert tenant settings to payment info.
     */
    private fun toPaymentInfo(settings: TenantSettings): PeppolPaymentInfo? {
        val iban = settings.companyIban ?: return null

        return PeppolPaymentInfo(
            iban = iban.value,
            bic = settings.companyBic?.value,
            paymentMeansCode = "30", // Credit transfer
            paymentId = null
        )
    }

    /**
     * Map VAT rate to Peppol tax category code.
     */
    private fun mapVatRateToCategory(vatRate: VatRate): String {
        return when {
            vatRate.basisPoints == 0 -> "Z" // Zero rated
            else -> "S" // Standard rate
        }
    }

    // ========================================================================
    // RECEIVED DOCUMENT MAPPING
    // ========================================================================

    /**
     * Convert a received Peppol document to normalized draft data.
     * Used when creating Documents+Drafts from Peppol inbox (architectural boundary).
     *
     * Maps by document type:
     * - invoice -> InvoiceDraftData(direction=Inbound)
     * - creditNote -> CreditNoteDraftData(direction=Inbound) (supplier crediting us)
     * - selfBillingInvoice -> InvoiceDraftData (client created invoice on our behalf)
     * - selfBillingCreditNote -> CreditNoteDraftData(direction=Outbound) (client corrects self-billing)
     */
    @Suppress("CyclomaticComplexMethod")
    fun toDraftData(
        document: PeppolReceivedDocument,
        senderPeppolId: String
    ): DocumentDraftData {
        val seller = document.seller
        val buyer = document.buyer
        val totals = document.totals
        val taxTotal = document.taxTotal

        // Parse dates
        val issueDate = document.issueDate?.let { parseDate(it) }
        val dueDate = document.dueDate?.let { parseDate(it) } ?: issueDate

        // Amounts
        val currency = Currency.from(document.currencyCode)
        val totalAmount = totals?.payableAmount?.let { Money.fromDouble(it, currency) }
            ?: totals?.taxInclusiveAmount?.let { Money.fromDouble(it, currency) }
        val vatAmount = taxTotal?.taxAmount?.let { Money.fromDouble(it, currency) }
        val subtotalAmount = totals?.taxExclusiveAmount?.let { Money.fromDouble(it, currency) }
            ?: if (totalAmount != null && vatAmount != null) totalAmount - vatAmount else null

        val lineItems = document.lineItems.orEmpty().mapNotNull { line ->
            val description = line.description ?: line.name ?: return@mapNotNull null
            val quantity = line.quantity?.takeIf { it % 1.0 == 0.0 }?.toLong()
            val unitPrice = line.unitPrice?.let { Money.fromDouble(it, currency).minor }
            val netAmount = line.lineTotal?.let { Money.fromDouble(it, currency).minor }
            val vatRate = line.taxPercent?.let { (it * 100).roundToInt() }

            FinancialLineItemDto(
                description = description,
                quantity = quantity,
                unitPrice = unitPrice,
                vatRate = vatRate,
                netAmount = netAmount
            )
        }

        val vatBreakdown = taxTotal?.taxSubtotals.orEmpty().mapNotNull { subtotal ->
            val rate = subtotal.taxPercent?.let { (it * 100).roundToInt() } ?: return@mapNotNull null
            val base = subtotal.taxableAmount?.let { Money.fromDouble(it, currency).minor } ?: return@mapNotNull null
            val amount = subtotal.taxAmount?.let { Money.fromDouble(it, currency).minor } ?: return@mapNotNull null
            VatBreakdownEntryDto(
                rate = rate,
                base = base,
                amount = amount
            )
        }

        val notes = document.note ?: "Received via Peppol from $senderPeppolId"

        return when (document.documentType) {
            PeppolDocumentType.CreditNote -> CreditNoteDraftData(
                creditNoteNumber = document.invoiceNumber,
                direction = DocumentDirection.Inbound,
                issueDate = issueDate,
                currency = currency,
                subtotalAmount = subtotalAmount,
                vatAmount = vatAmount,
                totalAmount = totalAmount,
                lineItems = lineItems,
                vatBreakdown = vatBreakdown,
                originalInvoiceNumber = null,
                reason = document.note,
                notes = notes,
                seller = PartyDraftDto(
                    name = seller?.name,
                    vat = VatNumber.from(seller?.vatNumber),
                ),
                buyer = PartyDraftDto(
                    name = buyer?.name,
                    vat = VatNumber.from(buyer?.vatNumber),
                )
            )

            PeppolDocumentType.SelfBillingInvoice -> InvoiceDraftData(
                direction = DocumentDirection.Outbound,
                invoiceNumber = document.invoiceNumber,
                issueDate = issueDate,
                dueDate = dueDate,
                currency = currency,
                subtotalAmount = subtotalAmount,
                vatAmount = vatAmount,
                totalAmount = totalAmount,
                lineItems = lineItems,
                vatBreakdown = vatBreakdown,
                notes = notes,
                seller = PartyDraftDto(
                    name = seller?.name,
                    vat = VatNumber.from(seller?.vatNumber),
                ),
                buyer = PartyDraftDto(
                    name = buyer?.name,
                    vat = VatNumber.from(buyer?.vatNumber),
                )
            )

            PeppolDocumentType.SelfBillingCreditNote -> CreditNoteDraftData(
                creditNoteNumber = document.invoiceNumber,
                direction = DocumentDirection.Outbound,
                issueDate = issueDate,
                currency = currency,
                subtotalAmount = subtotalAmount,
                vatAmount = vatAmount,
                totalAmount = totalAmount,
                lineItems = lineItems,
                vatBreakdown = vatBreakdown,
                originalInvoiceNumber = null,
                reason = document.note,
                notes = notes,
                seller = PartyDraftDto(
                    name = seller?.name,
                    vat = VatNumber.from(seller?.vatNumber),
                ),
                buyer = PartyDraftDto(
                    name = buyer?.name,
                    vat = VatNumber.from(buyer?.vatNumber),
                )
            )

            // Invoice, Xml — default to inbound invoice
            else -> InvoiceDraftData(
                direction = DocumentDirection.Inbound,
                invoiceNumber = document.invoiceNumber,
                issueDate = issueDate,
                dueDate = dueDate,
                currency = currency,
                subtotalAmount = subtotalAmount,
                vatAmount = vatAmount,
                totalAmount = totalAmount,
                lineItems = lineItems,
                vatBreakdown = vatBreakdown,
                iban = null,
                payment = null,
                notes = notes,
                seller = PartyDraftDto(
                    name = seller?.name,
                    vat = VatNumber.from(seller?.vatNumber),
                ),
                buyer = PartyDraftDto(
                    name = buyer?.name,
                    vat = VatNumber.from(buyer?.vatNumber),
                )
            )
        }
    }

    /**
     * Parse ISO date string to LocalDate.
     */
    private fun parseDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr)
        } catch (e: Exception) {
            logger.warn("Failed to parse date: $dateStr", e)
            null
        }
    }

}
