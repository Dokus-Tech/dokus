package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.WorkspaceCreateAction
import tech.dokus.features.auth.mvi.WorkspaceCreateContainer
import tech.dokus.features.auth.presentation.auth.screen.WorkspaceCreateScreen
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.replace

@Composable
internal fun WorkspaceCreateRoute(
    container: WorkspaceCreateContainer = container()
) {
    val navController = LocalNavController.current
    var triggerWarp by remember { mutableStateOf(false) }

    val state by container.store.subscribe { action ->
        when (action) {
            WorkspaceCreateAction.NavigateHome -> {
                triggerWarp = true
            }
            WorkspaceCreateAction.NavigateBack -> navController.navigateUp()
            is WorkspaceCreateAction.ShowCreationError -> {
                // TODO: surface error feedback
            }
        }
    }

    WorkspaceCreateScreen(
        state = state,
        onIntent = { container.store.intent(it) },
        onNavigateUp = { navController.navigateUp() },
        triggerWarp = triggerWarp,
        onWarpComplete = {
            triggerWarp = false
            navController.replace(SettingsDestination.PeppolRegistration)
        },
    )
}
