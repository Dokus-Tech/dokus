package tech.dokus.features.cashflow.presentation.detail

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIIntent
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.features.cashflow.presentation.detail.mvi.payment.DocumentPaymentIntent
import tech.dokus.features.cashflow.presentation.detail.mvi.preview.DocumentPreviewIntent

@Immutable
sealed interface DocumentDetailIntent : MVIIntent {

    data class LoadDocument(val documentId: DocumentId) : DocumentDetailIntent
    data object Refresh : DocumentDetailIntent
    data class ApplyRemoteSnapshot(val record: DocumentDetailDto) : DocumentDetailIntent
    data object HandleRemoteDeletion : DocumentDetailIntent
    data class SelectQueueDocument(val documentId: DocumentId) : DocumentDetailIntent
    data object LoadMoreQueue : DocumentDetailIntent
    data object RefreshQueue : DocumentDetailIntent

    // Preview intents — delegated to child store
    data class Preview(val intent: DocumentPreviewIntent) : DocumentDetailIntent
    // Payment intents — delegated to child store
    data class Payment(val intent: DocumentPaymentIntent) : DocumentDetailIntent

    data class SelectContact(val contactId: ContactId) : DocumentDetailIntent
    data object AcceptSuggestedContact : DocumentDetailIntent
    data object ClearSelectedContact : DocumentDetailIntent
    data class ContactCreated(val contactId: ContactId) : DocumentDetailIntent
    data object SetPendingCreation : DocumentDetailIntent

    // Contact sheet intents
    data object OpenContactSheet : DocumentDetailIntent
    data object CloseContactSheet : DocumentDetailIntent
    data class UpdateContactSheetSearch(val query: String) : DocumentDetailIntent

    data class SelectFieldForProvenance(val fieldPath: String?) : DocumentDetailIntent

    data object Confirm : DocumentDetailIntent
    data object ViewCashflowEntry : DocumentDetailIntent
    data object ViewEntity : DocumentDetailIntent

    // Reject dialog intents
    data object ShowRejectDialog : DocumentDetailIntent
    data object DismissRejectDialog : DocumentDetailIntent
    data class SelectRejectReason(val reason: DocumentRejectReason) : DocumentDetailIntent
    data class UpdateRejectNote(val note: String) : DocumentDetailIntent
    data object ConfirmReject : DocumentDetailIntent

    // Feedback dialog intents (correction-first "Something's wrong" flow)
    data object ShowFeedbackDialog : DocumentDetailIntent
    data object DismissFeedbackDialog : DocumentDetailIntent
    data class SelectFeedbackCategory(val category: FeedbackCategory) : DocumentDetailIntent
    data class UpdateFeedbackText(val text: String) : DocumentDetailIntent
    data object SubmitFeedback : DocumentDetailIntent
    data object RequestAmendment : DocumentDetailIntent

    // Failed analysis intents
    data object RetryAnalysis : DocumentDetailIntent
    data object DismissFailureBanner : DocumentDetailIntent

    data object ResolvePossibleMatchSame : DocumentDetailIntent
    data object ResolvePossibleMatchDifferent : DocumentDetailIntent

    // Bank statement transaction toggle
    data class ToggleBankStatementTransaction(val index: Int) : DocumentDetailIntent

    // Manual document type selection (when AI fails or type is unknown)
    data class SelectDocumentType(val type: DocumentType) : DocumentDetailIntent
    data class SelectDirection(val direction: DocumentDirection) : DocumentDetailIntent

    // Inline field editing
    data class UpdateField(val field: EditableField, val value: String) : DocumentDetailIntent

    // Unconfirm — revert confirmed document to draft for editing
    data object RequestUnconfirm : DocumentDetailIntent
}
