package tech.dokus.navigation

import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavUri
import androidx.navigation.compose.currentBackStackEntryAsState
import tech.dokus.navigation.destinations.NavigationDestination

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
    if (currentBackStackEntry?.destination?.hasRoute<T>() == true) return
    navigate(route, builder)
}

data class TopLevelTabNavigationPolicy(
    val saveState: Boolean,
    val restoreState: Boolean,
    val launchSingleTop: Boolean,
)

val defaultTopLevelTabNavigationPolicy = TopLevelTabNavigationPolicy(
    saveState = true,
    restoreState = true,
    launchSingleTop = true,
)

@MainThread
fun <T : NavigationDestination> NavController.navigateToTopLevelTab(
    route: T,
    policy: TopLevelTabNavigationPolicy = defaultTopLevelTabNavigationPolicy,
) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = policy.saveState
        }
        launchSingleTop = policy.launchSingleTop
        restoreState = policy.restoreState
    }
}

@MainThread
fun NavController.navigateTo(
    deepLink: NavUri,
) {
    navigate(deepLink)
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
