package ai.dokus.peppol.mapper

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.TenantSettings
import ai.dokus.peppol.model.PeppolDocumentType
import ai.dokus.peppol.model.PeppolInvoiceData
import ai.dokus.peppol.model.PeppolLineItem
import ai.dokus.peppol.model.PeppolParty
import ai.dokus.peppol.model.PeppolPaymentInfo
import ai.dokus.peppol.model.PeppolReceivedDocument
import ai.dokus.peppol.model.PeppolSendRequest
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory

/**
 * Maps between domain models and provider-agnostic Peppol models.
 *
 * This mapper handles conversion from Dokus domain models (Invoice, Client, etc.)
 * to the generic Peppol format that providers can then convert to their specific format.
 */
class PeppolMapper {
    private val logger = LoggerFactory.getLogger(PeppolMapper::class.java)

    /**
     * Convert a domain invoice to a Peppol send request.
     */
    fun toSendRequest(
        invoice: FinancialDocumentDto.InvoiceDto,
        contact: ContactDto,
        tenantSettings: TenantSettings,
        peppolSettings: PeppolSettingsDto
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
                seller = toSellerParty(tenantSettings),
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
     */
    private fun toBuyerParty(contact: ContactDto): PeppolParty {
        return PeppolParty(
            name = contact.name.value,
            vatNumber = contact.vatNumber?.value,
            streetName = contact.addressLine1,
            cityName = contact.city,
            postalZone = contact.postalCode,
            countryCode = contact.country,
            contactEmail = contact.email?.value,
            contactName = contact.contactPerson,
            companyNumber = contact.companyNumber
        )
    }

    /**
     * Convert tenant settings to Peppol seller party.
     */
    private fun toSellerParty(settings: TenantSettings): PeppolParty {
        val parsedAddress = parseCompanyAddress(settings.companyAddress)

        return PeppolParty(
            name = settings.companyName ?: "Unknown",
            vatNumber = settings.companyVatNumber?.value,
            streetName = parsedAddress.street,
            cityName = parsedAddress.city,
            postalZone = parsedAddress.postalCode,
            countryCode = parsedAddress.country ?: "BE"
        )
    }

    /**
     * Convert invoice item to Peppol line item.
     */
    private fun toLineItem(item: InvoiceItemDto, lineNumber: Int): PeppolLineItem {
        val vatCategory = mapVatRateToCategory(item.vatRate)
        val vatPercent = item.vatRate.value.toDoubleOrNull() ?: 0.0

        return PeppolLineItem(
            id = lineNumber.toString(),
            name = item.description,
            description = item.description,
            quantity = item.quantity,
            unitCode = "C62",  // Unit. Use HUR for hours, DAY for days
            unitPrice = item.unitPrice.value.toDoubleOrNull() ?: 0.0,
            lineTotal = item.lineTotal.value.toDoubleOrNull() ?: 0.0,
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
            paymentMeansCode = "30",  // Credit transfer
            paymentId = null
        )
    }

    /**
     * Map VAT rate to Peppol tax category code.
     */
    private fun mapVatRateToCategory(vatRate: VatRate): String {
        val rateValue = vatRate.value.toDoubleOrNull() ?: 0.0
        return when {
            rateValue == 0.0 -> "Z"  // Zero rated
            vatRate.value == "0.00" -> "E"  // Exempt
            else -> "S"  // Standard rate
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
        val amount = totals?.payableAmount?.let { Money(it.toString()) }
            ?: totals?.taxInclusiveAmount?.let { Money(it.toString()) }
            ?: Money.ZERO

        val vatAmount = taxTotal?.taxAmount?.let { Money(it.toString()) }

        // Determine VAT rate from tax subtotals
        val vatRate = taxTotal?.taxSubtotals?.firstOrNull()?.taxPercent?.let { percent ->
            VatRate(percent.toString())
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

    // ========================================================================
    // ADDRESS PARSING
    // ========================================================================

    private data class ParsedAddressComponents(
        val street: String?,
        val city: String?,
        val postalCode: String?,
        val country: String?
    )

    private fun parseCompanyAddress(address: String?): ParsedAddressComponents {
        if (address.isNullOrBlank()) {
            return ParsedAddressComponents(null, null, null, null)
        }

        val normalized = address.replace("\n", ", ").replace(Regex("\\s+"), " ").trim()
        val parts = normalized.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return when {
            parts.size >= 3 -> {
                val (postalCode, city) = parsePostalCodeAndCity(parts[1])
                ParsedAddressComponents(parts[0], city, postalCode, parseCountryCode(parts.getOrNull(2)))
            }
            parts.size == 2 -> {
                val (postalCode, city) = parsePostalCodeAndCity(parts[1])
                ParsedAddressComponents(parts[0], city, postalCode, null)
            }
            else -> {
                val (postalCode, city) = parsePostalCodeAndCity(parts[0])
                if (postalCode != null) {
                    ParsedAddressComponents(null, city, postalCode, null)
                } else {
                    ParsedAddressComponents(parts[0], null, null, null)
                }
            }
        }
    }

    private fun parsePostalCodeAndCity(text: String): Pair<String?, String?> {
        val pattern = Regex("^(\\d{4,5}(?:\\s?[A-Z]{2})?)\\s+(.+)$")
        val match = pattern.find(text.trim())
        return if (match != null) {
            Pair(match.groupValues[1], match.groupValues[2])
        } else {
            Pair(null, text.takeIf { it.isNotBlank() })
        }
    }

    private fun parseCountryCode(country: String?): String? {
        if (country.isNullOrBlank()) return null
        val normalized = country.trim().uppercase()
        return when {
            normalized.length == 2 -> normalized
            normalized in listOf("BELGIUM", "BELGIQUE", "BELGIE") -> "BE"
            normalized in listOf("NETHERLANDS", "NEDERLAND", "PAYS-BAS") -> "NL"
            normalized in listOf("FRANCE", "FRANKRIJK") -> "FR"
            normalized in listOf("GERMANY", "DEUTSCHLAND", "ALLEMAGNE", "DUITSLAND") -> "DE"
            normalized in listOf("LUXEMBOURG", "LUXEMBURG") -> "LU"
            else -> country.take(2).uppercase()
        }
    }
}
