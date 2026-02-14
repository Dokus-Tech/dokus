package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_empty_value
import tech.dokus.aura.resources.profile_cancel
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
import tech.dokus.aura.resources.profile_logging_out
import tech.dokus.aura.resources.profile_logout
import tech.dokus.aura.resources.profile_logout_description
import tech.dokus.aura.resources.profile_personal_info
import tech.dokus.aura.resources.profile_change_password
import tech.dokus.aura.resources.profile_resend_verification
import tech.dokus.aura.resources.profile_save
import tech.dokus.aura.resources.profile_sessions
import tech.dokus.domain.Name
import tech.dokus.features.auth.mvi.ProfileSettingsState
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldName

@Composable
internal fun ProfileViewingSection(
    state: ProfileSettingsState.Viewing,
    onEditClick: () -> Unit,
    onResendVerification: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onMySessionsClick: () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.profile_personal_info),
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onEditClick) {
                    Text(stringResource(Res.string.profile_edit))
                }
            }

            Spacer(Modifier.height(16.dp))

            ProfileField(
                label = stringResource(Res.string.profile_email),
                value = state.user.email.value
            )

            Spacer(Modifier.height(12.dp))

            ProfileField(
                label = stringResource(Res.string.profile_first_name),
                value = state.user.firstName?.value
                    ?: stringResource(Res.string.common_empty_value)
            )

            Spacer(Modifier.height(12.dp))

            ProfileField(
                label = stringResource(Res.string.profile_last_name),
                value = state.user.lastName?.value
                    ?: stringResource(Res.string.common_empty_value)
            )

            Spacer(Modifier.height(12.dp))

            ProfileField(
                label = stringResource(Res.string.profile_email_verification),
                value = if (state.user.emailVerified) {
                    stringResource(Res.string.profile_email_verified)
                } else {
                    stringResource(Res.string.profile_email_not_verified)
                }
            )

            Spacer(Modifier.height(16.dp))

            if (!state.user.emailVerified) {
                POutlinedButton(
                    text = stringResource(Res.string.profile_resend_verification),
                    enabled = !state.isResendingVerification,
                    onClick = onResendVerification,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
            }

            POutlinedButton(
                text = stringResource(Res.string.profile_change_password),
                onClick = onChangePasswordClick,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            POutlinedButton(
                text = stringResource(Res.string.profile_sessions),
                onClick = onMySessionsClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun ProfileEditingSection(
    state: ProfileSettingsState.Editing,
    onFirstNameChange: (Name) -> Unit,
    onLastNameChange: (Name) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
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
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
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

            ProfileField(
                label = stringResource(Res.string.profile_first_name),
                value = state.editFirstName.value
            )

            Spacer(Modifier.height(12.dp))

            ProfileField(
                label = stringResource(Res.string.profile_last_name),
                value = state.editLastName.value
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier.height(42.dp).width(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp).width(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProfileErrorSection() {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.profile_load_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
internal fun DangerZoneSection() {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
            Text(
                text = stringResource(Res.string.profile_danger_zone),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.profile_deactivate_warning),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            POutlinedButton(
                text = stringResource(Res.string.profile_deactivate_account),
                onClick = { /* TODO: Implement deactivation dialog */ },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun LogoutSection(
    isLoggingOut: Boolean,
    onLogout: () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column {
            Text(
                text = stringResource(Res.string.profile_logout),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.profile_logout_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            POutlinedButton(
                text = if (isLoggingOut) {
                    stringResource(Res.string.profile_logging_out)
                } else {
                    stringResource(Res.string.profile_logout)
                },
                enabled = !isLoggingOut,
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

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
