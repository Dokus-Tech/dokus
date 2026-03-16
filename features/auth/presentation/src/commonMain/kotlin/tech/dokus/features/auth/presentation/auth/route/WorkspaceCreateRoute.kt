package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.WorkspaceCreateAction
import tech.dokus.features.auth.mvi.WorkspaceCreateContainer
import tech.dokus.features.auth.presentation.auth.screen.WorkspaceCreateScreen
import tech.dokus.foundation.app.local.LocalBookkeeperConsoleCallback
import tech.dokus.foundation.app.shell.WorkspaceContextStore
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.replace

@Composable
internal fun WorkspaceCreateRoute(
    container: WorkspaceCreateContainer = container()
) {
    val navController = LocalNavController.current
    val bcCallback = LocalBookkeeperConsoleCallback.current

    val state by container.store.subscribe { action ->
        when (action) {
            WorkspaceCreateAction.NavigateHome -> {
                WorkspaceContextStore.selectTenantWorkspace()
                navController.replace(SettingsDestination.PeppolRegistration)
            }
            is WorkspaceCreateAction.NavigateToBookkeeperConsole -> {
                WorkspaceContextStore.selectFirmWorkspace(action.firmId)
                bcCallback?.invoke()
                navController.replace(CoreDestination.Home)
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
    )
}
