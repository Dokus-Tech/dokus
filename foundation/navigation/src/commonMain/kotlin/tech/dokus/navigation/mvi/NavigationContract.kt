package tech.dokus.navigation.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

/**
 * Contract for the Navigation state management.
 *
 * Manages:
 * - Which navigation sections are expanded (desktop rail)
 * - Currently selected destination
 *
 * Only one section can be expanded at a time (accordion behavior).
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class NavigationState(
    val isReady: Boolean = false,
    val expandedSections: Map<String, Boolean> = emptyMap(),
) : MVIState

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface NavigationIntent : MVIIntent {

    /** Initialize navigation state from preferences */
    data object Initialize : NavigationIntent

    /** Toggle a section's expanded state (accordion - collapses others) */
    data class ToggleSection(val sectionId: String) : NavigationIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface NavigationAction : MVIAction
// No actions needed for navigation state
