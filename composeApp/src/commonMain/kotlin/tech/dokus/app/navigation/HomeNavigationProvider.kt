package tech.dokus.app.navigation

import tech.dokus.app.screens.DashboardScreen
import tech.dokus.app.screens.SettingsScreen
import tech.dokus.app.screens.UnderDevelopmentScreen
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