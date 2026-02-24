package tech.dokus.app.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import tech.dokus.app.screens.AccountantScreen
import tech.dokus.app.screens.MoreRoute
import tech.dokus.app.screens.UnderDevelopmentScreen
import tech.dokus.app.screens.search.SearchRoute
import tech.dokus.app.screens.settings.route.TeamSettingsRoute
import tech.dokus.app.screens.settings.route.WorkspaceSettingsRoute
import tech.dokus.app.screens.today.TodayRoute
import tech.dokus.app.screens.tomorrow.TomorrowRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

internal object HomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Today> {
            TodayRoute()
        }
        composable<HomeDestination.Team> {
            TeamSettingsRoute()
        }
        composable<HomeDestination.WorkspaceDetails> {
            WorkspaceSettingsRoute()
        }
        composable<HomeDestination.Accountant> {
            AccountantScreen()
        }
        composable<HomeDestination.Search> {
            SearchRoute()
        }
        composable<HomeDestination.More> {
            MoreRoute()
        }
        composable<HomeDestination.Tomorrow> {
            TomorrowRoute()
        }
        composable<HomeDestination.UnderDevelopment> {
            UnderDevelopmentScreen()
        }
    }
}
