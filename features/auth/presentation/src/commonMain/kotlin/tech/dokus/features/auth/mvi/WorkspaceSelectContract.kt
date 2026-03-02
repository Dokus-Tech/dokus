package tech.dokus.features.auth.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.TenantWorkspaceSummary

/**
 * Contract for Workspace Selection screen.
 *
 * Flow:
 * 1. Loading → Fetching available workspaces from account/me
 * 2. Content → User selects tenant workspace or firm practice
 * 3. Selecting → User has selected a workspace, processing
 * 4. Error → Error occurred with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface WorkspaceSelectState : MVIState {

    data object Loading : WorkspaceSelectState

    data class Content(
        val tenants: List<TenantWorkspaceSummary>,
        val firms: List<FirmWorkspaceSummary>,
    ) : WorkspaceSelectState {
        val hasFirmAccess: Boolean get() = firms.isNotEmpty()
    }

    data class SelectingTenant(
        val tenants: List<TenantWorkspaceSummary>,
        val firms: List<FirmWorkspaceSummary>,
        val selectedTenantId: TenantId,
    ) : WorkspaceSelectState

    data class SelectingFirm(
        val tenants: List<TenantWorkspaceSummary>,
        val firms: List<FirmWorkspaceSummary>,
        val selectedFirmId: FirmId,
    ) : WorkspaceSelectState

    data class Error(
        val exception: DokusException,
        val retryHandler: RetryHandler,
    ) : WorkspaceSelectState
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface WorkspaceSelectIntent : MVIIntent {
    data object LoadWorkspaces : WorkspaceSelectIntent

    /** User selected a tenant from the list */
    data class SelectTenant(val tenantId: TenantId) : WorkspaceSelectIntent

    /** User selected a firm practice */
    data class SelectFirm(val firmId: FirmId) : WorkspaceSelectIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface WorkspaceSelectAction : MVIAction {
    /** Navigate to home screen after successful tenant selection */
    data object NavigateToHome : WorkspaceSelectAction

    /** Navigate to home screen with console surface active */
    data class NavigateToBookkeeperConsole(val firmId: FirmId) : WorkspaceSelectAction

    /** Show error message when selection fails */
    data class ShowSelectionError(val error: DokusException) : WorkspaceSelectAction
}
