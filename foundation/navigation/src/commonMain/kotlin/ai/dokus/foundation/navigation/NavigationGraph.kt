package ai.dokus.foundation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

/**
 * Main navigation graph for the application
 */
@Composable
fun NavigationGraph(
    navController: NavHostController = rememberNavController()
) {
    val navigator = AppNavigator(navController)

    NavHost(
        navController = navController,
        startDestination = AppRoutes.SPLASH
    ) {
    }
}

/**
 * Navigation graph for home tabs
 */
@Composable
fun HomeTabNavigationGraph(
    navController: NavHostController = rememberNavController(),
    parentNavigator: AppNavigator
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.TAB_DASHBOARD
    ) {
    }
}