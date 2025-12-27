package ai.dokus.app.auth.viewmodel

import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.TenantId
import tech.dokus.domain.model.Tenant
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for Workspace Selection screen.
 *
 * Flow:
 * 1. Loading → Fetching available tenants
 * 2. Content → User selects from tenant list
 * 3. Selecting → User has selected a tenant, processing
 * 4. Error → Error occurred with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface WorkspaceSelectState : MVIState, DokusState<List<Tenant>> {

    /**
     * Initial loading state - fetching tenants from server.
     */
    data object Loading : WorkspaceSelectState, DokusState.Loading<List<Tenant>>

    /**
     * Content state - user can select from available tenants.
     */
    data class Content(
        override val data: List<Tenant>,
    ) : WorkspaceSelectState, DokusState.Success<List<Tenant>> {
        val tenants: List<Tenant> get() = data
    }

    /**
     * Selecting state - user has selected a tenant, processing selection.
     */
    data class Selecting(
        val tenants: List<Tenant>,
        val selectedTenantId: TenantId,
    ) : WorkspaceSelectState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : WorkspaceSelectState, DokusState.Error<List<Tenant>>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface WorkspaceSelectIntent : MVIIntent {
    data object LoadTenants : WorkspaceSelectIntent

    /** User selected a tenant from the list */
    data class SelectTenant(val tenantId: TenantId) : WorkspaceSelectIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface WorkspaceSelectAction : MVIAction {
    /** Navigate to home screen after successful tenant selection */
    data object NavigateToHome : WorkspaceSelectAction

    /** Show error message when selection fails */
    data class ShowSelectionError(val message: String) : WorkspaceSelectAction
}
