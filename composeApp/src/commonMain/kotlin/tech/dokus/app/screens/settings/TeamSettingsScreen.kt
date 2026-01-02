package tech.dokus.app.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.viewmodel.TeamSettingsAction
import tech.dokus.app.viewmodel.TeamSettingsContainer
import tech.dokus.app.viewmodel.TeamSettingsIntent
import tech.dokus.app.viewmodel.TeamSettingsState
import tech.dokus.app.viewmodel.TeamSettingsSuccess
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_confirm
import tech.dokus.aura.resources.action_save
import tech.dokus.aura.resources.cancel
import tech.dokus.aura.resources.role_accountant
import tech.dokus.aura.resources.role_accountant_desc
import tech.dokus.aura.resources.role_admin
import tech.dokus.aura.resources.role_admin_desc
import tech.dokus.aura.resources.role_editor
import tech.dokus.aura.resources.role_editor_desc
import tech.dokus.aura.resources.role_owner
import tech.dokus.aura.resources.role_viewer
import tech.dokus.aura.resources.role_viewer_desc
import tech.dokus.aura.resources.state_sending
import tech.dokus.aura.resources.team_cancel_invitation
import tech.dokus.aura.resources.team_change_role
import tech.dokus.aura.resources.team_expires
import tech.dokus.aura.resources.team_invite_cancelled
import tech.dokus.aura.resources.team_invite_email
import tech.dokus.aura.resources.team_invite_member
import tech.dokus.aura.resources.team_invite_role
import tech.dokus.aura.resources.team_invite_success
import tech.dokus.aura.resources.team_invited_by
import tech.dokus.aura.resources.team_joined
import tech.dokus.aura.resources.team_member_removed_success
import tech.dokus.aura.resources.team_members
import tech.dokus.aura.resources.team_no_invitations
import tech.dokus.aura.resources.team_no_members
import tech.dokus.aura.resources.team_owner_badge
import tech.dokus.aura.resources.team_ownership_transferred_success
import tech.dokus.aura.resources.team_pending_invitations
import tech.dokus.aura.resources.team_remove_confirm
import tech.dokus.aura.resources.team_remove_member
import tech.dokus.aura.resources.team_role_update_success
import tech.dokus.aura.resources.team_send_invitation
import tech.dokus.aura.resources.team_settings_title
import tech.dokus.aura.resources.team_transfer_confirm
import tech.dokus.aura.resources.team_transfer_ownership
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.extensions.localized

/**
 * Team settings screen with top bar using FlowMVI Container pattern.
 * For mobile navigation flow.
 */
@Composable
internal fun TeamSettingsScreen(
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

    Scaffold(
        topBar = {
            PTopAppBar(Res.string.team_settings_title)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        TeamSettingsContentInternal(
            state = state,
            showInviteDialog = showInviteDialog,
            onShowInviteDialog = { showInviteDialog = it },
            onIntent = { container.store.intent(it) },
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/**
 * Team settings content without scaffold using FlowMVI Container pattern.
 * Can be embedded in split-pane layout for desktop or used in full-screen for mobile.
 */
@Composable
internal fun TeamSettingsContent(
    container: TeamSettingsContainer = container(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
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

    TeamSettingsContentInternal(
        state = state,
        showInviteDialog = showInviteDialog,
        onShowInviteDialog = { showInviteDialog = it },
        onIntent = { container.store.intent(it) },
        modifier = modifier.padding(contentPadding)
    )
}

@Composable
private fun TeamSettingsContentInternal(
    state: TeamSettingsState,
    showInviteDialog: Boolean,
    onShowInviteDialog: (Boolean) -> Unit,
    onIntent: (TeamSettingsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Dialog states
    var showRemoveConfirmDialog by remember { mutableStateOf<TeamMember?>(null) }
    var showChangeRoleDialog by remember { mutableStateOf<TeamMember?>(null) }
    var showTransferOwnershipDialog by remember { mutableStateOf<TeamMember?>(null) }

    // Extract data from state
    val contentState = state as? TeamSettingsState.Content
    val members = contentState?.members ?: emptyList()
    val invitations = contentState?.invitations ?: emptyList()
    val membersLoading = contentState?.membersLoading == true || state is TeamSettingsState.Loading
    val invitationsLoading = contentState?.invitationsLoading == true || state is TeamSettingsState.Loading
    val inviteEmail = contentState?.inviteEmail ?: ""
    val inviteRole = contentState?.inviteRole ?: UserRole.Editor
    val isInviting = contentState?.actionState is TeamSettingsState.Content.ActionState.Inviting

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .withContentPaddingForScrollable(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Invite Member Button
        PPrimaryButton(
            text = stringResource(Res.string.team_invite_member),
            onClick = { onShowInviteDialog(true) },
            modifier = Modifier.fillMaxWidth()
        )

        // Team Members Section
        DokusCard(
            modifier = Modifier.fillMaxWidth(),
            padding = DokusCardPadding.Default,
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.team_members),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(16.dp))

                when {
                    membersLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    members.isEmpty() -> {
                        Text(
                            text = stringResource(Res.string.team_no_members),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        members.forEachIndexed { index, member ->
                            TeamMemberItem(
                                member = member,
                                onChangeRole = { showChangeRoleDialog = member },
                                onRemove = { showRemoveConfirmDialog = member },
                                onTransferOwnership = { showTransferOwnershipDialog = member }
                            )
                            if (index < members.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        // Pending Invitations Section
        DokusCard(
            modifier = Modifier.fillMaxWidth(),
            padding = DokusCardPadding.Default,
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.team_pending_invitations),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(16.dp))

                when {
                    invitationsLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    invitations.isEmpty() -> {
                        Text(
                            text = stringResource(Res.string.team_no_invitations),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        invitations.forEachIndexed { index, invitation ->
                            InvitationItem(
                                invitation = invitation,
                                onCancel = { onIntent(TeamSettingsIntent.CancelInvitation(invitation.id)) }
                            )
                            if (index < invitations.lastIndex) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    // Invite Dialog
    if (showInviteDialog) {
        InviteDialog(
            email = inviteEmail,
            role = inviteRole,
            isInviting = isInviting,
            onEmailChange = { onIntent(TeamSettingsIntent.UpdateInviteEmail(it)) },
            onRoleChange = { onIntent(TeamSettingsIntent.UpdateInviteRole(it)) },
            onDismiss = {
                onShowInviteDialog(false)
                onIntent(TeamSettingsIntent.ResetInviteForm)
            },
            onInvite = { onIntent(TeamSettingsIntent.SendInvitation) }
        )
    }

    // Remove Confirmation Dialog
    showRemoveConfirmDialog?.let { member ->
        ConfirmationDialog(
            title = stringResource(Res.string.team_remove_member),
            message = stringResource(Res.string.team_remove_confirm),
            onDismiss = { showRemoveConfirmDialog = null },
            onConfirm = {
                onIntent(TeamSettingsIntent.RemoveMember(member.userId))
                showRemoveConfirmDialog = null
            }
        )
    }

    // Change Role Dialog
    showChangeRoleDialog?.let { member ->
        ChangeRoleDialog(
            currentRole = member.role,
            onDismiss = { showChangeRoleDialog = null },
            onRoleSelected = { newRole ->
                onIntent(TeamSettingsIntent.UpdateMemberRole(member.userId, newRole))
                showChangeRoleDialog = null
            }
        )
    }

    // Transfer Ownership Dialog
    showTransferOwnershipDialog?.let { member ->
        ConfirmationDialog(
            title = stringResource(Res.string.team_transfer_ownership),
            message = stringResource(Res.string.team_transfer_confirm),
            onDismiss = { showTransferOwnershipDialog = null },
            onConfirm = {
                onIntent(TeamSettingsIntent.TransferOwnership(member.userId))
                showTransferOwnershipDialog = null
            }
        )
    }
}

@Composable
private fun TeamMemberItem(
    member: TeamMember,
    onChangeRole: () -> Unit,
    onRemove: () -> Unit,
    onTransferOwnership: () -> Unit
) {
    val isOwner = member.role == UserRole.Owner

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.fullName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (isOwner) {
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(Res.string.team_owner_badge)) }
                    )
                }
            }
            Text(
                text = member.email.value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${getRoleDisplayName(member.role)} - ${stringResource(Res.string.team_joined)} ${formatDate(member.joinedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Action buttons (hidden for Owner)
        if (!isOwner) {
            TextButton(onClick = onChangeRole) {
                Text(stringResource(Res.string.team_change_role))
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.team_remove_member),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun InvitationItem(
    invitation: TenantInvitation,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = invitation.email.value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${getRoleDisplayName(invitation.role)} - ${stringResource(Res.string.team_invited_by)} ${invitation.invitedByName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${stringResource(Res.string.team_expires)} ${formatDate(invitation.expiresAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        POutlinedButton(
            text = stringResource(Res.string.team_cancel_invitation),
            onClick = onCancel
        )
    }
}

@Composable
private fun InviteDialog(
    email: String,
    role: UserRole,
    isInviting: Boolean,
    onEmailChange: (String) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onDismiss: () -> Unit,
    onInvite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.team_invite_member)) },
        text = {
            Column {
                PTextFieldStandard(
                    fieldName = stringResource(Res.string.team_invite_email),
                    value = email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.team_invite_role),
                    style = MaterialTheme.typography.labelMedium
                )

                Spacer(Modifier.height(8.dp))

                RoleSelector(
                    selectedRole = role,
                    onRoleSelected = onRoleChange
                )
            }
        },
        confirmButton = {
            PPrimaryButton(
                text = if (isInviting) {
                    stringResource(Res.string.state_sending)
                } else {
                    stringResource(Res.string.team_send_invitation)
                },
                enabled = !isInviting && email.isNotBlank(),
                onClick = onInvite
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun ChangeRoleDialog(
    currentRole: UserRole,
    onDismiss: () -> Unit,
    onRoleSelected: (UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(currentRole) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.team_change_role)) },
        text = {
            RoleSelector(
                selectedRole = selectedRole,
                onRoleSelected = { selectedRole = it }
            )
        },
        confirmButton = {
            PPrimaryButton(
                text = stringResource(Res.string.action_save),
                enabled = selectedRole != currentRole,
                onClick = { onRoleSelected(selectedRole) }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun RoleSelector(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        UserRole.assignable.forEach { role ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = role == selectedRole,
                        onClick = { onRoleSelected(role) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = role == selectedRole,
                    onClick = null
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = getRoleDisplayName(role),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = getRoleDescription(role),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            PPrimaryButton(
                text = stringResource(Res.string.action_confirm),
                onClick = onConfirm
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

@Composable
private fun getRoleDisplayName(role: UserRole): String {
    return when (role) {
        UserRole.Owner -> stringResource(Res.string.role_owner)
        UserRole.Admin -> stringResource(Res.string.role_admin)
        UserRole.Accountant -> stringResource(Res.string.role_accountant)
        UserRole.Editor -> stringResource(Res.string.role_editor)
        UserRole.Viewer -> stringResource(Res.string.role_viewer)
    }
}

@Composable
private fun getRoleDescription(role: UserRole): String {
    return when (role) {
        UserRole.Owner -> ""
        UserRole.Admin -> stringResource(Res.string.role_admin_desc)
        UserRole.Accountant -> stringResource(Res.string.role_accountant_desc)
        UserRole.Editor -> stringResource(Res.string.role_editor_desc)
        UserRole.Viewer -> stringResource(Res.string.role_viewer_desc)
    }
}

private fun formatDate(dateTime: LocalDateTime): String {
    return try {
        val format = LocalDateTime.Format {
            monthName(MonthNames.ENGLISH_ABBREVIATED)
            char(' ')
            day(padding = Padding.ZERO)
            chars(", ")
            year()
        }
        dateTime.format(format)
    } catch (_: Exception) {
        dateTime.toString()
    }
}
