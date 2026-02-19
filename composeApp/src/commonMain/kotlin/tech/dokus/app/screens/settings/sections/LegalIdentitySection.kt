package tech.dokus.app.screens.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_change
import tech.dokus.aura.resources.action_remove
import tech.dokus.aura.resources.action_upload
import tech.dokus.aura.resources.state_removing
import tech.dokus.aura.resources.state_uploading
import tech.dokus.aura.resources.workspace_address
import tech.dokus.aura.resources.workspace_company_logo
import tech.dokus.aura.resources.workspace_company_name
import tech.dokus.aura.resources.workspace_legal_name
import tech.dokus.aura.resources.workspace_vat_number
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.foundation.app.picker.FilePickerLauncher
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.settings.DataRow
import tech.dokus.foundation.aura.components.settings.DataRowStatus
import tech.dokus.foundation.aura.components.settings.SettingsSection
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun LegalIdentitySection(
    formState: WorkspaceSettingsState.Content.FormState,
    isLocked: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    editMode: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    avatarState: WorkspaceSettingsState.Content.AvatarState,
    currentAvatar: Thumbnail?,
    avatarPicker: FilePickerLauncher,
) {
    val subtitle = if (!expanded) formState.legalName else null

    SettingsSection(
        title = "Legal Identity",
        subtitle = subtitle,
        expanded = expanded,
        onToggle = onToggle,
        editMode = editMode,
        onEdit = if (!isLocked) onEdit else null,
        onSave = onSave,
        onCancel = onCancel,
    ) {
        if (editMode) {
            // Edit mode: show text fields

            // Legal Name is always read-only, show as DataRow even in edit mode
            DataRow(
                label = stringResource(Res.string.workspace_legal_name),
                value = formState.legalName,
                locked = true,
            )

            Spacer(Modifier.height(Constrains.Spacing.small))

            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_company_name),
                value = formState.companyName,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateCompanyName(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constrains.Spacing.small))

            // VAT Number: show as DataRow if locked, editable field otherwise
            if (isLocked) {
                DataRow(
                    label = stringResource(Res.string.workspace_vat_number),
                    value = formState.vatNumber,
                    locked = true,
                    status = DataRowStatus("Verified", StatusDotType.Confirmed),
                )
            } else {
                PTextFieldStandard(
                    fieldName = stringResource(Res.string.workspace_vat_number),
                    value = formState.vatNumber,
                    onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateVatNumber(it)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(Constrains.Spacing.small))

            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_address),
                value = formState.address,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateAddress(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constrains.Spacing.medium))

            // Avatar section
            CompanyAvatarSection(
                avatarState = avatarState,
                currentAvatar = currentAvatar,
                companyInitial = formState.companyName.take(1).ifBlank { "C" },
                avatarPicker = avatarPicker,
                onDeleteAvatar = { onIntent(WorkspaceSettingsIntent.DeleteAvatar) },
                onResetAvatarState = { onIntent(WorkspaceSettingsIntent.ResetAvatarState) }
            )
        } else {
            // View mode: show DataRows
            DataRow(
                label = stringResource(Res.string.workspace_legal_name),
                value = formState.legalName,
                locked = isLocked,
                status = if (isLocked) DataRowStatus("Verified", StatusDotType.Confirmed) else null,
            )

            DataRow(
                label = stringResource(Res.string.workspace_company_name),
                value = formState.companyName,
            )

            DataRow(
                label = stringResource(Res.string.workspace_vat_number),
                value = formState.vatNumber,
                locked = isLocked,
                status = if (isLocked) DataRowStatus("Verified", StatusDotType.Confirmed) else null,
            )

            DataRow(
                label = stringResource(Res.string.workspace_address),
                value = formState.address,
            )

            // Logo row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Constrains.Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.workspace_company_logo),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.width(140.dp),
                )
                CompanyAvatarImage(
                    avatarUrl = currentAvatar?.medium,
                    initial = formState.companyName.take(1).ifBlank { "C" },
                    size = AvatarSize.Small,
                    onClick = null,
                )
            }
        }
    }
}

/**
 * Extracted avatar section component.
 */
@Composable
private fun CompanyAvatarSection(
    avatarState: WorkspaceSettingsState.Content.AvatarState,
    currentAvatar: Thumbnail?,
    companyInitial: String,
    avatarPicker: FilePickerLauncher,
    onDeleteAvatar: () -> Unit,
    onResetAvatarState: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompanyAvatarImage(
            avatarUrl = currentAvatar?.medium,
            initial = companyInitial,
            size = AvatarSize.Large,
            onClick = { avatarPicker.launch() }
        )

        Spacer(Modifier.width(Constrains.Spacing.large))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.workspace_company_logo),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constrains.Spacing.xSmall))

            // Avatar action buttons
            val isActionInProgress =
                avatarState is WorkspaceSettingsState.Content.AvatarState.Uploading ||
                    avatarState is WorkspaceSettingsState.Content.AvatarState.Deleting
            Row(horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)) {
                TextButton(
                    onClick = { avatarPicker.launch() },
                    enabled = !isActionInProgress
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(Constrains.IconSize.small)
                    )
                    Spacer(Modifier.width(Constrains.Spacing.xSmall))
                    Text(
                        if (currentAvatar != null) {
                            stringResource(Res.string.action_change)
                        } else {
                            stringResource(Res.string.action_upload)
                        }
                    )
                }

                if (currentAvatar != null) {
                    TextButton(
                        onClick = onDeleteAvatar,
                        enabled = !isActionInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(Constrains.IconSize.small)
                        )
                        Spacer(Modifier.width(Constrains.Spacing.xSmall))
                        Text(stringResource(Res.string.action_remove))
                    }
                }
            }

            // Avatar upload/delete progress indicator
            AvatarStateIndicator(
                avatarState = avatarState,
                onResetState = onResetAvatarState
            )
        }
    }
}

/**
 * Avatar state indicator component.
 */
@Composable
private fun AvatarStateIndicator(
    avatarState: WorkspaceSettingsState.Content.AvatarState,
    onResetState: () -> Unit
) {
    when (avatarState) {
        is WorkspaceSettingsState.Content.AvatarState.Uploading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
            ) {
                CircularProgressIndicator(
                    progress = { avatarState.progress },
                    modifier = Modifier.size(Constrains.IconSize.xSmall),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(Res.string.state_uploading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        is WorkspaceSettingsState.Content.AvatarState.Deleting -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Constrains.IconSize.xSmall),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(Res.string.state_removing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        is WorkspaceSettingsState.Content.AvatarState.Error -> {
            Text(
                text = avatarState.error.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        is WorkspaceSettingsState.Content.AvatarState.Success -> {
            LaunchedEffect(Unit) {
                onResetState()
            }
        }

        else -> {}
    }
}
