package ai.dokus.foundation.domain.usecases.validators

import ai.dokus.foundation.domain.InvoiceNumber

/**
 * Validates invoice numbers
 *
 * Requirements:
 * - Cannot be blank
 * - Maximum 50 characters
 */
object ValidateInvoiceNumberUseCase : Validator<InvoiceNumber> {
    override operator fun invoke(value: InvoiceNumber): Boolean {
        if (value.value.isBlank()) return false
        if (value.value.length > 50) return false
        return true
    }
}
