package ai.dokus.foundation.navigation.local

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

/**
 * CompositionLocal for accessing SecondaryNavigationState from any composable.
 */
val LocalSecondaryNavigationState = staticCompositionLocalOf<SecondaryNavigationState> { error("No SecondaryNavigationState provided") }

/**
 * CompositionLocal for accessing secondary NavController from any composable.
 */
val LocalSecondaryNavController = staticCompositionLocalOf<NavHostController> { error("No secondary NavController provided") }

/**
 * Composable helper to remember the SecondaryNavigationState with persistence.
 */
@Composable
fun rememberSecondaryNavigationState(): SecondaryNavigationState {
    // Keep state instance stable across recompositions
    val state = remember { SecondaryNavigationState() }

    // Persist and restore visibility state
    var isPanelVisible by rememberSaveable { mutableStateOf(false) }

    // Only restore state once on first composition
    LaunchedEffect(Unit) {
        state.setPanelVisibility(isPanelVisible)
    }

    // Update saved state when visibility changes
    LaunchedEffect(state.isPanelVisible) {
        state.isPanelVisible.collect { visible ->
            isPanelVisible = visible
        }
    }

    return state
}