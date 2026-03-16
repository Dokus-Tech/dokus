package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.features.auth.mvi.WorkspaceSelectAction
import tech.dokus.features.auth.mvi.WorkspaceSelectContainer
import tech.dokus.features.auth.presentation.auth.screen.WorkspaceSelectScreen
import tech.dokus.foundation.app.local.LocalBookkeeperConsoleCallback
import tech.dokus.foundation.app.shell.WorkspaceContextStore
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
import tech.dokus.navigation.replace

@Composable
internal fun WorkspaceSelectRoute(
    container: WorkspaceSelectContainer = container()
) {
    val navController = LocalNavController.current
    val bcCallback = LocalBookkeeperConsoleCallback.current

    val state by container.store.subscribe { action ->
        when (action) {
            WorkspaceSelectAction.NavigateToHome -> {
                WorkspaceContextStore.selectTenantWorkspace()
                navController.replace(CoreDestination.Home)
            }
            is WorkspaceSelectAction.NavigateToBookkeeperConsole -> {
                WorkspaceContextStore.selectFirmWorkspace(action.firmId)
                bcCallback?.invoke()
                navController.replace(CoreDestination.Home)
            }
            is WorkspaceSelectAction.ShowSelectionError -> {
                // TODO: surface error feedback
            }
        }
    }

    WorkspaceSelectScreen(
        state = state,
        onIntent = { container.store.intent(it) },
        onAddTenantClick = { navController.navigateTo(AuthDestination.WorkspaceCreate) },
    )
}
