package tech.dokus.features.cashflow.presentation.review

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId

@Immutable
sealed interface DocumentReviewAction : MVIAction {
    data object NavigateBack : DocumentReviewAction
    data class NavigateToChat(val documentId: DocumentId) : DocumentReviewAction
    data class NavigateToEntity(val entityId: String, val entityType: DocumentType) : DocumentReviewAction

    data class ShowError(val error: DokusException) : DocumentReviewAction
    data class ShowSuccess(val success: DocumentReviewSuccess) : DocumentReviewAction
    data object ShowDiscardConfirmation : DocumentReviewAction
}

@Immutable
sealed interface DocumentReviewSuccess {
    data object DraftSaved : DocumentReviewSuccess
    data object DocumentConfirmed : DocumentReviewSuccess
}
