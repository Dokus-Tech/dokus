package ai.dokus.peppol.backend.mapper

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.CreateBillRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.RecommandInvoiceDocument
import ai.dokus.foundation.domain.model.RecommandLineItem
import ai.dokus.foundation.domain.model.RecommandParty
import ai.dokus.foundation.domain.model.RecommandPaymentMeans
import ai.dokus.foundation.domain.model.RecommandReceivedDocument
import ai.dokus.foundation.domain.model.RecommandSendRequest
import ai.dokus.foundation.domain.model.TenantSettings
import kotlinx.datetime.LocalDate
import org.slf4j.LoggerFactory

/**
 * Helper class for parsed address components.
 * Used internally for parsing free-text addresses into structured parts.
 */
private data class ParsedAddressComponents(
    val street: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?
)

/**
 * Maps between domain models and Recommand API models.
 */
class PeppolMapper {
    private val logger = LoggerFactory.getLogger(PeppolMapper::class.java)

    /**
     * Convert an invoice to Recommand send request.
     *
     * @param invoice The invoice to convert
     * @param client The client (buyer) information
     * @param tenantSettings The tenant settings (seller information)
     * @param peppolSettings The Peppol settings with sender Peppol ID
     * @return The Recommand send request
     */
    fun toRecommandSendRequest(
        invoice: FinancialDocumentDto.InvoiceDto,
        client: ClientDto,
        tenantSettings: TenantSettings,
        peppolSettings: PeppolSettingsDto
    ): RecommandSendRequest {
        val recipientPeppolId = client.peppolId
            ?: throw IllegalArgumentException("Client must have a Peppol ID to send via Peppol")

        val invoiceDocument = RecommandInvoiceDocument(
            invoiceNumber = invoice.invoiceNumber.value,
            issueDate = invoice.issueDate.toString(),
            dueDate = invoice.dueDate.toString(),
            buyer = toRecommandParty(client),
            seller = toSellerParty(tenantSettings),
            lineItems = invoice.items.mapIndexed { index, item ->
                toRecommandLineItem(item, index + 1)
            },
            note = invoice.notes,
            buyerReference = determineBuyerReference(client, invoice),
            paymentMeans = toPaymentMeans(tenantSettings),
            documentCurrencyCode = invoice.currency.dbValue
        )

        return RecommandSendRequest(
            recipient = recipientPeppolId,
            documentType = "invoice",
            document = invoiceDocument
        )
    }

    /**
     * Convert client to Recommand party (buyer).
     */
    private fun toRecommandParty(client: ClientDto): RecommandParty {
        return RecommandParty(
            vatNumber = client.vatNumber?.value,
            name = client.name.value,
            streetName = client.addressLine1,
            cityName = client.city,
            postalZone = client.postalCode,
            countryCode = client.country,
            contactEmail = client.email?.value,
            contactName = client.contactPerson
        )
    }

    /**
     * Convert tenant settings to Recommand party (seller).
     * Parses city and postal code from companyAddress if possible.
     */
    private fun toSellerParty(settings: TenantSettings): RecommandParty {
        val parsedAddress = parseCompanyAddress(settings.companyAddress)

        return RecommandParty(
            vatNumber = settings.companyVatNumber?.value,
            name = settings.companyName ?: "Unknown",
            streetName = parsedAddress.street,
            cityName = parsedAddress.city,
            postalZone = parsedAddress.postalCode,
            countryCode = parsedAddress.country ?: "BE",  // Default to Belgium
            contactEmail = null,
            contactName = null
        )
    }

    /**
     * Parse a company address string into structured components.
     * Supports common Belgian/European address formats.
     */
    private fun parseCompanyAddress(address: String?): ParsedAddressComponents {
        if (address.isNullOrBlank()) {
            return ParsedAddressComponents(null, null, null, null)
        }

        // Normalize line breaks and multiple spaces
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
            normalized in listOf("BELGIUM", "BELGIQUE", "BELGIË", "BELGIE") -> "BE"
            normalized in listOf("NETHERLANDS", "NEDERLAND", "PAYS-BAS") -> "NL"
            normalized in listOf("FRANCE", "FRANKRIJK") -> "FR"
            normalized in listOf("GERMANY", "DEUTSCHLAND", "ALLEMAGNE", "DUITSLAND") -> "DE"
            normalized in listOf("LUXEMBOURG", "LUXEMBURG") -> "LU"
            else -> country.take(2).uppercase()
        }
    }

    /**
     * Determine the buyer reference for Peppol.
     * Uses client's company number, VAT number, or invoice reference.
     */
    private fun determineBuyerReference(
        client: ClientDto,
        invoice: FinancialDocumentDto.InvoiceDto
    ): String? {
        // Priority: company number > VAT number > invoice number as fallback
        return client.companyNumber
            ?: client.vatNumber?.value
            ?: invoice.invoiceNumber.value.takeIf { it.length <= 20 }
    }

    /**
     * Convert invoice item to Recommand line item.
     */
    private fun toRecommandLineItem(item: InvoiceItemDto, lineNumber: Int): RecommandLineItem {
        val vatCategory = mapVatRateToCategory(item.vatRate)
        val vatPercent = item.vatRate.value.toDoubleOrNull() ?: 0.0

        return RecommandLineItem(
            id = lineNumber.toString(),
            name = item.description,
            description = item.description,
            quantity = item.quantity,
            unitCode = "C62",  // Unit (default). Use HUR for hours, DAY for days
            unitPrice = item.unitPrice.value.toDoubleOrNull() ?: 0.0,
            lineTotal = item.lineTotal.value.toDoubleOrNull() ?: 0.0,
            taxCategory = vatCategory,
            taxPercent = vatPercent
        )
    }

    /**
     * Map VAT rate to Peppol tax category code.
     */
    private fun mapVatRateToCategory(vatRate: VatRate): String {
        val rateValue = vatRate.value.toDoubleOrNull() ?: 0.0
        return when {
            rateValue == 0.0 -> "Z"  // Zero rated
            vatRate.value == "0.00" -> "E"  // Exempt (could also be Z, depends on context)
            else -> "S"  // Standard rate
        }
    }

    /**
     * Convert tenant settings to payment means.
     */
    private fun toPaymentMeans(settings: TenantSettings): RecommandPaymentMeans? {
        val iban = settings.companyIban ?: return null

        return RecommandPaymentMeans(
            iban = iban.value,
            bic = settings.companyBic?.value,
            paymentMeansCode = "30",  // Credit transfer
            paymentId = null
        )
    }

    /**
     * Convert a received Peppol document to a CreateBillRequest.
     *
     * @param document The received document from Recommand
     * @param senderPeppolId The sender's Peppol ID
     * @return CreateBillRequest for creating a bill in the system
     */
    fun toCreateBillRequest(
        document: RecommandReceivedDocument,
        senderPeppolId: String
    ): CreateBillRequest {
        val seller = document.seller
        val totals = document.legalMonetaryTotal
        val taxTotal = document.taxTotal

        // Parse dates
        val issueDate = document.issueDate?.let { parseDate(it) } ?: LocalDate.fromEpochDays(0)
        val dueDate = document.dueDate?.let { parseDate(it) } ?: issueDate

        // Calculate amounts
        val amount = totals?.payableAmount?.let { Money(it.toString()) }
            ?: totals?.taxInclusiveAmount?.let { Money(it.toString()) }
            ?: Money.ZERO

        val vatAmount = taxTotal?.taxAmount?.let { Money(it.toString()) }

        // Determine VAT rate from first line item or tax subtotal
        val vatRate = taxTotal?.taxSubtotals?.firstOrNull()?.taxPercent?.let { percent ->
            VatRate(percent.toString())
        }

        // Infer category from document content (default to ProfessionalServices)
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
            mediaId = null  // Will be linked later if document has attachments
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
     * This is a simple heuristic - in production, this could use ML or more sophisticated rules.
     */
    private fun inferCategory(document: RecommandReceivedDocument): ExpenseCategory {
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
