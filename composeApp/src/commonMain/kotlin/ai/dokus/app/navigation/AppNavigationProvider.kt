package ai.dokus.app.navigation

import ai.dokus.app.screens.EmptyScreen
import ai.dokus.app.screens.HomeScreen
import ai.dokus.app.screens.SplashScreen
import ai.dokus.app.screens.UnderDevelopmentScreen
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.destinations.AppDestination
import ai.dokus.foundation.navigation.destinations.CoreDestination
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
    }
}