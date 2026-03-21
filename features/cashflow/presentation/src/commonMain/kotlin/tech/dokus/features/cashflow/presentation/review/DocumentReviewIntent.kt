package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIIntent
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.features.cashflow.presentation.review.mvi.payment.DocumentPaymentIntent
import tech.dokus.features.cashflow.presentation.review.mvi.preview.DocumentPreviewIntent

@Immutable
sealed interface DocumentReviewIntent : MVIIntent {

    data class LoadDocument(val documentId: DocumentId) : DocumentReviewIntent
    data object Refresh : DocumentReviewIntent
    data class ApplyRemoteSnapshot(val record: DocumentDetailDto) : DocumentReviewIntent
    data object HandleRemoteDeletion : DocumentReviewIntent
    data class SelectQueueDocument(val documentId: DocumentId) : DocumentReviewIntent
    data object LoadMoreQueue : DocumentReviewIntent
    data object RefreshQueue : DocumentReviewIntent

    // Preview intents — delegated to child store
    data class Preview(val intent: DocumentPreviewIntent) : DocumentReviewIntent
    // Payment intents — delegated to child store
    data class Payment(val intent: DocumentPaymentIntent) : DocumentReviewIntent

    data class SelectContact(val contactId: ContactId) : DocumentReviewIntent
    data object AcceptSuggestedContact : DocumentReviewIntent
    data object ClearSelectedContact : DocumentReviewIntent
    data class ContactCreated(val contactId: ContactId) : DocumentReviewIntent
    data object SetPendingCreation : DocumentReviewIntent

    // Contact sheet intents
    data object OpenContactSheet : DocumentReviewIntent
    data object CloseContactSheet : DocumentReviewIntent
    data class UpdateContactSheetSearch(val query: String) : DocumentReviewIntent

    data class SelectFieldForProvenance(val fieldPath: String?) : DocumentReviewIntent

    data object Confirm : DocumentReviewIntent
    data object ViewCashflowEntry : DocumentReviewIntent
    data object ViewEntity : DocumentReviewIntent

    // Reject dialog intents
    data object ShowRejectDialog : DocumentReviewIntent
    data object DismissRejectDialog : DocumentReviewIntent
    data class SelectRejectReason(val reason: DocumentRejectReason) : DocumentReviewIntent
    data class UpdateRejectNote(val note: String) : DocumentReviewIntent
    data object ConfirmReject : DocumentReviewIntent

    // Feedback dialog intents (correction-first "Something's wrong" flow)
    data object ShowFeedbackDialog : DocumentReviewIntent
    data object DismissFeedbackDialog : DocumentReviewIntent
    data class SelectFeedbackCategory(val category: FeedbackCategory) : DocumentReviewIntent
    data class UpdateFeedbackText(val text: String) : DocumentReviewIntent
    data object SubmitFeedback : DocumentReviewIntent
    data object RequestAmendment : DocumentReviewIntent

    // Failed analysis intents
    data object RetryAnalysis : DocumentReviewIntent
    data object DismissFailureBanner : DocumentReviewIntent

    data object ResolvePossibleMatchSame : DocumentReviewIntent
    data object ResolvePossibleMatchDifferent : DocumentReviewIntent

    // Bank statement transaction toggle
    data class ToggleBankStatementTransaction(val index: Int) : DocumentReviewIntent

    // Manual document type selection (when AI fails or type is unknown)
    data class SelectDocumentType(val type: DocumentType) : DocumentReviewIntent
    data class SelectDirection(val direction: DocumentDirection) : DocumentReviewIntent

    // Inline field editing
    data class UpdateField(val field: EditableField, val value: String) : DocumentReviewIntent

    // Unconfirm — revert confirmed document to draft for editing
    data object RequestUnconfirm : DocumentReviewIntent
}
