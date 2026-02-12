package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.NotificationCategory
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.NotificationDto
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Today screen.
 *
 * The Today screen displays:
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
sealed interface TodayState : MVIState, DokusState<Nothing> {

    /**
     * Loading state - initial data fetch in progress.
     */
    data object Loading : TodayState

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
        val pendingDocumentsState: DokusState<PaginationState<DocumentRecordDto>> = DokusState.idle(),
        val notificationsState: DokusState<List<NotificationDto>> = DokusState.idle(),
        val unreadNotificationCount: Int = 0,
        val notificationFilter: NotificationFilterTab = NotificationFilterTab.All,
    ) : TodayState

    /**
     * Error state - failed to load initial data.
     *
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : TodayState, DokusState.Error<Nothing>

    companion object {
        const val PENDING_PAGE_SIZE = 5
    }
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface TodayIntent : MVIIntent {

    // === Data Loading ===

    /** Refresh tenant data */
    data object RefreshTenant : TodayIntent

    /** Refresh pending documents */
    data object RefreshPendingDocuments : TodayIntent

    /** Load more pending documents for infinite scroll */
    data object LoadMorePendingDocuments : TodayIntent

    /** Load notifications for a selected filter tab */
    data class LoadNotifications(val filter: NotificationFilterTab = NotificationFilterTab.All) : TodayIntent

    /** Refresh unread notification badge count */
    data object RefreshUnreadNotifications : TodayIntent

    /** Open a notification and navigate to its linked screen */
    data class OpenNotification(val notification: NotificationDto) : TodayIntent

    /** Mark all notifications as read */
    data object MarkAllNotificationsRead : TodayIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface TodayAction : MVIAction {

    /** Navigate to document details/edit screen */
    data class NavigateToDocument(val documentId: String) : TodayAction

    /** Navigate to workspace selection */
    data object NavigateToWorkspaceSelect : TodayAction

    /** Show error message */
    data class ShowError(val error: DokusException) : TodayAction
}

@Immutable
enum class NotificationFilterTab(
    val label: String,
    val category: NotificationCategory? = null,
    val unreadOnly: Boolean = false
) {
    All(label = "All"),
    Unread(label = "Unread", unreadOnly = true),
    Peppol(label = "PEPPOL", category = NotificationCategory.Peppol),
    Compliance(label = "Compliance", category = NotificationCategory.Compliance)
}
