package tech.dokus.app.screens.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
import tech.dokus.aura.resources.team_cancel_invitation
import tech.dokus.aura.resources.team_change_role
import tech.dokus.aura.resources.team_invite_email
import tech.dokus.aura.resources.team_invite_member
import tech.dokus.aura.resources.team_invite_role
import tech.dokus.aura.resources.team_remove_confirm
import tech.dokus.aura.resources.team_remove_member
import tech.dokus.aura.resources.team_send_invitation
import tech.dokus.aura.resources.team_settings_title
import tech.dokus.aura.resources.team_transfer_confirm
import tech.dokus.aura.resources.team_transfer_ownership
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.model.TeamMember
import tech.dokus.domain.model.TenantInvitation
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.MonogramAvatar
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusSelectableRowGroup
import tech.dokus.app.navigation.local.resolveBackNavController
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.badges.TierBadge
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

private val MaxContentWidth = 400.dp
private val ContentPaddingH = 16.dp
private val SectionSpacing = 14.dp

/**
 * Team settings screen with top bar.
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
            if (!isLargeScreen) PTopAppBar(Res.string.team_settings_title, navController = resolveBackNavController())
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
 * Team settings content — Apple Family Sharing style, centered.
 */
@Composable
fun TeamSettingsContent(
    state: TeamSettingsState,
    showInviteDialog: Boolean,
    onShowInviteDialog: (Boolean) -> Unit,
    onIntent: (TeamSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    // Dialog states
    var showRemoveConfirmDialog by remember { mutableStateOf<TeamMember?>(null) }
    var showChangeRoleDialog by remember { mutableStateOf<TeamMember?>(null) }
    var showTransferOwnershipDialog by remember { mutableStateOf<TeamMember?>(null) }

    // Extract data from state
    val contentState = state as? TeamSettingsState.Content
    val members = contentState?.members ?: emptyList()
    val invitations = contentState?.invitations ?: emptyList()
    val isLoading = state is TeamSettingsState.Loading
    val inviteEmail = contentState?.inviteEmail ?: ""
    val inviteRole = contentState?.inviteRole ?: UserRole.Editor
    val isInviting = contentState?.actionState is TeamSettingsState.Content.ActionState.Inviting

    val owner = members.find { it.role == UserRole.Owner }
    val nonOwnerMembers = members.filter { it.role != UserRole.Owner }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .padding(horizontal = ContentPaddingH)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing),
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusLoader()
                    }
                }

                state is TeamSettingsState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Failed to load team",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                else -> {
                    // Owner hero
                    if (owner != null) {
                        OwnerHero(owner = owner)
                    }

                    Spacer(Modifier.height(18.dp))

                    // Members card
                    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            // Owner row (always first in card)
                            if (owner != null) {
                                MemberRow(
                                    member = owner,
                                    isCurrentUser = true,
                                    showDivider = nonOwnerMembers.isNotEmpty() || invitations.isNotEmpty(),
                                )
                            }

                            // Other members
                            nonOwnerMembers.forEachIndexed { index, member ->
                                MemberRow(
                                    member = member,
                                    isCurrentUser = false,
                                    showDivider = index < nonOwnerMembers.lastIndex || invitations.isNotEmpty(),
                                    onChangeRole = { showChangeRoleDialog = member },
                                    onRemove = { showRemoveConfirmDialog = member },
                                )
                            }

                            // Pending invitations
                            invitations.forEachIndexed { index, invitation ->
                                InvitationRow(
                                    invitation = invitation,
                                    showDivider = index < invitations.lastIndex,
                                    onCancel = { onIntent(TeamSettingsIntent.CancelInvitation(invitation.id)) }
                                )
                            }

                            // Invite row
                            InviteRow(onClick = { onShowInviteDialog(true) })
                        }
                    }

                    // Footer note
                    Text(
                        text = "Accountants get read-only export access.\nCore includes 3 seats.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.textFaint,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
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

// =============================================================================
// Hero
// =============================================================================

@Composable
private fun OwnerHero(
    owner: TeamMember,
    modifier: Modifier = Modifier,
) {
    val initials = memberInitials(owner)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        MonogramAvatar(initials = initials, size = 64.dp, radius = 20.dp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = owner.fullName,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02).em,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = owner.email.value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TierBadge(label = "Owner")
            Text(
                text = "since ${formatDate(owner.joinedAt)}",
                fontSize = 10.sp,
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                color = MaterialTheme.colorScheme.textFaint,
            )
        }
    }
}

// =============================================================================
// Member & Invite rows
// =============================================================================

@Composable
private fun MemberRow(
    member: TeamMember,
    isCurrentUser: Boolean,
    showDivider: Boolean,
    onChangeRole: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    val initials = memberInitials(member)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonogramAvatar(initials = initials, size = 34.dp, radius = 10.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.fullName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isCurrentUser) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "You",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }
                if (!isCurrentUser && member.role != UserRole.Owner) {
                    Text(
                        text = member.role.localized,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.textFaint,
                    )
                }
            }
            if (isCurrentUser) {
                StatusDot(type = StatusDotType.Confirmed, pulse = true, size = 5.dp)
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun InvitationRow(
    invitation: TenantInvitation,
    showDivider: Boolean,
    onCancel: () -> Unit,
) {
    val initials = invitation.email.value.take(2).uppercase()

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onCancel)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonogramAvatar(initials = initials, size = 34.dp, radius = 10.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invitation.email.value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Pending \u00b7 ${invitation.role.localized}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.textFaint,
                )
            }
            StatusDot(type = StatusDotType.Warning, size = 5.dp)
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun InviteRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dashed circle placeholder
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.textFaint,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.team_invite_member),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "2 seats available",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.textFaint,
            )
        }
        Text(
            text = "\u203a",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.textFaint,
        )
    }
}

// =============================================================================
// Dialogs (preserved)
// =============================================================================

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

// =============================================================================
// Helpers
// =============================================================================

private fun memberInitials(member: TeamMember): String {
    val first = member.firstName?.value?.firstOrNull()
    val last = member.lastName?.value?.firstOrNull()
    return when {
        first != null && last != null -> "$first$last"
        first != null -> "${first}${member.firstName?.value?.getOrNull(1) ?: ""}"
        last != null -> "${last}${member.lastName?.value?.getOrNull(1) ?: ""}"
        else -> member.email.value.take(2)
    }.uppercase()
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
