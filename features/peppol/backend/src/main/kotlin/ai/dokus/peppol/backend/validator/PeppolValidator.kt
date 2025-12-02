package ai.dokus.peppol.backend.validator

import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.PeppolValidationError
import ai.dokus.foundation.domain.model.PeppolValidationResult
import ai.dokus.foundation.domain.model.PeppolValidationWarning
import ai.dokus.foundation.domain.model.TenantSettings
import org.slf4j.LoggerFactory

/**
 * Validates invoices against Peppol BIS Billing 3.0 requirements.
 *
 * Validation rules based on:
 * - EN 16931 (European e-invoicing standard)
 * - Peppol BIS Billing 3.0
 * - Belgian CIUS (Core Invoice Usage Specification)
 */
class PeppolValidator {
    private val logger = LoggerFactory.getLogger(PeppolValidator::class.java)

    /**
     * Validate an invoice for Peppol sending.
     *
     * @param invoice The invoice to validate
     * @param client The client (buyer) information
     * @param tenantSettings The tenant settings (seller information)
     * @param peppolSettings The Peppol settings
     * @return Validation result with errors and warnings
     */
    fun validateForSending(
        invoice: FinancialDocumentDto.InvoiceDto,
        client: ClientDto,
        tenantSettings: TenantSettings,
        peppolSettings: PeppolSettingsDto
    ): PeppolValidationResult {
        val errors = mutableListOf<PeppolValidationError>()
        val warnings = mutableListOf<PeppolValidationWarning>()

        // ========================================================================
        // PEPPOL SETTINGS VALIDATION
        // ========================================================================

        if (!peppolSettings.isEnabled) {
            errors.add(
                PeppolValidationError(
                    code = "PEPPOL_NOT_ENABLED",
                    message = "Peppol is not enabled for this tenant",
                    field = "peppolSettings.isEnabled"
                )
            )
        }

        // ========================================================================
        // SELLER (SUPPLIER) VALIDATION - BT-27 to BT-35
        // ========================================================================

        if (tenantSettings.companyName.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "BT-27",
                    message = "Seller name is required",
                    field = "tenantSettings.companyName"
                )
            )
        }

            val companyVatNumber = tenantSettings.companyVatNumber
        if (companyVatNumber == null) {
            errors.add(
                PeppolValidationError(
                    code = "BT-31",
                    message = "Seller VAT identifier is required for Belgian invoices",
                    field = "tenantSettings.companyVatNumber"
                )
            )
        } else if (!isValidBelgianVat(companyVatNumber.value)) {
            errors.add(
                PeppolValidationError(
                    code = "BT-31-FORMAT",
                    message = "Seller VAT number must be a valid Belgian VAT number (BE + 10 digits)",
                    field = "tenantSettings.companyVatNumber"
                )
            )
        }

        if (tenantSettings.companyAddress.isNullOrBlank()) {
            warnings.add(
                PeppolValidationWarning(
                    code = "BT-35",
                    message = "Seller address is recommended for Peppol invoices",
                    field = "tenantSettings.companyAddress"
                )
            )
        }

        // ========================================================================
        // BUYER (CUSTOMER) VALIDATION - BT-44 to BT-52
        // ========================================================================

        if (client.name.value.isBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "BT-44",
                    message = "Buyer name is required",
                    field = "client.name"
                )
            )
        }

        val clientPeppolId = client.peppolId
        if (clientPeppolId.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "PEPPOL_ID_REQUIRED",
                    message = "Buyer must have a Peppol participant ID to receive e-invoices",
                    field = "client.peppolId"
                )
            )
        } else if (!isValidPeppolId(clientPeppolId)) {
            errors.add(
                PeppolValidationError(
                    code = "PEPPOL_ID_FORMAT",
                    message = "Buyer Peppol ID must be in format 'scheme:identifier' (e.g., '0208:BE0123456789')",
                    field = "client.peppolId"
                )
            )
        }

        if (client.vatNumber == null) {
            warnings.add(
                PeppolValidationWarning(
                    code = "BT-48",
                    message = "Buyer VAT identifier is recommended for B2B invoices",
                    field = "client.vatNumber"
                )
            )
        }

        // ========================================================================
        // INVOICE HEADER VALIDATION - BT-1 to BT-9
        // ========================================================================

        if (invoice.invoiceNumber.value.isBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "BT-1",
                    message = "Invoice number is required",
                    field = "invoice.invoiceNumber"
                )
            )
        }

        if (invoice.issueDate.toEpochDays() <= 0) {
            errors.add(
                PeppolValidationError(
                    code = "BT-2",
                    message = "Invoice issue date is required",
                    field = "invoice.issueDate"
                )
            )
        }

        // ========================================================================
        // LINE ITEMS VALIDATION - BG-25
        // ========================================================================

        if (invoice.items.isEmpty()) {
            errors.add(
                PeppolValidationError(
                    code = "BG-25",
                    message = "Invoice must have at least one line item",
                    field = "invoice.items"
                )
            )
        }

        invoice.items.forEachIndexed { index, item ->
            if (item.description.isBlank()) {
                errors.add(
                    PeppolValidationError(
                        code = "BT-153",
                        message = "Line item ${index + 1}: Item name/description is required",
                        field = "invoice.items[$index].description"
                    )
                )
            }

            if (item.quantity <= 0) {
                errors.add(
                    PeppolValidationError(
                        code = "BT-129",
                        message = "Line item ${index + 1}: Quantity must be greater than zero",
                        field = "invoice.items[$index].quantity"
                    )
                )
            }

            if (item.unitPrice.value.toDouble() < 0) {
                errors.add(
                    PeppolValidationError(
                        code = "BT-146",
                        message = "Line item ${index + 1}: Unit price cannot be negative",
                        field = "invoice.items[$index].unitPrice"
                    )
                )
            }
        }

        // ========================================================================
        // MONETARY TOTALS VALIDATION - BG-22
        // ========================================================================

        val calculatedSubtotal = invoice.items.sumOf { it.lineTotal.value.toDouble() }
        val invoiceSubtotal = invoice.subtotalAmount.value.toDouble()

        if (kotlin.math.abs(calculatedSubtotal - invoiceSubtotal) > 0.01) {
            warnings.add(
                PeppolValidationWarning(
                    code = "BT-106",
                    message = "Sum of line totals ($calculatedSubtotal) doesn't match invoice subtotal ($invoiceSubtotal)",
                    field = "invoice.subtotalAmount"
                )
            )
        }

        val expectedTotal = invoice.subtotalAmount.value.toDouble() + invoice.vatAmount.value.toDouble()
        val actualTotal = invoice.totalAmount.value.toDouble()

        if (kotlin.math.abs(expectedTotal - actualTotal) > 0.01) {
            errors.add(
                PeppolValidationError(
                    code = "BT-112",
                    message = "Invoice total (${actualTotal}) doesn't equal subtotal + VAT ($expectedTotal)",
                    field = "invoice.totalAmount"
                )
            )
        }

        // ========================================================================
        // PAYMENT INFORMATION VALIDATION - BG-16
        // ========================================================================

        if (tenantSettings.companyIban == null) {
            warnings.add(
                PeppolValidationWarning(
                    code = "BT-84",
                    message = "Payment account (IBAN) is recommended for credit transfer payments",
                    field = "tenantSettings.companyIban"
                )
            )
        }

        // ========================================================================
        // BELGIAN SPECIFIC RULES
        // ========================================================================

        // Belgian invoices should have structured communication if possible
        // This is a warning, not an error

        logger.info(
            "Peppol validation completed. Errors: ${errors.size}, Warnings: ${warnings.size}"
        )

        return PeppolValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validate a Belgian VAT number format.
     * Format: BE + 10 digits (e.g., BE0123456789)
     */
    private fun isValidBelgianVat(vatNumber: String): Boolean {
        val cleaned = vatNumber.uppercase().replace(" ", "").replace(".", "")
        return cleaned.matches(Regex("^BE[0-9]{10}$"))
    }

    /**
     * Validate Peppol participant ID format.
     * Format: scheme:identifier (e.g., 0208:BE0123456789)
     */
    private fun isValidPeppolId(peppolId: String): Boolean {
        return peppolId.matches(Regex("^[0-9]{4}:.+$"))
    }

    /**
     * Validate an incoming document from Peppol.
     *
     * @param documentId The external document ID
     * @param senderPeppolId The sender's Peppol ID
     * @return Validation result
     */
    fun validateIncoming(
        documentId: String,
        senderPeppolId: String
    ): PeppolValidationResult {
        val errors = mutableListOf<PeppolValidationError>()
        val warnings = mutableListOf<PeppolValidationWarning>()

        if (documentId.isBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "DOCUMENT_ID_REQUIRED",
                    message = "Document ID is required",
                    field = "documentId"
                )
            )
        }

        if (!isValidPeppolId(senderPeppolId)) {
            errors.add(
                PeppolValidationError(
                    code = "SENDER_PEPPOL_ID_FORMAT",
                    message = "Sender Peppol ID format is invalid",
                    field = "senderPeppolId"
                )
            )
        }

        return PeppolValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
}
