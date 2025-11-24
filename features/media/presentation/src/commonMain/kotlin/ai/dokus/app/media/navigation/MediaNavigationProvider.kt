package ai.dokus.app.media.navigation

import ai.dokus.app.media.screens.MediaScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.HomeDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

internal object MediaNavigationProvider : NavigationProvider {
    override fun NavGraphBuilder.registerGraph() {
        composable<HomeDestination.Media> {
            MediaScreen()
        }
    }
}
