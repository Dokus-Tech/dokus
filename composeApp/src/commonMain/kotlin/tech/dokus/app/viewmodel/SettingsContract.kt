package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.state.DokusState

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class SettingsState(
    val tenant: DokusState<Tenant> = DokusState.loading(),
) : MVIState

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
    data class ShowError(val error: DokusException) : SettingsAction
}
