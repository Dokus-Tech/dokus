package tech.dokus.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import tech.dokus.app.screens.DashboardScreen
import tech.dokus.app.screens.DocumentsPlaceholderScreen
import tech.dokus.app.screens.MoreScreen
import tech.dokus.app.screens.SettingsScreen
import tech.dokus.app.screens.UnderDevelopmentScreen
import tech.dokus.app.screens.settings.route.TeamSettingsRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

internal object HomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Dashboard> {
            DashboardScreen()
        }
        composable<HomeDestination.Documents> {
            DocumentsPlaceholderScreen()
        }
        composable<HomeDestination.Team> {
            TeamSettingsRoute()
        }
        composable<HomeDestination.Settings> {
            SettingsScreen()
        }
        composable<HomeDestination.More> {
            MoreScreen()
        }
        composable<HomeDestination.UnderDevelopment> {
            UnderDevelopmentScreen()
        }
    }
}
