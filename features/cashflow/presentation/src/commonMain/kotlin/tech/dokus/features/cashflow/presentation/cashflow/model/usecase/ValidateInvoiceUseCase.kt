package tech.dokus.features.cashflow.presentation.cashflow.model.usecase

import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.domain.exceptions.DokusException

/**
 * Validation result for invoice form.
 *
 * @property isValid True if the form passes all validation checks.
 * @property errors Map of field names to validation exceptions.
 */
data class InvoiceValidationResult(
    val isValid: Boolean,
    val errors: Map<String, DokusException>
)

/**
 * Use case for validating invoice form data before submission.
 *
 * Validates required fields, line items, and date constraints.
 * Returns a validation result with field-specific error messages.
 */
class ValidateInvoiceUseCase {

    /**
     * Validate the invoice form.
     *
     * @param formState The current form state to validate.
     * @return [InvoiceValidationResult] with validation status and any errors.
     */
    operator fun invoke(formState: CreateInvoiceFormState): InvoiceValidationResult {
        val errors = mutableMapOf<String, DokusException>()

        // Validate client selection
        if (formState.selectedClient == null) {
            errors[FIELD_CLIENT] = DokusException.Validation.InvoiceClientRequired
        }

        // Validate line items - at least one valid item required
        if (!formState.items.any { it.isValid }) {
            errors[FIELD_ITEMS] = DokusException.Validation.InvoiceItemsRequired
        }

        // Validate date constraints
        if (formState.issueDate != null && formState.dueDate != null && formState.dueDate < formState.issueDate) {
            errors[FIELD_DUE_DATE] = DokusException.Validation.InvoiceDueDateBeforeIssue
        }

        return InvoiceValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    companion object {
        // Field keys for error mapping
        const val FIELD_CLIENT = "client"
        const val FIELD_ITEMS = "items"
        const val FIELD_DUE_DATE = "dueDate"
    }
}
