package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_change
import tech.dokus.aura.resources.action_upload
import tech.dokus.aura.resources.common_empty_value
import tech.dokus.aura.resources.profile_cancel
import tech.dokus.aura.resources.profile_change_password
import tech.dokus.aura.resources.profile_danger_zone
import tech.dokus.aura.resources.profile_deactivate_account
import tech.dokus.aura.resources.profile_deactivate_warning
import tech.dokus.aura.resources.profile_edit
import tech.dokus.aura.resources.profile_email
import tech.dokus.aura.resources.profile_email_not_verified
import tech.dokus.aura.resources.profile_email_verification
import tech.dokus.aura.resources.profile_email_verified
import tech.dokus.aura.resources.profile_first_name
import tech.dokus.aura.resources.profile_last_name
import tech.dokus.aura.resources.profile_load_failed
import tech.dokus.aura.resources.profile_logout
import tech.dokus.aura.resources.profile_personal_info
import tech.dokus.aura.resources.profile_server_change
import tech.dokus.aura.resources.profile_server_dokus_cloud
import tech.dokus.aura.resources.profile_server_label
import tech.dokus.aura.resources.profile_server_reset_to_cloud
import tech.dokus.aura.resources.profile_server_url
import tech.dokus.aura.resources.profile_server_version
import tech.dokus.aura.resources.profile_sessions_description
import tech.dokus.aura.resources.profile_version_footer
import tech.dokus.aura.resources.profile_resend_verification
import tech.dokus.aura.resources.profile_save
import tech.dokus.aura.resources.profile_sessions
import tech.dokus.aura.resources.state_uploading
import tech.dokus.aura.resources.user_avatar_content_description
import tech.dokus.domain.Name
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.model.User
import tech.dokus.features.auth.mvi.ProfileSettingsState
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.app.network.rememberResolvedApiUrl
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.EditableAvatarSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.UserAvatarImage
import tech.dokus.foundation.aura.components.badges.TierBadge
import tech.dokus.foundation.aura.components.fields.PTextFieldName
import tech.dokus.foundation.aura.components.settings.SettingsRow
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.components.text.DokusLabel
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.domain.Email
import tech.dokus.domain.ids.UserId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.datetime.LocalDateTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// =============================================================================
// v2 Profile Sections
// =============================================================================

private val EditableProfileAvatarSize = 88.dp
private val EditableProfileAvatarRadius = 28.dp

/**
 * Profile hero: centered avatar, name, email, tier badges.
 */
@Composable
internal fun ProfileHero(
    user: User,
    avatarState: ProfileSettingsState.AvatarState,
    onUploadAvatar: () -> Unit,
    onResetAvatarState: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = userDisplayName(user)
    val initials = userInitials(user)
    val avatarUrl = rememberResolvedApiUrl(user.avatar?.medium)
    val imageLoader = rememberAuthenticatedImageLoader()
    val isAvatarBusy = avatarState is ProfileSettingsState.AvatarState.Uploading
    val uploadProgress = (avatarState as? ProfileSettingsState.AvatarState.Uploading)?.progress
    val editDescription = stringResource(
        if (user.avatar != null) Res.string.action_change else Res.string.action_upload
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(bottom = 14.dp),
    ) {
        EditableAvatarSurface(
            onEditClick = onUploadAvatar,
            editContentDescription = editDescription,
            modifier = Modifier.size(EditableProfileAvatarSize),
            shape = RoundedCornerShape(EditableProfileAvatarRadius),
            enabled = !isAvatarBusy,
            isBusy = isAvatarBusy,
            progress = uploadProgress,
        ) {
            UserAvatarImage(
                avatarUrl = avatarUrl,
                initials = initials,
                size = EditableProfileAvatarSize,
                radius = EditableProfileAvatarRadius,
                imageLoader = imageLoader,
                contentDescription = stringResource(Res.string.user_avatar_content_description),
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = name,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.02).em,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = user.email.value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TierBadge(label = "Core")
            TierBadge(label = "Owner")
        }
        ProfileAvatarStateIndicator(
            avatarState = avatarState,
            onResetState = onResetAvatarState,
        )
    }
}

@Composable
private fun ProfileAvatarStateIndicator(
    avatarState: ProfileSettingsState.AvatarState,
    onResetState: () -> Unit,
) {
    when (avatarState) {
        is ProfileSettingsState.AvatarState.Uploading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                CircularProgressIndicator(
                    progress = { avatarState.progress },
                    modifier = Modifier.height(16.dp).width(16.dp)
                )
                Text(
                    text = stringResource(Res.string.state_uploading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        is ProfileSettingsState.AvatarState.Error -> {
            Text(
                text = avatarState.error.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        ProfileSettingsState.AvatarState.Success -> {
            LaunchedEffect(Unit) {
                onResetState()
            }
        }

        ProfileSettingsState.AvatarState.Idle -> Unit
    }
}

/**
 * Account card: email, name, verification status, verify/edit actions.
 */
@Composable
internal fun AccountCard(
    user: User,
    isResendingVerification: Boolean,
    onResendVerification: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fullName = userDisplayName(user)
    val isVerified = user.emailVerified

    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            SettingsRow(label = stringResource(Res.string.profile_email), value = user.email.value, mono = true)
            SettingsRow(label = stringResource(Res.string.profile_personal_info), value = fullName)
            SettingsRow(
                label = stringResource(Res.string.profile_email_verification),
                trailing = {
                    StatusDot(
                        type = if (isVerified) StatusDotType.Confirmed else StatusDotType.Warning,
                    )
                    Text(
                        text = if (isVerified) {
                            stringResource(Res.string.profile_email_verified)
                        } else {
                            stringResource(Res.string.profile_email_not_verified)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isVerified) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                },
            )
            if (!isVerified) {
                SettingsRow(
                    label = stringResource(Res.string.profile_resend_verification),
                    chevron = true,
                    showDivider = false,
                    onClick = onResendVerification,
                )
            }
        }
    }
}

/**
 * Security card: change password, active sessions.
 */
@Composable
internal fun SecurityCard(
    onChangePassword: () -> Unit,
    onMySessions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            SettingsRow(
                label = stringResource(Res.string.profile_change_password),
                chevron = true,
                onClick = onChangePassword,
            )
            SettingsRow(
                label = stringResource(Res.string.profile_sessions),
                value = stringResource(Res.string.profile_sessions_description),
                chevron = true,
                showDivider = false,
                onClick = onMySessions,
            )
        }
    }
}

/**
 * Server connection card with DokusLabel header + StatusDot + SettingsRows.
 */
@Composable
internal fun ServerCard(
    currentServer: ServerConfig,
    onChangeServer: () -> Unit,
    onResetToCloud: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cloudName = stringResource(Res.string.profile_server_dokus_cloud)
    val serverName = currentServer.name ?: if (currentServer.isCloud) cloudName else currentServer.host

    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            // Header with label + green pulse dot
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DokusLabel(text = stringResource(Res.string.profile_server_label))
                StatusDot(type = StatusDotType.Confirmed, pulse = true)
            }

            SettingsRow(label = stringResource(Res.string.profile_server_label), value = serverName)
            SettingsRow(
                label = stringResource(Res.string.profile_server_url),
                value = currentServer.baseUrl
                    .removePrefix("https://")
                    .removePrefix("http://"),
                mono = true
            )
            if (currentServer.version != null) {
                SettingsRow(label = stringResource(Res.string.profile_server_version), value = currentServer.version!!, mono = true)
            }
            SettingsRow(
                label = stringResource(Res.string.profile_server_change),
                chevron = true,
                showDivider = !currentServer.isCloud,
                onClick = onChangeServer,
            )

            // Reset to cloud (only on self-hosted)
            if (!currentServer.isCloud) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.profile_server_reset_to_cloud),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * Danger zone card with red label + deactivation action.
 */
@Composable
internal fun DangerZoneCard(
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            Box(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                DokusLabel(text = stringResource(Res.string.profile_danger_zone), color = MaterialTheme.colorScheme.error)
            }
            Box(modifier = Modifier.padding(horizontal = 18.dp).padding(bottom = 8.dp)) {
                Text(
                    text = stringResource(Res.string.profile_deactivate_warning),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
            SettingsRow(
                label = stringResource(Res.string.profile_deactivate_account),
                destructive = true,
                showDivider = false,
            )
        }
    }
}

/**
 * Log out card: single destructive row.
 */
@Composable
internal fun LogOutCard(
    isLoggingOut: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        SettingsRow(
            label = stringResource(Res.string.profile_logout),
            destructive = true,
            showDivider = false,
            onClick = if (isLoggingOut) null else onLogout,
        )
    }
}

/**
 * Version footer: "Dokus v0.1.0 · Core" centered mono textFaint.
 */
@Composable
internal fun VersionFooter(
    modifier: Modifier = Modifier,
) {
    Text(
        text = stringResource(Res.string.profile_version_footer, "0.1.0", "Core"),
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        textAlign = TextAlign.Center,
        fontSize = 10.sp,
        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
        color = MaterialTheme.colorScheme.textFaint,
    )
}

// =============================================================================
// Editing / Saving / Error sections (kept for backward compat)
// =============================================================================

@Composable
internal fun ProfileEditingSection(
    state: ProfileSettingsState.Editing,
    onFirstNameChange: (Name) -> Unit,
    onLastNameChange: (Name) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.profile_personal_info),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            ProfileField(
                label = stringResource(Res.string.profile_email),
                value = state.user.email.value
            )

            Spacer(Modifier.height(12.dp))

            PTextFieldName(
                fieldName = stringResource(Res.string.profile_first_name),
                value = state.editFirstName,
                icon = null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onFirstNameChange
            )

            Spacer(Modifier.height(12.dp))

            PTextFieldName(
                fieldName = stringResource(Res.string.profile_last_name),
                value = state.editLastName,
                icon = null,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                onAction = { if (state.canSave) onSave() },
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onLastNameChange
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                POutlinedButton(
                    text = stringResource(Res.string.profile_cancel),
                    onClick = onCancel
                )
                Spacer(Modifier.width(8.dp))
                PPrimaryButton(
                    text = stringResource(Res.string.profile_save),
                    enabled = state.canSave,
                    onClick = onSave
                )
            }
        }
    }
}

@Composable
internal fun ProfileSavingSection(
    state: ProfileSettingsState.Saving
) {
    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.profile_personal_info),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(16.dp))

            ProfileField(label = stringResource(Res.string.profile_email), value = state.user.email.value)
            Spacer(Modifier.height(12.dp))
            ProfileField(label = stringResource(Res.string.profile_first_name), value = state.editFirstName.value)
            Spacer(Modifier.height(12.dp))
            ProfileField(label = stringResource(Res.string.profile_last_name), value = state.editLastName.value)

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier.height(40.dp).width(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp))
                }
            }
        }
    }
}

// =============================================================================
// Helpers
// =============================================================================

@Composable
internal fun ProfileField(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun userDisplayName(user: User): String {
    return buildString {
        user.firstName?.value?.let { append(it) }
        if (user.firstName != null && user.lastName != null) append(" ")
        user.lastName?.value?.let { append(it) }
    }.ifEmpty { user.email.value }
}

private fun userInitials(user: User): String {
    val first = user.firstName?.value?.firstOrNull()
    val last = user.lastName?.value?.firstOrNull()
    return when {
        first != null && last != null -> "$first$last"
        first != null -> "${first}${user.firstName?.value?.getOrNull(1) ?: ""}"
        last != null -> "${last}${user.lastName?.value?.getOrNull(1) ?: ""}"
        else -> user.email.value.take(2)
    }.uppercase()
}

@OptIn(ExperimentalUuidApi::class)
private val previewUser = User(
    id = UserId(Uuid.parse("00000000-0000-0000-0000-000000000001")),
    email = Email("john@dokus.tech"),
    firstName = Name("John"),
    lastName = Name("Doe"),
    emailVerified = true,
    isActive = true,
    createdAt = LocalDateTime(2025, 1, 1, 0, 0),
    updatedAt = LocalDateTime(2025, 1, 1, 0, 0),
)

@Preview
@Composable
private fun ProfileHeroPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ProfileHero(
            user = previewUser,
            avatarState = ProfileSettingsState.AvatarState.Idle,
            onUploadAvatar = {},
            onResetAvatarState = {},
        )
    }
}

@Preview
@Composable
private fun AccountCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        AccountCard(
            user = previewUser,
            isResendingVerification = false,
            onResendVerification = {},
            onEditClick = {},
        )
    }
}

@Preview
@Composable
private fun SecurityCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SecurityCard(
            onChangePassword = {},
            onMySessions = {},
        )
    }
}

@Preview
@Composable
private fun ServerCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ServerCard(
            currentServer = ServerConfig.Cloud,
            onChangeServer = {},
            onResetToCloud = {},
        )
    }
}

@Preview
@Composable
private fun DangerZoneCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DangerZoneCard()
    }
}

@Preview
@Composable
private fun LogOutCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        LogOutCard(isLoggingOut = false, onLogout = {})
    }
}

@Preview
@Composable
private fun VersionFooterPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        VersionFooter()
    }
}

