package tech.dokus.features.cashflow.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.presentation.cashflow.components.DocumentSortOption
import tech.dokus.features.cashflow.presentation.cashflow.components.VatSummaryData
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentDeletionHandle
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Cashflow screen.
 *
 * The Cashflow screen is the main document management hub displaying:
 * - Financial documents (invoices, expenses, bills) with pagination
 * - Pending/processing documents
 * - VAT summary card
 * - Search and sort capabilities
 * - Document upload functionality
 *
 * Flow:
 * 1. Loading → Initial data fetch
 * 2. Content → Documents loaded, user can search/sort/paginate
 * 3. Error → Failed to load with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface CashflowState : MVIState, DokusState<Nothing> {

    /**
     * Loading state - initial data fetch in progress.
     */
    data object Loading : CashflowState

    /**
     * Content state - documents loaded and ready for display.
     *
     * @property documents Paginated list of financial documents
     * @property searchQuery Current search filter
     * @property sortOption Current sort order
     * @property isSidebarOpen Whether the sidebar/drawer is open
     * @property isQrDialogOpen Whether the QR code dialog is open
     * @property pendingDocuments Documents being processed (with pagination)
     * @property pendingDocumentsState Loading state for pending documents
     * @property vatSummaryState Loading state for VAT summary card
     * @property uploadTasks Active document upload tasks
     * @property uploadedDocuments Successfully uploaded documents (by task ID)
     * @property deletionHandles Pending deletions with undo capability (by document ID)
     */
    data class Content(
        val documents: PaginationState<FinancialDocumentDto>,
        val searchQuery: String = "",
        val sortOption: DocumentSortOption = DocumentSortOption.Default,
        val isSidebarOpen: Boolean = false,
        val isQrDialogOpen: Boolean = false,
        val pendingDocuments: PaginationState<DocumentRecordDto> = PaginationState(pageSize = PENDING_PAGE_SIZE),
        val pendingDocumentsState: DokusState<PaginationState<DocumentRecordDto>> = DokusState.idle(),
        val vatSummaryState: DokusState<VatSummaryData> = DokusState.loading(),
        val uploadTasks: List<DocumentUploadTask> = emptyList(),
        val uploadedDocuments: Map<String, DocumentDto> = emptyMap(),
        val deletionHandles: Map<String, DocumentDeletionHandle> = emptyMap(),
    ) : CashflowState {
        companion object {
            private const val PENDING_PAGE_SIZE = 4
        }
    }

    /**
     * Error state - failed to load initial data.
     *
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : CashflowState, DokusState.Error<Nothing>

    companion object {
        const val PAGE_SIZE = 20
        const val PENDING_PAGE_SIZE = 4
    }
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface CashflowIntent : MVIIntent {

    // === Document Loading ===

    /** Refresh all data (documents, VAT summary, business health) */
    data object Refresh : CashflowIntent

    /** Load next page of documents */
    data object LoadMore : CashflowIntent

    /** Refresh only pending/processing documents */
    data object RefreshPendingDocuments : CashflowIntent

    /** Load more pending documents */
    data object LoadMorePendingDocuments : CashflowIntent

    // === Search & Filter ===

    /** Update search query and filter documents */
    data class UpdateSearchQuery(val query: String) : CashflowIntent

    /** Update sort option */
    data class UpdateSortOption(val option: DocumentSortOption) : CashflowIntent

    // === UI State ===

    /** Open the sidebar/drawer */
    data object OpenSidebar : CashflowIntent

    /** Close the sidebar/drawer */
    data object CloseSidebar : CashflowIntent

    /** Toggle sidebar open/close state */
    data object ToggleSidebar : CashflowIntent

    /** Show QR code dialog */
    data object ShowQrDialog : CashflowIntent

    /** Hide QR code dialog */
    data object HideQrDialog : CashflowIntent

    // === Document Actions ===

    /** Cancel a pending document deletion (undo) */
    data class CancelDeletion(val documentId: String) : CashflowIntent

    /** Retry a failed upload task */
    data class RetryUpload(val taskId: String) : CashflowIntent

    /** Cancel an upload task */
    data class CancelUpload(val taskId: String) : CashflowIntent

    /** Dismiss a completed upload task */
    data class DismissUpload(val taskId: String) : CashflowIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface CashflowAction : MVIAction {

    /** Navigate to document details */
    data class NavigateToDocument(val documentId: String) : CashflowAction

    /** Navigate to create new invoice */
    data object NavigateToCreateInvoice : CashflowAction

    /** Navigate to add document (upload) screen */
    data object NavigateToAddDocument : CashflowAction

    /** Navigate to settings */
    data object NavigateToSettings : CashflowAction

    /** Show error message as snackbar/toast */
    data class ShowError(val error: DokusException) : CashflowAction

    /** Show success message as snackbar/toast */
    data class ShowSuccess(val success: CashflowSuccess) : CashflowAction
}

enum class CashflowSuccess {
    InvoiceCreated,
}
