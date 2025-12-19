package tech.dokus.app.navigation

import tech.dokus.app.screens.EmptyScreen
import tech.dokus.app.screens.HomeScreen
import tech.dokus.app.screens.SplashScreen
import tech.dokus.app.screens.UnderDevelopmentScreen
import tech.dokus.app.screens.settings.AppearanceSettingsScreen
import tech.dokus.app.screens.settings.TeamSettingsScreen
import tech.dokus.app.screens.settings.WorkspaceSettingsScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.AppDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
import ai.dokus.foundation.navigation.destinations.SettingsDestination
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
            WorkspaceSettingsScreen()
        }
        composable<SettingsDestination.TeamSettings> {
            TeamSettingsScreen()
        }
        composable<SettingsDestination.AppearanceSettings> {
            AppearanceSettingsScreen()
        }
    }
}