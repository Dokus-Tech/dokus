package tech.dokus.app.navigation

import tech.dokus.app.screens.EmptyScreen
import tech.dokus.app.screens.HomeScreen
import tech.dokus.app.screens.SplashScreen
import tech.dokus.app.screens.UnderDevelopmentScreen
import tech.dokus.app.screens.settings.route.AppearanceSettingsRoute
import tech.dokus.app.screens.settings.route.TeamSettingsRoute
import tech.dokus.app.screens.settings.route.WorkspaceSettingsRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.AppDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.destinations.SettingsDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object AppNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<CoreDestination.Splash> {
            SplashScreen()
        }
        composable<CoreDestination.Home> {
            HomeScreen()
        }
        composable<AppDestination.UnderDevelopment> {
            UnderDevelopmentScreen()
        }
        composable<AppDestination.Empty> {
            EmptyScreen()
        }
        // Settings screens
        composable<SettingsDestination.WorkspaceSettings> {
            WorkspaceSettingsRoute()
        }
        composable<SettingsDestination.TeamSettings> {
            TeamSettingsRoute()
        }
        composable<SettingsDestination.AppearanceSettings> {
            AppearanceSettingsRoute()
        }
    }
}