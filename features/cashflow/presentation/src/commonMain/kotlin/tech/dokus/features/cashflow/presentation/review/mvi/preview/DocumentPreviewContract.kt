package tech.dokus.features.cashflow.presentation.review.mvi.preview

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.SourceEvidenceViewerState

@Immutable
data class DocumentPreviewChildState(
    val documentId: DocumentId? = null,
    val documentRecord: DocumentDetailDto? = null,
    val hasContent: Boolean = false,
    val hasPendingMatchReview: Boolean = false,
    val previewState: DocumentPreviewState = DocumentPreviewState.Loading,
    val incomingPreviewState: DocumentPreviewState? = null,
    val sourceViewerState: SourceEvidenceViewerState? = null,
) : MVIState

@Immutable
sealed interface DocumentPreviewIntent : MVIIntent {
    /** Parent pushes document context so preview handlers can read it. */
    data class SetDocumentContext(
        val documentId: DocumentId?,
        val documentRecord: DocumentDetailDto?,
        val hasContent: Boolean,
        val hasPendingMatchReview: Boolean,
        val previewState: DocumentPreviewState? = null,
        val incomingPreviewState: DocumentPreviewState? = null,
        val resetSourceViewer: Boolean = false,
    ) : DocumentPreviewIntent

    data object LoadPages : DocumentPreviewIntent
    data class LoadMorePages(val maxPages: Int) : DocumentPreviewIntent
    data object RetryLoad : DocumentPreviewIntent
    data class OpenSourceModal(val sourceId: DocumentSourceId) : DocumentPreviewIntent
    data object CloseSourceModal : DocumentPreviewIntent
    data object ToggleSourceTechnicalDetails : DocumentPreviewIntent
}

@Immutable
sealed interface DocumentPreviewAction : MVIAction {
    data class ShowError(val error: DokusException) : DocumentPreviewAction
}
