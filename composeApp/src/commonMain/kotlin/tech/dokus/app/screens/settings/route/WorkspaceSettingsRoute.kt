package tech.dokus.app.screens.settings.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.WorkspaceSettingsScreen
import tech.dokus.app.viewmodel.WorkspaceSettingsContainer
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.workspace_settings_title
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.shell.RegisterHomeShellTopBar
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.SettingsDestination
import tech.dokus.navigation.destinations.route
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

@Composable
internal fun WorkspaceSettingsRoute(
    container: WorkspaceSettingsContainer = container()
) {
    val navController = LocalNavController.current

    val workspaceTitle = stringResource(Res.string.workspace_settings_title)

    RegisterHomeShellTopBar(
        route = HomeDestination.WorkspaceDetails.route,
        config = remember(workspaceTitle) {
            HomeShellTopBarConfig(mode = HomeShellTopBarMode.Title(title = workspaceTitle))
        }
    )

    val state by container.store.subscribe(DefaultLifecycle) { _ -> }

    // Load settings on first composition
    LaunchedEffect(Unit) {
        container.store.intent(WorkspaceSettingsIntent.Load)
    }

    WorkspaceSettingsScreen(
        state = state,
        onIntent = { container.store.intent(it) },
        onNavigateToPeppol = { navController.navigateTo(SettingsDestination.PeppolRegistration) },
    )
}
