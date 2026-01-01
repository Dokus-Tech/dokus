package tech.dokus.features.cashflow.mvi

import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceUiState
import tech.dokus.features.cashflow.mvi.model.InvoiceDeliveryMethod
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.contact.ContactDto
import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for creating a new invoice with multi-step form.
 *
 * Flow:
 * 1. Editing → User fills out invoice form (client, items, dates, notes)
 * 2. SendOptions → User selects delivery method (PDF, Peppol, Email)
 * 3. Saving → Invoice is being submitted
 * 4. Success → Invoice created successfully, navigate back
 * 5. Error → Submission failed with retry option
 *
 * Desktop: Two-column layout with form and send options side by side
 * Mobile: Two-step flow - edit invoice first, then send options
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface CreateInvoiceState : MVIState, DokusState<Nothing> {

    /**
     * Form data shared across all editing states.
     */
    val formState: CreateInvoiceFormState

    /**
     * UI state shared across all editing states.
     */
    val uiState: CreateInvoiceUiState

    /**
     * Editing state - user is filling out the invoice form.
     *
     * @property formState The invoice form data (client, items, dates, notes)
     * @property uiState UI state (expanded item, panels, current step)
     * @property clients Available clients for selection
     * @property clientsLoading Whether clients are being loaded
     * @property invoiceNumberPreview Preview of the next invoice number
     */
    data class Editing(
        override val formState: CreateInvoiceFormState,
        override val uiState: CreateInvoiceUiState,
        val clients: List<ContactDto> = emptyList(),
        val clientsLoading: Boolean = false,
        val invoiceNumberPreview: String? = null,
    ) : CreateInvoiceState

    /**
     * Saving state - invoice is being submitted.
     *
     * @property formState The form data being saved
     * @property uiState Current UI state
     */
    data class Saving(
        override val formState: CreateInvoiceFormState,
        override val uiState: CreateInvoiceUiState,
    ) : CreateInvoiceState

    /**
     * Success state - invoice created successfully.
     *
     * @property formState The form data that was saved
     * @property uiState Current UI state
     * @property createdInvoiceId ID of the created invoice
     */
    data class Success(
        override val formState: CreateInvoiceFormState,
        override val uiState: CreateInvoiceUiState,
        val createdInvoiceId: InvoiceId,
    ) : CreateInvoiceState

    /**
     * Error state - submission failed with recovery option.
     *
     * @property formState The form data (preserved for retry)
     * @property uiState Current UI state
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        override val formState: CreateInvoiceFormState,
        override val uiState: CreateInvoiceUiState,
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : CreateInvoiceState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface CreateInvoiceIntent : MVIIntent {

    // === Client Selection ===

    /** Open the client selection panel */
    data object OpenClientPanel : CreateInvoiceIntent

    /** Close the client selection panel */
    data object CloseClientPanel : CreateInvoiceIntent

    /** Update client search query */
    data class UpdateClientSearchQuery(val query: String) : CreateInvoiceIntent

    /** Select a client and close the panel */
    data class SelectClient(val client: ContactDto) : CreateInvoiceIntent

    /** Clear the selected client */
    data object ClearClient : CreateInvoiceIntent

    // === Date Selection ===

    /** Open issue date picker */
    data object OpenIssueDatePicker : CreateInvoiceIntent

    /** Open due date picker */
    data object OpenDueDatePicker : CreateInvoiceIntent

    /** Close date picker */
    data object CloseDatePicker : CreateInvoiceIntent

    /** Select a date (applies to currently open date picker) */
    data class SelectDate(val date: LocalDate) : CreateInvoiceIntent

    /** Update issue date directly */
    data class UpdateIssueDate(val date: LocalDate) : CreateInvoiceIntent

    /** Update due date directly */
    data class UpdateDueDate(val date: LocalDate) : CreateInvoiceIntent

    // === Line Items ===

    /** Expand a line item for editing */
    data class ExpandItem(val itemId: String) : CreateInvoiceIntent

    /** Collapse the currently expanded item */
    data object CollapseItem : CreateInvoiceIntent

    /** Toggle item expanded state */
    data class ToggleItemExpanded(val itemId: String) : CreateInvoiceIntent

    /** Add a new line item */
    data object AddLineItem : CreateInvoiceIntent

    /** Remove a line item */
    data class RemoveLineItem(val itemId: String) : CreateInvoiceIntent

    /** Update line item description */
    data class UpdateItemDescription(val itemId: String, val description: String) : CreateInvoiceIntent

    /** Update line item quantity */
    data class UpdateItemQuantity(val itemId: String, val quantity: Double) : CreateInvoiceIntent

    /** Update line item unit price */
    data class UpdateItemUnitPrice(val itemId: String, val unitPrice: String) : CreateInvoiceIntent

    /** Update line item VAT rate */
    data class UpdateItemVatRate(val itemId: String, val vatRatePercent: Int) : CreateInvoiceIntent

    // === Notes ===

    /** Update invoice notes */
    data class UpdateNotes(val notes: String) : CreateInvoiceIntent

    // === Delivery Options ===

    /** Select delivery method */
    data class SelectDeliveryMethod(val method: InvoiceDeliveryMethod) : CreateInvoiceIntent

    // === Navigation (Multi-Step Flow) ===

    /** Go to send options step (mobile) */
    data object GoToSendOptions : CreateInvoiceIntent

    /** Go back to edit invoice step (mobile) */
    data object GoBackToEdit : CreateInvoiceIntent

    /** User clicked back button */
    data object BackClicked : CreateInvoiceIntent

    // === Form Actions ===

    /** Validate the form */
    data object ValidateForm : CreateInvoiceIntent

    /** Save the invoice as draft */
    data object SaveAsDraft : CreateInvoiceIntent

    /** Reset the form to initial state */
    data object ResetForm : CreateInvoiceIntent

    /** Reload clients list */
    data object ReloadClients : CreateInvoiceIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface CreateInvoiceAction : MVIAction {

    /** Navigate back to previous screen */
    data object NavigateBack : CreateInvoiceAction

    /** Navigate to create new contact */
    data object NavigateToCreateContact : CreateInvoiceAction

    /** Navigate to invoice details after creation */
    data class NavigateToInvoice(val invoiceId: InvoiceId) : CreateInvoiceAction

    /** Show validation error */
    data class ShowValidationError(val error: DokusException) : CreateInvoiceAction

    /** Show success message */
    data object ShowSuccess : CreateInvoiceAction

    /** Show error */
    data class ShowError(val error: DokusException) : CreateInvoiceAction
}
