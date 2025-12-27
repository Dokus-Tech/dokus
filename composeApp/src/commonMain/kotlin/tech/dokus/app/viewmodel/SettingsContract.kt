package tech.dokus.app.viewmodel

import ai.dokus.foundation.domain.asbtractions.RetryHandler
import ai.dokus.foundation.domain.exceptions.DokusException
import tech.dokus.domain.model.Tenant
import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Settings screen.
 *
 * The Settings screen displays:
 * - Current tenant/workspace information
 * - Navigation to various settings sections
 *
 * Flow:
 * 1. Loading -> Initial tenant fetch
 * 2. Content -> Tenant loaded, user can interact
 * 3. Error -> Failed to load with retry option
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface SettingsState : MVIState, DokusState<Tenant?> {

    /**
     * Loading state - fetching current tenant.
     */
    data object Loading : SettingsState

    /**
     * Content state - tenant loaded and ready for display.
     *
     * @property tenant The current tenant/workspace
     */
    data class Content(
        val tenant: Tenant?,
    ) : SettingsState

    /**
     * Error state - failed to load tenant.
     *
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : SettingsState, DokusState.Error<Tenant?>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface SettingsIntent : MVIIntent {

    /** Load the current tenant */
    data object Load : SettingsIntent

    /** Refresh the current tenant (e.g., after returning from workspace selection) */
    data object Refresh : SettingsIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface SettingsAction : MVIAction {

    /** Navigate to workspace selection */
    data object NavigateToWorkspaceSelect : SettingsAction

    /** Show error message */
    data class ShowError(val message: String) : SettingsAction
}
