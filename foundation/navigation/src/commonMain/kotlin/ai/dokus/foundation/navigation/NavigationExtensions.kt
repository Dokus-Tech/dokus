package ai.dokus.foundation.navigation

import ai.dokus.foundation.navigation.destinations.NavigationDestination
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavUri
import androidx.navigation.compose.currentBackStackEntryAsState

@MainThread
fun <T : Any> NavController.replace(
    route: T,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(route) {
        currentBackStackEntry?.destination?.route?.let { currentRoute ->
            popUpTo(currentRoute) {
                inclusive = true
            }
        }
        builder()
    }
}

@MainThread
inline fun <reified T : NavigationDestination> NavController.navigateTo(
    route: T,
    noinline builder: NavOptionsBuilder.() -> Unit = {},
) {
    if (currentBackStackEntry?.destination?.hasRoute(route::class) == true) return
    navigate(route, builder)
}

@MainThread
fun NavController.navigateTo(
    deepLink: NavUri,
) {
    navigate(deepLink)
}

@MainThread
fun <T : Any> NavController.replaceAll(
    route: T,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    navigate(route) {
        currentBackStackEntry?.destination?.route?.let { currentRoute ->
            popUpTo(route) {
                inclusive = true
            }
        }
        builder()
    }
}

@Composable
fun rememberSelectedDestination(
    navController: NavController,
    destinations: List<NavigationDestination>
): NavigationDestination? {
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    return remember(currentDestination) {
        if (currentDestination == null) return@remember null
        for (possibleDestination in destinations) {
            if (currentDestination.hasRoute(possibleDestination::class)) return@remember possibleDestination
        }
        return@remember null
    }
}