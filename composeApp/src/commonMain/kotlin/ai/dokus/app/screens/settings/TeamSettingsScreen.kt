package ai.dokus.app.screens.settings

import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.cancel
import ai.dokus.app.resources.generated.role_admin
import ai.dokus.app.resources.generated.role_admin_desc
import ai.dokus.app.resources.generated.role_accountant
import ai.dokus.app.resources.generated.role_accountant_desc
import ai.dokus.app.resources.generated.role_editor
import ai.dokus.app.resources.generated.role_editor_desc
import ai.dokus.app.resources.generated.role_owner
import ai.dokus.app.resources.generated.role_viewer
import ai.dokus.app.resources.generated.role_viewer_desc
import ai.dokus.app.resources.generated.team_cancel_invitation
import ai.dokus.app.resources.generated.team_change_role
import ai.dokus.app.resources.generated.team_expires
import ai.dokus.app.resources.generated.team_invite_email
import ai.dokus.app.resources.generated.team_invite_member
import ai.dokus.app.resources.generated.team_invite_role
import ai.dokus.app.resources.generated.team_invited_by
import ai.dokus.app.resources.generated.team_joined
import ai.dokus.app.resources.generated.team_members
import ai.dokus.app.resources.generated.team_no_invitations
import ai.dokus.app.resources.generated.team_no_members
import ai.dokus.app.resources.generated.team_owner_badge
import ai.dokus.app.resources.generated.team_pending_invitations
import ai.dokus.app.resources.generated.team_remove_confirm
import ai.dokus.app.resources.generated.team_remove_member
import ai.dokus.app.resources.generated.team_send_invitation
import ai.dokus.app.resources.generated.team_settings_title
import ai.dokus.app.resources.generated.team_transfer_confirm
import ai.dokus.app.resources.generated.team_transfer_ownership
import ai.dokus.app.viewmodel.TeamActionState
import ai.dokus.app.viewmodel.TeamSettingsViewModel
import ai.dokus.foundation.design.components.POutlinedButton
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.model.TeamMember
import ai.dokus.foundation.domain.model.TenantInvitation
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tech.dokus.foundation.app.state.DokusState

/**
 * Team settings screen with top bar.
 * For mobile navigation flow.
 */
@Composable
fun TeamSettingsScreen(
    viewModel: TeamSettingsViewModel = koinViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.team_settings_title)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        TeamSettingsContent(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/**
 * Team settings content without scaffold.
 * Can be embedded in split-pane layout for desktop or used in full-screen for mobile.
 */
@Composable
fun TeamSettingsContent(
    viewModel: TeamSettingsViewModel = koinViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val membersState by viewModel.membersState.collectAsState()
    val invitationsState by viewModel.invitationsState.collectAsState()
    val inviteFormState by viewModel.inviteFormState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()

    // Dialog states
    var showInviteDialog by remember { mutableStateOf(false) }
    var showRemoveConfirmDialog by remember { mutableStateOf<TeamMember?>(null) }
    var showChangeRoleDialog by remember { mutableStateOf<TeamMember?>(null) }
    var showTransferOwnershipDialog by remember { mutableStateOf<TeamMember?>(null) }

    // Load data on first composition
    LaunchedEffect(viewModel) {
        viewModel.loadAll()
    }

    // Handle action state feedback
    LaunchedEffect(actionState) {
        when (actionState) {
            is TeamActionState.Success -> {
                snackbarHostState.showSnackbar((actionState as TeamActionState.Success).message)
                viewModel.resetActionState()
                showInviteDialog = false
            }
            is TeamActionState.Error -> {
                snackbarHostState.showSnackbar((actionState as TeamActionState.Error).message)
                viewModel.resetActionState()
            }
            else -> {}
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .withContentPaddingForScrollable(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Invite Member Button
        PPrimaryButton(
            text = stringResource(Res.string.team_invite_member),
            onClick = { showInviteDialog = true },
            modifier = Modifier.fillMaxWidth()
        )

        // Team Members Section
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.team_members),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(16.dp))

                when {
                    membersState.isLoading() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    membersState.isSuccess() -> {
                        val members = (membersState as DokusState.Success).data
                        if (members.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.team_no_members),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
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
                    else -> {
                        Text(
                            text = "Failed to load team members",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Pending Invitations Section
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.team_pending_invitations),
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(16.dp))

                when {
                    invitationsState.isLoading() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    invitationsState.isSuccess() -> {
                        val invitations = (invitationsState as DokusState.Success).data
                        if (invitations.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.team_no_invitations),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            invitations.forEachIndexed { index, invitation ->
                                InvitationItem(
                                    invitation = invitation,
                                    onCancel = { viewModel.cancelInvitation(invitation.id) }
                                )
                                if (index < invitations.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = "Failed to load invitations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }

    // Invite Dialog
    if (showInviteDialog) {
        InviteDialog(
            email = inviteFormState.email,
            role = inviteFormState.role,
            isInviting = actionState is TeamActionState.Inviting,
            onEmailChange = viewModel::updateInviteEmail,
            onRoleChange = viewModel::updateInviteRole,
            onDismiss = {
                showInviteDialog = false
                viewModel.resetInviteForm()
            },
            onInvite = {
                viewModel.sendInvitation()
            }
        )
    }

    // Remove Confirmation Dialog
    showRemoveConfirmDialog?.let { member ->
        ConfirmationDialog(
            title = stringResource(Res.string.team_remove_member),
            message = stringResource(Res.string.team_remove_confirm),
            onDismiss = { showRemoveConfirmDialog = null },
            onConfirm = {
                viewModel.removeMember(member.userId)
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
                viewModel.updateMemberRole(member.userId, newRole)
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
                viewModel.transferOwnership(member.userId)
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
                text = if (isInviting) "Sending..." else stringResource(Res.string.team_send_invitation),
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
                text = "Save",
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
                text = "Confirm",
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
            dayOfMonth()
            chars(", ")
            year()
        }
        dateTime.format(format)
    } catch (_: Exception) {
        dateTime.toString()
    }
}
