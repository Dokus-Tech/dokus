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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import tech.dokus.foundation.aura.components.common.DokusLoader
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.viewmodel.TeamSettingsIntent
import tech.dokus.app.viewmodel.TeamSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_confirm
import tech.dokus.aura.resources.action_save
import tech.dokus.aura.resources.cancel
import tech.dokus.aura.resources.state_sending
import tech.dokus.aura.resources.team_invitation_expires
import tech.dokus.aura.resources.team_invitation_info
import tech.dokus.aura.resources.team_member_info
import tech.dokus.aura.resources.team_cancel_invitation
import tech.dokus.aura.resources.team_change_role
import tech.dokus.aura.resources.team_invite_email
import tech.dokus.aura.resources.team_invite_member
import tech.dokus.aura.resources.team_invite_role
import tech.dokus.aura.resources.team_members
import tech.dokus.aura.resources.team_no_invitations
import tech.dokus.aura.resources.team_no_members
import tech.dokus.aura.resources.team_owner_badge
import tech.dokus.aura.resources.team_pending_invitations
import tech.dokus.aura.resources.team_remove_confirm
import tech.dokus.aura.resources.team_remove_member
import tech.dokus.aura.resources.team_send_invitation
import tech.dokus.aura.resources.team_settings_title
import tech.dokus.aura.resources.team_transfer_confirm
import tech.dokus.aura.resources.team_transfer_ownership
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.model.TeamMember
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusSelectableRowGroup
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.local.LocalScreenSize

/**
 * Team settings screen with top bar.
 * Pure UI composable that takes state and callbacks.
 */
@Composable
internal fun TeamSettingsScreen(
    state: TeamSettingsState,
    snackbarHostState: SnackbarHostState,
    showInviteDialog: Boolean,
    onShowInviteDialog: (Boolean) -> Unit,
    onIntent: (TeamSettingsIntent) -> Unit
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    Scaffold(
        topBar = {
            if (!isLargeScreen) PTopAppBar(Res.string.team_settings_title)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        TeamSettingsContent(
            state = state,
            showInviteDialog = showInviteDialog,
            onShowInviteDialog = onShowInviteDialog,
            onIntent = onIntent,
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/**
 * Team settings content without scaffold.
 * Can be embedded in split-pane layout for desktop.
 */
@Composable
fun TeamSettingsContent(
    state: TeamSettingsState,
    showInviteDialog: Boolean,
    onShowInviteDialog: (Boolean) -> Unit,
    onIntent: (TeamSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    TeamSettingsContentInternal(
        state = state,
        showInviteDialog = showInviteDialog,
        onShowInviteDialog = onShowInviteDialog,
        onIntent = onIntent,
        modifier = modifier.padding(contentPadding)
    )
}

@Composable
internal fun TeamSettingsContentInternal(
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
                            DokusLoader()
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
                            DokusLoader()
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

@Suppress("UnusedParameter") // Transfer ownership UI not yet implemented
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
                    style = MaterialTheme.typography.titleLarge,
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
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    Res.string.team_member_info,
                    member.role.localized,
                    formatDate(member.joinedAt)
                ),
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
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(
                    Res.string.team_invitation_info,
                    invitation.role.localized,
                    invitation.invitedByName
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.team_invitation_expires, formatDate(invitation.expiresAt)),
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
    DokusDialog(
        onDismissRequest = { if (!isInviting) onDismiss() },
        title = stringResource(Res.string.team_invite_member),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PTextFieldStandard(
                    fieldName = stringResource(Res.string.team_invite_email),
                    value = email,
                    onValueChange = onEmailChange,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(Res.string.team_invite_role),
                    style = MaterialTheme.typography.labelMedium
                )

                DokusSelectableRowGroup(
                    items = UserRole.assignable,
                    selectedItem = role,
                    onItemSelected = onRoleChange,
                    itemText = { it.localized },
                    enabled = !isInviting,
                )
            }
        },
        primaryAction = DokusDialogAction(
            text = if (isInviting) {
                stringResource(Res.string.state_sending)
            } else {
                stringResource(Res.string.team_send_invitation)
            },
            onClick = onInvite,
            isLoading = isInviting,
            enabled = !isInviting && email.isNotBlank()
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss,
            enabled = !isInviting
        ),
        dismissOnBackPress = !isInviting,
        dismissOnClickOutside = !isInviting
    )
}

@Composable
private fun ChangeRoleDialog(
    currentRole: UserRole,
    onDismiss: () -> Unit,
    onRoleSelected: (UserRole) -> Unit
) {
    var selectedRole by remember { mutableStateOf(currentRole) }

    DokusDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.team_change_role),
        content = {
            DokusSelectableRowGroup(
                items = UserRole.assignable,
                selectedItem = selectedRole,
                onItemSelected = { selectedRole = it },
                itemText = { it.localized },
            )
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_save),
            onClick = { onRoleSelected(selectedRole) },
            enabled = selectedRole != currentRole
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss
        )
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    DokusDialog(
        onDismissRequest = onDismiss,
        title = title,
        content = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_confirm),
            onClick = onConfirm,
            isDestructive = true
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.cancel),
            onClick = onDismiss
        )
    )
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
