package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.User
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Home screen.
 *
 * The Home screen is the main navigation shell that contains:
 * - Bottom navigation bar (on mobile)
 * - Navigation rail (on larger screens)
 * - Nested navigation host for home destinations
 *
 * This is primarily a navigation container, so the state is minimal.
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface HomeState : MVIState {

    /**
     * Ready state containing shell-level data for workspace/profile controls.
     */
    data class Ready(
        val tenantState: DokusState<Tenant> = DokusState.idle(),
        val userState: DokusState<User> = DokusState.idle(),
        val isLoggingOut: Boolean = false,
    ) : HomeState
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface HomeIntent : MVIIntent {

    /** Screen appeared - perform any initialization */
    data object ScreenAppeared : HomeIntent

    /** Refresh shell data (tenant + user). */
    data object RefreshShellData : HomeIntent

    /** Logout from the shell profile controls. */
    data object Logout : HomeIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface HomeAction : MVIAction {

    /** Show a shell-level error message. */
    data class ShowError(val error: DokusException) : HomeAction
}
