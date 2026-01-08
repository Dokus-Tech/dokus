package tech.dokus.features.cashflow.presentation.documents.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Documents screen.
 *
 * The Documents screen is the operational inbox displaying:
 * - All documents (uploads, Peppol, email) with status
 * - Status derived from ingestion + draft state
 * - Row click navigates to Document Review for confirmation flow
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface DocumentsState : MVIState, DokusState<Nothing> {

    /**
     * Loading state - initial data fetch in progress.
     */
    data object Loading : DocumentsState

    /**
     * Content state - documents loaded and ready for display.
     */
    data class Content(
        val documents: PaginationState<DocumentRecordDto>,
        val searchQuery: String = "",
        val statusFilter: DraftStatus? = null,
    ) : DocumentsState

    /**
     * Error state - failed to load initial data.
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : DocumentsState, DokusState.Error<Nothing>

    companion object {
        const val PAGE_SIZE = 20
    }
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface DocumentsIntent : MVIIntent {

    /** Refresh all documents */
    data object Refresh : DocumentsIntent

    /** Load next page of documents */
    data object LoadMore : DocumentsIntent

    /** Update search query */
    data class UpdateSearchQuery(val query: String) : DocumentsIntent

    /** Update status filter */
    data class UpdateStatusFilter(val status: DraftStatus?) : DocumentsIntent

    /** Open a document for review */
    data class OpenDocument(val documentId: DocumentId) : DocumentsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface DocumentsAction : MVIAction {

    /** Navigate to document review screen */
    data class NavigateToDocumentReview(val documentId: DocumentId) : DocumentsAction

    /** Show error message */
    data class ShowError(val error: DokusException) : DocumentsAction
}
