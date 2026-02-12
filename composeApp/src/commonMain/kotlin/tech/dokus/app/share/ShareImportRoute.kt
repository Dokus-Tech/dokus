package tech.dokus.app.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.replace

@Composable
internal fun ShareImportRoute(
    container: ShareImportContainer = container()
) {
    val navController = LocalNavController.current

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is ShareImportAction.Finish -> {
                navController.replace(HomeDestination.Documents)
            }

            ShareImportAction.NavigateToLogin -> {
                navController.replace(AuthDestination.Login)
            }

            ShareImportAction.OpenApp -> {
                navController.replace(HomeDestination.Documents)
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
