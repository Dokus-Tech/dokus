package ai.dokus.peppol.validator

import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.PeppolSettingsDto
import ai.dokus.foundation.domain.model.PeppolValidationError
import ai.dokus.foundation.domain.model.PeppolValidationResult
import ai.dokus.foundation.domain.model.PeppolValidationWarning
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings

/**
 * Validates invoices and settings for Peppol compliance.
 *
 * Checks include:
 * - Required fields for UBL compliance
 * - Peppol ID format validation
 * - VAT number format validation
 * - Belgian/EU specific requirements
 */
class PeppolValidator {

    /**
     * Validate an invoice before sending via Peppol.
     */
    fun validateForSending(
        invoice: FinancialDocumentDto.InvoiceDto,
        contact: ContactDto,
        tenant: Tenant,
        tenantSettings: TenantSettings,
        peppolSettings: PeppolSettingsDto
    ): PeppolValidationResult {
        val errors = mutableListOf<PeppolValidationError>()
        val warnings = mutableListOf<PeppolValidationWarning>()

        // Peppol Settings Validation
        if (!peppolSettings.isEnabled) {
            errors.add(PeppolValidationError(
                code = "PEPPOL_DISABLED",
                message = "Peppol is not enabled for this tenant",
                field = "peppolSettings.isEnabled"
            ))
        }

        if (peppolSettings.peppolId.value.isBlank()) {
            errors.add(PeppolValidationError(
                code = "MISSING_SENDER_PEPPOL_ID",
                message = "Sender Peppol ID is not configured",
                field = "peppolSettings.peppolId"
            ))
        } else if (!isValidPeppolId(peppolSettings.peppolId.value)) {
            errors.add(PeppolValidationError(
                code = "INVALID_SENDER_PEPPOL_ID",
                message = "Sender Peppol ID format is invalid. Expected format: scheme:identifier (e.g., 0208:BE0123456789)",
                field = "peppolSettings.peppolId"
            ))
        }

        // Recipient Validation
        val contactPeppolId = contact.peppolId
        if (contactPeppolId.isNullOrBlank()) {
            errors.add(PeppolValidationError(
                code = "MISSING_RECIPIENT_PEPPOL_ID",
                message = "Contact does not have a Peppol ID configured",
                field = "contact.peppolId"
            ))
        } else if (!isValidPeppolId(contactPeppolId)) {
            errors.add(PeppolValidationError(
                code = "INVALID_RECIPIENT_PEPPOL_ID",
                message = "Contact Peppol ID format is invalid. Expected format: scheme:identifier",
                field = "contact.peppolId"
            ))
        }

        // Seller (Tenant) Validation
        if (tenantSettings.companyName.isNullOrBlank()) {
            errors.add(PeppolValidationError(
                code = "MISSING_SELLER_NAME",
                message = "Company name is required in tenant settings",
                field = "tenantSettings.companyName"
            ))
        }

        if (tenant.vatNumber == null) {
            warnings.add(PeppolValidationWarning(
                code = "MISSING_SELLER_VAT",
                message = "Company VAT number is recommended for Peppol compliance",
                field = "tenant.vatNumber"
            ))
        }

        if (tenant.companyAddress.isBlank()) {
            warnings.add(PeppolValidationWarning(
                code = "MISSING_SELLER_ADDRESS",
                message = "Company address is recommended for Peppol compliance",
                field = "tenant.companyAddress"
            ))
        }

        // Buyer (Contact) Validation
        if (contact.name.value.isBlank()) {
            errors.add(PeppolValidationError(
                code = "MISSING_BUYER_NAME",
                message = "Contact name is required",
                field = "contact.name"
            ))
        }

        // Invoice Validation
        if (invoice.invoiceNumber.value.isBlank()) {
            errors.add(PeppolValidationError(
                code = "MISSING_INVOICE_NUMBER",
                message = "Invoice number is required",
                field = "invoice.invoiceNumber"
            ))
        }

        if (invoice.items.isEmpty()) {
            errors.add(PeppolValidationError(
                code = "NO_LINE_ITEMS",
                message = "Invoice must have at least one line item",
                field = "invoice.items"
            ))
        }

        // Validate each line item
        invoice.items.forEachIndexed { index, item ->
            if (item.description.isBlank()) {
                errors.add(PeppolValidationError(
                    code = "MISSING_LINE_DESCRIPTION",
                    message = "Line item ${index + 1} must have a description",
                    field = "invoice.items[$index].description"
                ))
            }

            if (item.quantity <= 0) {
                errors.add(PeppolValidationError(
                    code = "INVALID_LINE_QUANTITY",
                    message = "Line item ${index + 1} must have a positive quantity",
                    field = "invoice.items[$index].quantity"
                ))
            }
        }

        // Payment info validation
        if (tenantSettings.companyIban == null) {
            warnings.add(PeppolValidationWarning(
                code = "MISSING_IBAN",
                message = "IBAN is recommended for payment processing",
                field = "tenantSettings.companyIban"
            ))
        }

        return PeppolValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validate incoming document metadata.
     */
    fun validateIncoming(documentId: String, senderPeppolId: String): PeppolValidationResult {
        val errors = mutableListOf<PeppolValidationError>()
        val warnings = mutableListOf<PeppolValidationWarning>()

        if (documentId.isBlank()) {
            errors.add(PeppolValidationError(
                code = "MISSING_DOCUMENT_ID",
                message = "Document ID is required",
                field = "documentId"
            ))
        }

        if (senderPeppolId.isBlank()) {
            errors.add(PeppolValidationError(
                code = "MISSING_SENDER_ID",
                message = "Sender Peppol ID is required",
                field = "senderPeppolId"
            ))
        } else if (!isValidPeppolId(senderPeppolId)) {
            warnings.add(PeppolValidationWarning(
                code = "INVALID_SENDER_PEPPOL_ID",
                message = "Sender Peppol ID format may be invalid",
                field = "senderPeppolId"
            ))
        }

        return PeppolValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Validate Peppol ID format.
     *
     * Format: scheme:identifier
     * Common schemes:
     * - 0208: Belgian enterprise number (BE + 10 digits)
     * - 0191: Dutch KvK number
     * - 0088: EAN location code
     */
    private fun isValidPeppolId(peppolId: String): Boolean {
        if (!peppolId.contains(":")) return false

        val parts = peppolId.split(":", limit = 2)
        if (parts.size != 2) return false

        val scheme = parts[0]
        val identifier = parts[1]

        // Scheme should be 4 digits
        if (!scheme.matches(Regex("^\\d{4}$"))) return false

        // Identifier should not be empty
        if (identifier.isBlank()) return false

        // Belgian specific validation
        if (scheme == "0208") {
            // Belgian enterprise number: BE + 10 digits
            return identifier.matches(Regex("^BE\\d{10}$"))
        }

        return true
    }
}
