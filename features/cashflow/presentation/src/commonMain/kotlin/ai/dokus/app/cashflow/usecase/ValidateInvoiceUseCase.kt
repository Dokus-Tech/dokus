package ai.dokus.app.cashflow.usecase

import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceFormState
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.invoice_validation_client_required
import ai.dokus.app.resources.generated.invoice_validation_due_date_before_issue
import ai.dokus.app.resources.generated.invoice_validation_items_required
import org.jetbrains.compose.resources.getString

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
    suspend operator fun invoke(formState: CreateInvoiceFormState): InvoiceValidationResult {
        val errors = mutableMapOf<String, String>()

        // Validate client selection
        if (formState.selectedClient == null) {
            errors[FIELD_CLIENT] = getString(Res.string.invoice_validation_client_required)
        }

        // Validate line items - at least one valid item required
        if (!formState.items.any { it.isValid }) {
            errors[FIELD_ITEMS] = getString(Res.string.invoice_validation_items_required)
        }

        // Validate date constraints
        if (formState.issueDate != null && formState.dueDate != null && formState.dueDate < formState.issueDate) {
            errors[FIELD_DUE_DATE] = getString(Res.string.invoice_validation_due_date_before_issue)
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
