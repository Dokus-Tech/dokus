package tech.dokus.app.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.navigation.HomeNavigationCommand
import tech.dokus.app.navigation.HomeNavigationCommandBus
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun ShareImportRoute(
    container: ShareImportContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is ShareImportAction.Finish -> {
                navController.navigateTo(CoreDestination.Home) {
                    launchSingleTop = true
                    restoreState = true
                }
                HomeNavigationCommandBus.dispatch(HomeNavigationCommand.OpenDocuments)
            }

            ShareImportAction.NavigateToLogin -> {
                navController.navigateTo(AuthDestination.Login) {
                    popUpTo(0) { inclusive = true }
                }
            }

            ShareImportAction.OpenApp -> {
                navController.navigateTo(CoreDestination.Home) {
                    launchSingleTop = true
                    restoreState = true
                }
                HomeNavigationCommandBus.dispatch(HomeNavigationCommand.OpenDocuments)
            }
        }
    }

    LaunchedEffect(Unit) {
        container.store.intent(ShareImportIntent.Load)
    }

    ShareImportScreen(
        state = state,
        onIntent = { container.store.intent(it) }
    )
}
