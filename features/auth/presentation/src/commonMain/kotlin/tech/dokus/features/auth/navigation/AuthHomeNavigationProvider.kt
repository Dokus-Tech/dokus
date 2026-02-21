package tech.dokus.features.auth.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import tech.dokus.features.auth.presentation.auth.route.ProfileSettingsRoute
import tech.dokus.navigation.NavigationProvider
import tech.dokus.navigation.destinations.HomeDestination

/**
 * Navigation provider for auth-owned routes within the home NavHost.
 */
internal object AuthHomeNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Profile> {
            ProfileSettingsRoute()
        }
    }
}
