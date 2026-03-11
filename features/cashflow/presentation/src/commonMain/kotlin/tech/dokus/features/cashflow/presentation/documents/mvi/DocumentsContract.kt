package tech.dokus.features.cashflow.presentation.documents.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.navigation.destinations.CashFlowDestination

/**
 * Contract for the Documents screen.
 *
 * The Documents screen is the operational inbox displaying:
 * - All documents (uploads, Peppol, email) with status
 * - Status derived from ingestion + draft state
 * - Row click navigates to Document Review for confirmation flow
 */

// ============================================================================
// FILTER
// ============================================================================

/**
 * Simplified document filter options.
 * Replaces the complex DocumentDisplayStatus for filtering.
 */
@Immutable
enum class DocumentFilter {
    /** Show all documents */
    All,

    /** Documents requiring user attention (processing, needs review, failed) */
    NeedsAttention,

    /** Confirmed documents only */
    Confirmed
}

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class DocumentsState(
    val documents: DokusState<PaginationState<DocumentListItemDto>>,
    val filter: DocumentFilter,
    val needsAttentionCount: Int,
    val confirmedCount: Int,
) : MVIState {
    companion object {
        const val PAGE_SIZE = 20

        val initial by lazy {
            DocumentsState(
                documents = DokusState.loading(),
                filter = DocumentFilter.All,
                needsAttentionCount = 0,
                confirmedCount = 0
            )
        }
    }
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface DocumentsIntent : MVIIntent {

    /** Refresh all documents */
    data object Refresh : DocumentsIntent

    /** Refresh documents after external changes (upload/review completion). */
    data object ExternalDocumentsChanged : DocumentsIntent

    /** Load next page of documents */
    data object LoadMore : DocumentsIntent

    /** Update document filter */
    data class UpdateFilter(val filter: DocumentFilter) : DocumentsIntent

    /** Open a document for review */
    data class OpenDocument(val documentId: DocumentId) : DocumentsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface DocumentsAction : MVIAction {

    /** Navigate to document review screen */
    data class NavigateToDocumentReview(
        val documentId: DocumentId,
        val sourceFilter: DocumentFilter,
        val sourceSort: CashFlowDestination.DocumentReviewSourceSort =
            CashFlowDestination.DocumentReviewSourceSort.NewestFirst,
    ) : DocumentsAction

    /** Show error message */
    data class ShowError(val error: DokusException) : DocumentsAction
}
