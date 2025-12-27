package tech.dokus.app.viewmodel

import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.DocumentProcessingDto
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.common.Thumbnail
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Dashboard screen.
 *
 * The Dashboard screen displays:
 * - Current tenant/workspace information
 * - Pending documents for processing (mobile only)
 * - Quick actions and widgets
 *
 * Flow:
 * 1. Loading -> Initial data fetch
 * 2. Content -> Data loaded, user can interact
 * 3. Error -> Failed to load with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface DashboardState : MVIState, DokusState<Nothing> {

    /**
     * Loading state - initial data fetch in progress.
     */
    data object Loading : DashboardState

    /**
     * Content state - data loaded and ready for display.
     *
     * @property tenantState Loading state for current tenant
     * @property currentAvatar Current workspace avatar
     * @property pendingDocumentsState Loading state for pending documents (with pagination)
     */
    data class Content(
        val tenantState: DokusState<Tenant?> = DokusState.idle(),
        val currentAvatar: Thumbnail? = null,
        val pendingDocumentsState: DokusState<PaginationState<DocumentProcessingDto>> = DokusState.idle(),
    ) : DashboardState

    /**
     * Error state - failed to load initial data.
     *
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : DashboardState, DokusState.Error<Nothing>

    companion object {
        const val PENDING_PAGE_SIZE = 5
    }
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface DashboardIntent : MVIIntent {

    // === Data Loading ===

    /** Refresh tenant data */
    data object RefreshTenant : DashboardIntent

    /** Refresh pending documents */
    data object RefreshPendingDocuments : DashboardIntent

    /** Load more pending documents for infinite scroll */
    data object LoadMorePendingDocuments : DashboardIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface DashboardAction : MVIAction {

    /** Navigate to document details/edit screen */
    data class NavigateToDocument(val documentId: String) : DashboardAction

    /** Navigate to workspace selection */
    data object NavigateToWorkspaceSelect : DashboardAction

    /** Show error message */
    data class ShowError(val message: String) : DashboardAction
}
