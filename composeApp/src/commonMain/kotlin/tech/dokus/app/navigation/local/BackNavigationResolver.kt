package tech.dokus.app.navigation.local

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import tech.dokus.navigation.local.LocalNavController

/**
 * Selects the correct [NavController] for back navigation.
 *
 * Prefers the Home nav controller when it can pop (screen was pushed onto the Home stack),
 * falls back to the root controller, or returns `null` when neither can pop.
 */
@Composable
internal fun resolveBackNavController(): NavController? {
    val homeNav = LocalHomeNavController.current
    val rootNav = LocalNavController.current
    return when {
        homeNav?.previousBackStackEntry != null -> homeNav
        rootNav.previousBackStackEntry != null -> rootNav
        else -> null
    }
}
