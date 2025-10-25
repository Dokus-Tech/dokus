package ai.dokus.app.navigation.secondary

import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.animation.TransitionsProvider
import ai.dokus.foundation.navigation.destinations.AppDestination
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

/**
 * Secondary navigation host for dual-panel layout.
 * This provides an independent navigation stack for the right panel.
 */
@Composable
fun SecondaryNavHost(
    navController: NavHostController,
    navigationProvider: List<NavigationProvider>,
    modifier: Modifier = Modifier
) {
    val largeScreen = LocalScreenSize.isLarge
    val transitionsProvider by remember(largeScreen) {
        derivedStateOf { TransitionsProvider.forRoot(largeScreen) }
    }

    NavHost(
        navController = navController,
        startDestination = AppDestination.Slogan,
        enterTransition = { with(transitionsProvider) { enterTransition } },
        exitTransition = { with(transitionsProvider) { exitTransition } },
        popEnterTransition = { with(transitionsProvider) { popEnterTransition } },
        popExitTransition = { with(transitionsProvider) { popExitTransition } },
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        navigationProvider.forEach { provider ->
            with(provider) {
                registerGraph()
            }
        }
    }
}