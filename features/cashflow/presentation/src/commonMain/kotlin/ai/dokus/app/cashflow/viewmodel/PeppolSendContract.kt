package ai.dokus.app.cashflow.viewmodel

import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.InvoiceId
import tech.dokus.domain.model.PeppolInboxPollResponse
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.domain.model.PeppolValidationResult
import tech.dokus.domain.model.PeppolVerifyResponse
import tech.dokus.domain.model.SendInvoiceViaPeppolResponse
import ai.dokus.foundation.domain.model.common.PaginationState
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Peppol sending operations screen.
 *
 * Flow:
 * 1. Idle → Initial state, no data loaded
 * 2. Loading → Fetching transmission history
 * 3. Content → Showing transmission list with operations
 *    - Verify recipient, Validate invoice, Send invoice, Poll inbox
 * 4. Error → Error with retry option
 */

// ============================================================================
// STATE
// ============================================================================

/**
 * State for sub-operations (verify, validate, send, poll, lookup).
 */
@Immutable
sealed interface OperationState<out T> {
    data object Idle : OperationState<Nothing>
    data object Loading : OperationState<Nothing>
    data class Success<T>(val data: T) : OperationState<T>
    data class Error(
        val exception: DokusException,
        val retryHandler: RetryHandler,
    ) : OperationState<Nothing>
}

@Immutable
sealed interface PeppolSendState : MVIState, DokusState<Nothing> {

    /**
     * Initial state - not yet loaded.
     */
    data object Idle : PeppolSendState

    /**
     * Loading transmission history.
     */
    data object Loading : PeppolSendState

    /**
     * Content state with transmission list and sub-operations.
     */
    data class Content(
        val transmissions: List<PeppolTransmissionDto>,
        val pagination: PaginationState<PeppolTransmissionDto>,
        val directionFilter: PeppolTransmissionDirection? = null,
        val statusFilter: PeppolStatus? = null,
        val verificationState: OperationState<PeppolVerifyResponse> = OperationState.Idle,
        val validationState: OperationState<PeppolValidationResult> = OperationState.Idle,
        val sendState: OperationState<SendInvoiceViaPeppolResponse> = OperationState.Idle,
        val pollState: OperationState<PeppolInboxPollResponse> = OperationState.Idle,
        val transmissionLookup: OperationState<PeppolTransmissionDto?> = OperationState.Idle,
    ) : PeppolSendState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : PeppolSendState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface PeppolSendIntent : MVIIntent {
    // Transmission history
    /** Load transmission history */
    data object LoadTransmissions : PeppolSendIntent

    /** Refresh transmission history */
    data object Refresh : PeppolSendIntent

    /** Load next page of transmissions */
    data object LoadNextPage : PeppolSendIntent

    // Filters
    /** Set direction filter */
    data class SetDirectionFilter(val direction: PeppolTransmissionDirection?) : PeppolSendIntent

    /** Set status filter */
    data class SetStatusFilter(val status: PeppolStatus?) : PeppolSendIntent

    /** Clear all filters */
    data object ClearFilters : PeppolSendIntent

    // Recipient verification
    /** Verify a Peppol recipient */
    data class VerifyRecipient(val peppolId: String) : PeppolSendIntent

    /** Reset verification state */
    data object ResetVerificationState : PeppolSendIntent

    // Invoice validation
    /** Validate an invoice for Peppol sending */
    data class ValidateInvoice(val invoiceId: InvoiceId) : PeppolSendIntent

    /** Reset validation state */
    data object ResetValidationState : PeppolSendIntent

    // Send invoice
    /** Send an invoice via Peppol */
    data class SendInvoice(val invoiceId: InvoiceId) : PeppolSendIntent

    /** Reset send state */
    data object ResetSendState : PeppolSendIntent

    // Inbox polling
    /** Poll Peppol inbox for new documents */
    data object PollInbox : PeppolSendIntent

    /** Reset poll state */
    data object ResetPollState : PeppolSendIntent

    // Transmission lookup
    /** Get transmission for a specific invoice */
    data class GetTransmissionForInvoice(val invoiceId: InvoiceId) : PeppolSendIntent

    /** Reset invoice transmission state */
    data object ResetTransmissionLookupState : PeppolSendIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface PeppolSendAction : MVIAction {
    /** Invoice sent successfully */
    data class InvoiceSent(val response: SendInvoiceViaPeppolResponse) : PeppolSendAction

    /** Invoice send failed */
    data class InvoiceSendFailed(val error: Throwable) : PeppolSendAction

    /** Inbox polled successfully */
    data class InboxPolled(val response: PeppolInboxPollResponse) : PeppolSendAction

    /** Inbox poll failed */
    data class InboxPollFailed(val error: Throwable) : PeppolSendAction
}
