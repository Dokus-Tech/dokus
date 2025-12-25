package tech.dokus.app.viewmodel

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

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
     * Ready state - home navigation is ready to display.
     */
    data object Ready : HomeState
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface HomeIntent : MVIIntent {

    /** Screen appeared - perform any initialization */
    data object ScreenAppeared : HomeIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface HomeAction : MVIAction
// No actions needed for this navigation shell screen
