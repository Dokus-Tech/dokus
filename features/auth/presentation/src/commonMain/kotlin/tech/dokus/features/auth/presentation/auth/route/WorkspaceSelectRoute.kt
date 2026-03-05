package tech.dokus.features.auth.presentation.auth.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var triggerWarp by remember { mutableStateOf(false) }
    var pendingConsoleNavigation by remember { mutableStateOf(false) }

    // WorkspaceContextStore is mutated here because the route is the coordination
    // point between the auth flow action and the app shell's surface context.
    // The container emits the navigation action; the route translates it into a
    // global context switch before triggering the warp animation.
    val state by container.store.subscribe { action ->
        when (action) {
            WorkspaceSelectAction.NavigateToHome -> {
                WorkspaceContextStore.selectTenantWorkspace()
                triggerWarp = true
            }
            is WorkspaceSelectAction.NavigateToBookkeeperConsole -> {
                WorkspaceContextStore.selectFirmWorkspace(action.firmId)
                pendingConsoleNavigation = true
                triggerWarp = true
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
        triggerWarp = triggerWarp,
        onWarpComplete = {
            if (pendingConsoleNavigation) {
                pendingConsoleNavigation = false
                bcCallback?.invoke()
            }
            triggerWarp = false
            navController.replace(CoreDestination.Home)
        },
    )
}
