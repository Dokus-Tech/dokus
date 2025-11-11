package ai.dokus.app.core.extensions

import ai.dokus.foundation.navigation.destinations.AppDestination
import ai.dokus.foundation.navigation.destinations.NavigationDestination
import ai.dokus.foundation.navigation.local.LocalSecondaryNavController
import ai.dokus.foundation.navigation.local.LocalSecondaryNavigationState
import ai.dokus.foundation.navigation.local.SecondaryPanelType
import ai.dokus.foundation.navigation.navigateTo
import ai.dokus.foundation.design.local.LocalScreenSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination.Companion.hasRoute

/**
 * Effect that shows secondary panel with specific route when the composable enters composition.
 * Useful for screens that want to automatically show help content or supplementary information.
 *
 * @param route The destination to navigate to in the secondary panel. If null, the panel will be hidden.
 * @param panelType The type of panel to display (Inline, Complimentary, or Info).
 *                  Defaults to Complimentary which provides a floating panel with elevation.
 */
@Composable
inline fun <reified Destination : NavigationDestination> SetupSecondaryPanel(
    route: Destination?,
    panelType: SecondaryPanelType = SecondaryPanelType.Complimentary,
) {
    val state = LocalSecondaryNavigationState.current
    val navController = LocalSecondaryNavController.current
    val localScreenSize = LocalScreenSize.current

    LaunchedEffect(localScreenSize, route, panelType) {
        if (route != null && localScreenSize.isLarge) {
            state.showPanel(panelType)
            if (navController.currentDestination?.hasRoute<Destination>() == true) {
                return@LaunchedEffect
            }
            navController.navigateTo(route) {
                launchSingleTop = true
            }
        } else {
            navController.navigateTo(AppDestination.Empty)
            state.hidePanel()
        }
    }
}