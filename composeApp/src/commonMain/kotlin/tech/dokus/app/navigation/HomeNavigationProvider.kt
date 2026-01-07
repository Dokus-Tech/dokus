package tech.dokus.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import tech.dokus.domain.model.PeppolProvider
import tech.dokus.app.screens.DashboardScreen
import tech.dokus.app.screens.DocumentsPlaceholderScreen
import tech.dokus.app.screens.MoreScreen
import tech.dokus.app.screens.UnderDevelopmentScreen
import tech.dokus.app.screens.settings.route.AppearanceSettingsRoute
import tech.dokus.app.screens.settings.route.TeamSettingsRoute
import tech.dokus.app.screens.settings.route.WorkspaceSettingsRoute
import tech.dokus.features.auth.presentation.auth.route.ProfileSettingsRoute
import tech.dokus.features.cashflow.presentation.settings.route.PeppolConnectRoute
import tech.dokus.features.cashflow.presentation.settings.route.PeppolSettingsRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination

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
        composable<SettingsDestination.WorkspaceSettings> {
            WorkspaceSettingsRoute()
        }
        composable<SettingsDestination.PeppolSettings> {
            PeppolSettingsRoute()
        }
        composable<SettingsDestination.PeppolConfiguration.Connect> { backStackEntry ->
            val route = backStackEntry.toRoute<SettingsDestination.PeppolConfiguration.Connect>()
            val provider = PeppolProvider.fromName(route.providerName) ?: PeppolProvider.Recommand
            PeppolConnectRoute(provider = provider)
        }
        composable<SettingsDestination.AppearanceSettings> {
            AppearanceSettingsRoute()
        }
        composable<AuthDestination.ProfileSettings> {
            ProfileSettingsRoute()
        }
        composable<HomeDestination.More> {
            MoreScreen()
        }
        composable<HomeDestination.UnderDevelopment> {
            UnderDevelopmentScreen()
        }
    }
}
