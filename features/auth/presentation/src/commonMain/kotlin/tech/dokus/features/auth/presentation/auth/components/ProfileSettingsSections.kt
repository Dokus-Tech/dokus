package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import tech.dokus.aura.resources.profile_sessions_device_count
import tech.dokus.aura.resources.profile_version_footer
import tech.dokus.aura.resources.profile_resend_verification
import tech.dokus.aura.resources.profile_save
import tech.dokus.aura.resources.profile_sessions
import tech.dokus.domain.Name
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.model.User
import tech.dokus.features.auth.mvi.ProfileSettingsState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.MonogramAvatar
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.badges.TierBadge
import tech.dokus.foundation.aura.components.fields.PTextFieldName
import tech.dokus.foundation.aura.components.settings.SettingsRow
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.components.text.DokusLabel
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

// =============================================================================
// v2 Profile Sections
// =============================================================================

/**
 * Profile hero: centered avatar, name, email, tier badges.
 */
@Composable
internal fun ProfileHero(
    user: User,
    modifier: Modifier = Modifier,
) {
    val name = userDisplayName(user)
    val initials = userInitials(user)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth().padding(bottom = 14.dp),
    ) {
        MonogramAvatar(initials = initials, size = 72.dp, radius = 22.dp)
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
                    onClick = onResendVerification,
                )
            }
            SettingsRow(
                label = stringResource(Res.string.profile_edit),
                chevron = true,
                showDivider = false,
                onClick = onEditClick,
            )
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
                value = stringResource(Res.string.profile_sessions_device_count),
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
            SettingsRow(label = stringResource(Res.string.profile_server_url), value = currentServer.baseUrl, mono = true)
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

@Composable
internal fun ProfileErrorSection() {
    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(Res.string.profile_load_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
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

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
private val previewUser = User(
    id = tech.dokus.domain.ids.UserId(kotlin.uuid.Uuid.parse("00000000-0000-0000-0000-000000000001")),
    email = tech.dokus.domain.Email("john@dokus.tech"),
    firstName = Name("John"),
    lastName = Name("Doe"),
    emailVerified = true,
    isActive = true,
    createdAt = kotlinx.datetime.LocalDateTime(2025, 1, 1, 0, 0),
    updatedAt = kotlinx.datetime.LocalDateTime(2025, 1, 1, 0, 0),
)

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ProfileHeroPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ProfileHero(user = previewUser)
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun AccountCardPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        AccountCard(
            user = previewUser,
            isResendingVerification = false,
            onResendVerification = {},
            onEditClick = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun SecurityCardPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        SecurityCard(
            onChangePassword = {},
            onMySessions = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ServerCardPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ServerCard(
            currentServer = ServerConfig.Cloud,
            onChangeServer = {},
            onResetToCloud = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun DangerZoneCardPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        DangerZoneCard()
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun LogOutCardPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        LogOutCard(isLoggingOut = false, onLogout = {})
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun VersionFooterPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        VersionFooter()
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ProfileErrorSectionPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ProfileErrorSection()
    }
}
