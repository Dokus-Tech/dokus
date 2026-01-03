package tech.dokus.app.screens.settings.route

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.screens.settings.TeamSettingsScreen
import tech.dokus.app.viewmodel.TeamSettingsAction
import tech.dokus.app.viewmodel.TeamSettingsContainer
import tech.dokus.app.viewmodel.TeamSettingsIntent
import tech.dokus.app.viewmodel.TeamSettingsSuccess
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.team_invite_cancelled
import tech.dokus.aura.resources.team_invite_success
import tech.dokus.aura.resources.team_member_removed_success
import tech.dokus.aura.resources.team_ownership_transferred_success
import tech.dokus.aura.resources.team_role_update_success
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.extensions.localized

@Composable
internal fun TeamSettingsRoute(
    container: TeamSettingsContainer = container()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<TeamSettingsSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var showInviteDialog by remember { mutableStateOf(false) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            TeamSettingsSuccess.InviteSent -> stringResource(Res.string.team_invite_success)
            TeamSettingsSuccess.InviteCancelled -> stringResource(Res.string.team_invite_cancelled)
            TeamSettingsSuccess.RoleUpdated -> stringResource(Res.string.team_role_update_success)
            TeamSettingsSuccess.MemberRemoved -> stringResource(Res.string.team_member_removed_success)
            TeamSettingsSuccess.OwnershipTransferred ->
                stringResource(Res.string.team_ownership_transferred_success)
        }
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = null
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is TeamSettingsAction.ShowSuccess -> {
                pendingSuccess = action.success
            }
            is TeamSettingsAction.ShowError -> {
                pendingError = action.error
            }
            TeamSettingsAction.DismissInviteDialog -> {
                showInviteDialog = false
            }
        }
    }

    // Load data on first composition
    LaunchedEffect(Unit) {
        container.store.intent(TeamSettingsIntent.Load)
    }

    TeamSettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        showInviteDialog = showInviteDialog,
        onShowInviteDialog = { showInviteDialog = it },
        onIntent = { container.store.intent(it) }
    )
}
