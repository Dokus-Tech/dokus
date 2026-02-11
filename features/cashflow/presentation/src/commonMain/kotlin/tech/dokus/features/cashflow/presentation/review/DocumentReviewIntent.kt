package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIIntent
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.FinancialLineItem

@Immutable
sealed interface DocumentReviewIntent : MVIIntent {

    data class LoadDocument(val documentId: DocumentId) : DocumentReviewIntent
    data object Refresh : DocumentReviewIntent

    data object LoadPreviewPages : DocumentReviewIntent
    data class LoadMorePages(val maxPages: Int) : DocumentReviewIntent
    data object RetryLoadPreview : DocumentReviewIntent

    data class SelectContact(val contactId: ContactId) : DocumentReviewIntent
    data object AcceptSuggestedContact : DocumentReviewIntent
    data object ClearSelectedContact : DocumentReviewIntent
    data class ContactCreated(val contactId: ContactId) : DocumentReviewIntent
    data class SetCounterpartyIntent(val intent: CounterpartyIntent) : DocumentReviewIntent

    data object OpenPreviewSheet : DocumentReviewIntent
    data object ClosePreviewSheet : DocumentReviewIntent

    // Contact sheet intents
    data object OpenContactSheet : DocumentReviewIntent
    data object CloseContactSheet : DocumentReviewIntent
    data class UpdateContactSheetSearch(val query: String) : DocumentReviewIntent

    data object AddLineItem : DocumentReviewIntent
    data class UpdateLineItem(val index: Int, val item: FinancialLineItem) : DocumentReviewIntent
    data class RemoveLineItem(val index: Int) : DocumentReviewIntent

    data class SelectFieldForProvenance(val fieldPath: String?) : DocumentReviewIntent

    data object SaveDraft : DocumentReviewIntent
    data object DiscardChanges : DocumentReviewIntent
    data object ConfirmDiscardChanges : DocumentReviewIntent
    data object Confirm : DocumentReviewIntent
    data object OpenChat : DocumentReviewIntent
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
    data class UpdateFeedbackText(val text: String) : DocumentReviewIntent
    data object SubmitFeedback : DocumentReviewIntent

    // Failed analysis intents
    data object RetryAnalysis : DocumentReviewIntent
    data object DismissFailureBanner : DocumentReviewIntent

    // Manual document type selection (when AI fails or type is unknown)
    data class SelectDocumentType(val type: DocumentType) : DocumentReviewIntent
    data class SelectDirection(val direction: DocumentDirection) : DocumentReviewIntent
}
