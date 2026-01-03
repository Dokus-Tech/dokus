package tech.dokus.domain.validators

import tech.dokus.domain.ids.InvoiceNumber

/**
 * Validates invoice numbers
 *
 * Requirements:
 * - Cannot be blank
 * - Maximum 50 characters
 */
object ValidateInvoiceNumberUseCase : Validator<InvoiceNumber> {
    /** Maximum allowed length for invoice numbers */
    private const val MaxLength = 50

    override operator fun invoke(value: InvoiceNumber): Boolean {
        if (value.value.isBlank()) return false
        if (value.value.length > MaxLength) return false
        return true
    }
}
