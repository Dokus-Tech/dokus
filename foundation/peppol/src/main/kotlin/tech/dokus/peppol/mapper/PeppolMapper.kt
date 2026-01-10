package tech.dokus.peppol.mapper

import kotlinx.datetime.LocalDate
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.CreateBillRequest
import tech.dokus.domain.model.ExtractedBillFields
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.peppol.model.PeppolDocumentType
import tech.dokus.peppol.model.PeppolInvoiceData
import tech.dokus.peppol.model.PeppolLineItem
import tech.dokus.peppol.model.PeppolParty
import tech.dokus.peppol.model.PeppolPaymentInfo
import tech.dokus.peppol.model.PeppolReceivedDocument
import tech.dokus.peppol.model.PeppolSendRequest

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
     */
    fun toSendRequest(
        invoice: FinancialDocumentDto.InvoiceDto,
        contact: ContactDto,
        tenant: Tenant,
        tenantSettings: TenantSettings,
        peppolSettings: PeppolSettingsDto,
        companyAddress: Address?
    ): PeppolSendRequest {
        val recipientPeppolId = contact.peppolId
            ?: throw IllegalArgumentException("Contact must have a Peppol ID to send via Peppol")

        return PeppolSendRequest(
            recipientPeppolId = recipientPeppolId,
            documentType = PeppolDocumentType.INVOICE,
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
    private fun toLineItem(item: InvoiceItemDto, lineNumber: Int): PeppolLineItem {
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
     * Convert a received Peppol document to a CreateBillRequest.
     */
    fun toCreateBillRequest(
        document: PeppolReceivedDocument,
        senderPeppolId: String
    ): CreateBillRequest {
        val seller = document.seller
        val totals = document.totals
        val taxTotal = document.taxTotal

        // Parse dates
        val issueDate = document.issueDate?.let { parseDate(it) } ?: LocalDate.fromEpochDays(0)
        val dueDate = document.dueDate?.let { parseDate(it) } ?: issueDate

        // Calculate amounts
        val amount = totals?.payableAmount?.let { Money.fromDouble(it) }
            ?: totals?.taxInclusiveAmount?.let { Money.fromDouble(it) }
            ?: Money.ZERO

        val vatAmount = taxTotal?.taxAmount?.let { Money.fromDouble(it) }

        // Determine VAT rate from tax subtotals (percent is in %, e.g. 21.00 for 21%)
        val vatRate = taxTotal?.taxSubtotals?.firstOrNull()?.taxPercent?.let { percent ->
            VatRate.parse(percent.toString())
        }

        // Infer category from document content
        val category = inferCategory(document)

        return CreateBillRequest(
            supplierName = seller?.name ?: "Unknown Supplier",
            supplierVatNumber = seller?.vatNumber,
            invoiceNumber = document.invoiceNumber,
            issueDate = issueDate,
            dueDate = dueDate,
            amount = amount,
            vatAmount = vatAmount,
            vatRate = vatRate,
            category = category,
            description = document.note,
            notes = "Received via Peppol from $senderPeppolId",
            documentId = null
        )
    }

    /**
     * Convert a received Peppol document to ExtractedDocumentData.
     * Used when creating Documents+Drafts from Peppol inbox (architectural boundary).
     */
    fun toExtractedDocumentData(
        document: PeppolReceivedDocument,
        senderPeppolId: String
    ): ExtractedDocumentData {
        val seller = document.seller
        val totals = document.totals
        val taxTotal = document.taxTotal

        // Parse dates
        val issueDate = document.issueDate?.let { parseDate(it) }
        val dueDate = document.dueDate?.let { parseDate(it) } ?: issueDate

        // Calculate amounts
        val amount = totals?.payableAmount?.let { Money.fromDouble(it) }
            ?: totals?.taxInclusiveAmount?.let { Money.fromDouble(it) }

        val vatAmount = taxTotal?.taxAmount?.let { Money.fromDouble(it) }

        // Determine VAT rate from tax subtotals
        val vatRate = taxTotal?.taxSubtotals?.firstOrNull()?.taxPercent?.let { percent ->
            VatRate.parse(percent.toString())
        }

        // Infer category from document content
        val category = inferCategory(document)

        return ExtractedDocumentData(
            documentType = DocumentType.Bill,
            bill = ExtractedBillFields(
                supplierName = seller?.name,
                supplierVatNumber = seller?.vatNumber,
                invoiceNumber = document.invoiceNumber,
                issueDate = issueDate,
                dueDate = dueDate,
                amount = amount,
                vatAmount = vatAmount,
                vatRate = vatRate,
                category = category,
                description = document.note,
                notes = "Received via Peppol from $senderPeppolId"
            )
        )
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

    /**
     * Infer expense category from document content.
     */
    private fun inferCategory(document: PeppolReceivedDocument): ExpenseCategory {
        val keywords = (document.seller?.name ?: "") +
            (document.note ?: "") +
            (document.lineItems?.joinToString(" ") { it.name ?: "" } ?: "")

        val keywordsLower = keywords.lowercase()

        return when {
            keywordsLower.contains("software") || keywordsLower.contains("license") -> ExpenseCategory.Software
            keywordsLower.contains("hosting") || keywordsLower.contains("cloud") -> ExpenseCategory.Software
            keywordsLower.contains("travel") || keywordsLower.contains("flight") || keywordsLower.contains("hotel") -> ExpenseCategory.Travel
            keywordsLower.contains("telecom") || keywordsLower.contains("phone") || keywordsLower.contains("internet") -> ExpenseCategory.Telecommunications
            keywordsLower.contains("office") || keywordsLower.contains("supplies") -> ExpenseCategory.OfficeSupplies
            keywordsLower.contains("hardware") || keywordsLower.contains("computer") || keywordsLower.contains("laptop") -> ExpenseCategory.Hardware
            keywordsLower.contains("insurance") -> ExpenseCategory.Insurance
            keywordsLower.contains("rent") || keywordsLower.contains("lease") -> ExpenseCategory.Rent
            keywordsLower.contains("marketing") || keywordsLower.contains("advertising") -> ExpenseCategory.Marketing
            keywordsLower.contains("consulting") || keywordsLower.contains("professional") || keywordsLower.contains("legal") || keywordsLower.contains("accounting") -> ExpenseCategory.ProfessionalServices
            else -> ExpenseCategory.Other
        }
    }
}
