package tech.dokus.peppol.validator

import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.PeppolValidationError
import tech.dokus.domain.model.PeppolValidationResult
import tech.dokus.domain.model.PeppolValidationWarning
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
import tech.dokus.domain.model.contact.ContactDto

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
     * NOTE: recipientPeppolId should be resolved via PeppolRecipientResolver before calling this.
     */
    fun validateForSending(
        invoice: FinancialDocumentDto.InvoiceDto,
        contact: ContactDto,
        tenant: Tenant,
        companyAddress: Address?,
        tenantSettings: TenantSettings,
        peppolSettings: PeppolSettingsDto,
        recipientPeppolId: String?
    ): PeppolValidationResult {
        val errors = mutableListOf<PeppolValidationError>()
        val warnings = mutableListOf<PeppolValidationWarning>()

        // Peppol Settings Validation
        if (!peppolSettings.isEnabled) {
            errors.add(
                PeppolValidationError(
                    code = "PEPPOL_DISABLED",
                    message = "Peppol is not enabled for this tenant",
                    field = "peppolSettings.isEnabled"
                )
            )
        }

        if (peppolSettings.peppolId.value.isBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_SENDER_PEPPOL_ID",
                    message = "Sender Peppol ID is not configured",
                    field = "peppolSettings.peppolId"
                )
            )
        } else if (!isValidPeppolId(peppolSettings.peppolId.value)) {
            errors.add(
                PeppolValidationError(
                    code = "INVALID_SENDER_PEPPOL_ID",
                    message = "Sender Peppol ID format is invalid. Expected format: scheme:identifier (e.g., 0208:BE0123456789)",
                    field = "peppolSettings.peppolId"
                )
            )
        }

        // Recipient Validation (uses resolved PEPPOL ID)
        if (recipientPeppolId.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_RECIPIENT_PEPPOL_ID",
                    message = "Contact does not have a resolved Peppol ID",
                    field = "recipientPeppolId"
                )
            )
        } else if (!isValidPeppolId(recipientPeppolId)) {
            errors.add(
                PeppolValidationError(
                    code = "INVALID_RECIPIENT_PEPPOL_ID",
                    message = "Recipient Peppol ID format is invalid. Expected format: scheme:identifier",
                    field = "recipientPeppolId"
                )
            )
        }

        // Seller (Tenant) Validation
        validateSellerRequiredFields(tenant, tenantSettings, companyAddress, errors)

        // Buyer (Contact) Validation
        validateBuyerRequiredFields(contact, errors)

        // Invoice Validation
        validateInvoiceDirection(invoice, errors)

        if (invoice.invoiceNumber.value.isBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_INVOICE_NUMBER",
                    message = "Invoice number is required",
                    field = "invoice.invoiceNumber"
                )
            )
        }

        validateNonZeroTotals(invoice, errors)
        validateVatMath(invoice, errors)

        if (invoice.items.isEmpty()) {
            errors.add(
                PeppolValidationError(
                    code = "NO_LINE_ITEMS",
                    message = "Invoice must have at least one line item",
                    field = "invoice.items"
                )
            )
        }

        // Validate each line item
        invoice.items.forEachIndexed { index, item ->
            if (item.description.isBlank()) {
                errors.add(
                    PeppolValidationError(
                        code = "MISSING_LINE_DESCRIPTION",
                        message = "Line item ${index + 1} must have a description",
                        field = "invoice.items[$index].description"
                    )
                )
            }

            if (item.quantity <= 0) {
                errors.add(
                    PeppolValidationError(
                        code = "INVALID_LINE_QUANTITY",
                        message = "Line item ${index + 1} must have a positive quantity",
                        field = "invoice.items[$index].quantity"
                    )
                )
            }
        }

        // Payment info validation
        if (tenantSettings.companyIban == null) {
            warnings.add(
                PeppolValidationWarning(
                    code = "MISSING_IBAN",
                    message = "IBAN is recommended for payment processing",
                    field = "tenantSettings.companyIban"
                )
            )
        }

        return PeppolValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateInvoiceDirection(
        invoice: FinancialDocumentDto.InvoiceDto,
        errors: MutableList<PeppolValidationError>
    ) {
        if (invoice.direction != DocumentDirection.Outbound) {
            errors.add(
                PeppolValidationError(
                    code = "INVALID_DOCUMENT_DIRECTION",
                    message = "Only outbound invoices can be sent via PEPPOL",
                    field = "invoice.direction"
                )
            )
        }
    }

    private fun validateNonZeroTotals(
        invoice: FinancialDocumentDto.InvoiceDto,
        errors: MutableList<PeppolValidationError>
    ) {
        if (!invoice.totalAmount.isPositive) {
            errors.add(
                PeppolValidationError(
                    code = "INVALID_TOTAL_AMOUNT",
                    message = "Invoice total must be greater than zero",
                    field = "invoice.totalAmount"
                )
            )
        }
        if (!invoice.subtotalAmount.isPositive) {
            errors.add(
                PeppolValidationError(
                    code = "INVALID_SUBTOTAL_AMOUNT",
                    message = "Invoice subtotal must be greater than zero",
                    field = "invoice.subtotalAmount"
                )
            )
        }
    }

    private fun validateVatMath(
        invoice: FinancialDocumentDto.InvoiceDto,
        errors: MutableList<PeppolValidationError>
    ) {
        val computedSubtotal = Money(invoice.items.sumOf { it.lineTotal.minor })
        if (computedSubtotal != invoice.subtotalAmount) {
            errors.add(
                PeppolValidationError(
                    code = "SUBTOTAL_MISMATCH",
                    message = "Invoice subtotal does not match line totals",
                    field = "invoice.subtotalAmount"
                )
            )
        }

        val computedVat = Money(invoice.items.sumOf { it.vatAmount.minor })
        if (computedVat != invoice.vatAmount) {
            errors.add(
                PeppolValidationError(
                    code = "VAT_MISMATCH",
                    message = "Invoice VAT amount does not match line VAT totals",
                    field = "invoice.vatAmount"
                )
            )
        }

        val computedTotal = computedSubtotal + computedVat
        if (computedTotal != invoice.totalAmount) {
            errors.add(
                PeppolValidationError(
                    code = "TOTAL_MISMATCH",
                    message = "Invoice total does not equal subtotal plus VAT",
                    field = "invoice.totalAmount"
                )
            )
        }
    }

    private fun validateSellerRequiredFields(
        tenant: Tenant,
        tenantSettings: TenantSettings,
        companyAddress: Address?,
        errors: MutableList<PeppolValidationError>
    ) {
        if (tenantSettings.companyName.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_SELLER_NAME",
                    message = "Company name is required in tenant settings",
                    field = "tenantSettings.companyName"
                )
            )
        }

        if (tenant.vatNumber.value.isBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_SELLER_VAT",
                    message = "Company VAT number is required for PEPPOL sending",
                    field = "tenant.vatNumber"
                )
            )
        }

        if (companyAddress == null) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_SELLER_ADDRESS",
                    message = "Company address is required for PEPPOL sending",
                    field = "tenantAddress"
                )
            )
            return
        }

        if (companyAddress.streetLine1.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_SELLER_STREET",
                    message = "Company street address is required",
                    field = "tenantAddress.streetLine1"
                )
            )
        }
        if (companyAddress.city.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_SELLER_CITY",
                    message = "Company city is required",
                    field = "tenantAddress.city"
                )
            )
        }
        if (companyAddress.postalCode.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_SELLER_POSTAL_CODE",
                    message = "Company postal code is required",
                    field = "tenantAddress.postalCode"
                )
            )
        }
        if (companyAddress.country.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_SELLER_COUNTRY",
                    message = "Company country is required",
                    field = "tenantAddress.country"
                )
            )
        }
    }

    private fun validateBuyerRequiredFields(
        contact: ContactDto,
        errors: MutableList<PeppolValidationError>
    ) {
        if (contact.name.value.isBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_BUYER_NAME",
                    message = "Contact name is required",
                    field = "contact.name"
                )
            )
        }

        val defaultAddress = contact.addresses.firstOrNull { it.isDefault } ?: contact.addresses.firstOrNull()
        if (defaultAddress?.address?.streetLine1.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_BUYER_STREET",
                    message = "Buyer street address is required",
                    field = "contact.addresses[].streetLine1"
                )
            )
        }
        if (defaultAddress?.address?.city.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_BUYER_CITY",
                    message = "Buyer city is required",
                    field = "contact.addresses[].city"
                )
            )
        }
        if (defaultAddress?.address?.postalCode.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_BUYER_POSTAL_CODE",
                    message = "Buyer postal code is required",
                    field = "contact.addresses[].postalCode"
                )
            )
        }
        if (defaultAddress?.address?.country.isNullOrBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_BUYER_COUNTRY",
                    message = "Buyer country is required",
                    field = "contact.addresses[].country"
                )
            )
        }
    }

    /**
     * Validate incoming document metadata.
     */
    fun validateIncoming(documentId: String, senderPeppolId: String): PeppolValidationResult {
        val errors = mutableListOf<PeppolValidationError>()
        val warnings = mutableListOf<PeppolValidationWarning>()

        if (documentId.isBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_DOCUMENT_ID",
                    message = "Document ID is required",
                    field = "documentId"
                )
            )
        }

        if (senderPeppolId.isBlank()) {
            errors.add(
                PeppolValidationError(
                    code = "MISSING_SENDER_ID",
                    message = "Sender Peppol ID is required",
                    field = "senderPeppolId"
                )
            )
        } else if (!isValidPeppolId(senderPeppolId)) {
            warnings.add(
                PeppolValidationWarning(
                    code = "INVALID_SENDER_PEPPOL_ID",
                    message = "Sender Peppol ID format may be invalid",
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
