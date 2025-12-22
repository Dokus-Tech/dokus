package ai.dokus.app.cashflow.usecase

import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceFormState
import kotlinx.datetime.LocalDate

/**
 * Validation result for invoice form.
 *
 * @property isValid True if the form passes all validation checks.
 * @property errors Map of field names to error messages.
 */
data class InvoiceValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String>
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
        val errors = mutableMapOf<String, String>()

        // Validate client selection
        if (formState.selectedClient == null) {
            errors[FIELD_CLIENT] = ERROR_CLIENT_REQUIRED
        }

        // Validate line items - at least one valid item required
        if (!formState.items.any { it.isValid }) {
            errors[FIELD_ITEMS] = ERROR_ITEMS_REQUIRED
        }

        // Validate date constraints
        validateDates(formState.issueDate, formState.dueDate)?.let { error ->
            errors[FIELD_DUE_DATE] = error
        }

        return InvoiceValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Validate date constraints.
     *
     * @param issueDate The issue date.
     * @param dueDate The due date.
     * @return Error message if dates are invalid, null otherwise.
     */
    private fun validateDates(issueDate: LocalDate?, dueDate: LocalDate?): String? {
        if (issueDate != null && dueDate != null && dueDate < issueDate) {
            return ERROR_DUE_DATE_BEFORE_ISSUE
        }
        return null
    }

    companion object {
        // Field keys for error mapping
        const val FIELD_CLIENT = "client"
        const val FIELD_ITEMS = "items"
        const val FIELD_DUE_DATE = "dueDate"

        // Error messages
        const val ERROR_CLIENT_REQUIRED = "Please select a client"
        const val ERROR_ITEMS_REQUIRED = "Please add at least one valid line item"
        const val ERROR_DUE_DATE_BEFORE_ISSUE = "Due date cannot be before issue date"
    }
}
