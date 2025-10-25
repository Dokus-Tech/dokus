package ai.dokus.app.navigation

import ai.dokus.app.screens.DashboardScreen
import ai.dokus.app.screens.SettingsScreen
import ai.dokus.app.screens.UnderDevelopmentScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object HomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Dashboard> {
            DashboardScreen()
        }
        composable<HomeDestination.Settings> {
            SettingsScreen()
        }
        composable<HomeDestination.UnderDevelopment> {
            UnderDevelopmentScreen()
        }
    }
}