package tech.dokus.app.screens.settings.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.TeamSettingsScreen
import tech.dokus.app.viewmodel.TeamSettingsAction
import tech.dokus.app.viewmodel.TeamSettingsContainer
import tech.dokus.app.viewmodel.TeamSettingsIntent
import tech.dokus.foundation.app.mvi.container

@Composable
internal fun TeamSettingsRoute(
    container: TeamSettingsContainer = container()
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    var showBookkeeperDialog by remember { mutableStateOf(false) }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            TeamSettingsAction.DismissInviteDialog -> {
                showInviteDialog = false
            }
            TeamSettingsAction.DismissBookkeeperDialog -> {
                showBookkeeperDialog = false
            }
        }
    }

    // Load data on first composition
    LaunchedEffect(Unit) {
        container.store.intent(TeamSettingsIntent.Load)
    }

    TeamSettingsScreen(
        state = state,
        showInviteDialog = showInviteDialog,
        onShowInviteDialog = { showInviteDialog = it },
        showBookkeeperDialog = showBookkeeperDialog,
        onShowBookkeeperDialog = { showBookkeeperDialog = it },
        onIntent = { container.store.intent(it) }
    )
}
