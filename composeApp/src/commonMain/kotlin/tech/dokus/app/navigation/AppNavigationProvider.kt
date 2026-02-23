package tech.dokus.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import tech.dokus.app.screens.EmptyScreen
import tech.dokus.app.screens.HomeRoute
import tech.dokus.app.screens.SplashRoute
import tech.dokus.app.screens.UnderDevelopmentScreen
import tech.dokus.app.screens.settings.route.AppearanceSettingsRoute
import tech.dokus.app.screens.settings.route.NotificationPreferencesRoute
import tech.dokus.app.screens.settings.route.TeamSettingsRoute
import tech.dokus.app.screens.settings.route.WorkspaceSettingsRoute
import tech.dokus.app.share.ShareImportRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.AppDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.destinations.SettingsDestination

internal object AppNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<CoreDestination.Splash> {
            SplashRoute()
        }
        composable<CoreDestination.Home> {
            HomeRoute()
        }
        composable<AppDestination.UnderDevelopment> {
            UnderDevelopmentScreen()
        }
        composable<AppDestination.Empty> {
            EmptyScreen()
        }
        composable<AppDestination.ShareImport> {
            ShareImportRoute()
        }
        composable<SettingsDestination.WorkspaceSettings> {
            WorkspaceSettingsRoute()
        }
        composable<SettingsDestination.TeamSettings> {
            TeamSettingsRoute()
        }
        composable<SettingsDestination.AppearanceSettings> {
            AppearanceSettingsRoute()
        }
        composable<SettingsDestination.NotificationPreferences> {
            NotificationPreferencesRoute()
        }
    }
}
