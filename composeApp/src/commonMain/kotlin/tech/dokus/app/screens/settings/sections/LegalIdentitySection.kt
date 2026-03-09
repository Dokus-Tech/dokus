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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_change
import tech.dokus.aura.resources.action_upload
import tech.dokus.aura.resources.state_removing
import tech.dokus.aura.resources.state_uploading
import tech.dokus.aura.resources.workspace_address
import tech.dokus.aura.resources.workspace_company_logo
import tech.dokus.aura.resources.workspace_company_name
import tech.dokus.aura.resources.workspace_legal_name
import tech.dokus.aura.resources.workspace_vat_number
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.app.network.rememberResolvedApiUrl
import tech.dokus.foundation.app.picker.FilePickerLauncher
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.EditableAvatarSurface
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.settings.DataRow
import tech.dokus.foundation.aura.components.settings.DataRowStatus
import tech.dokus.foundation.aura.components.settings.SettingsSection
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val EditableCompanyAvatarSize = 144.dp

@Composable
internal fun LegalIdentitySection(
    formState: WorkspaceSettingsState.FormState,
    isLocked: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    editMode: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    avatarState: WorkspaceSettingsState.AvatarState,
    currentAvatar: Thumbnail?,
    avatarPicker: FilePickerLauncher,
) {
    val imageLoader = rememberAuthenticatedImageLoader()
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

            Spacer(Modifier.height(Constraints.Spacing.small))

            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_company_name),
                value = formState.companyName,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateCompanyName(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constraints.Spacing.small))

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

            Spacer(Modifier.height(Constraints.Spacing.small))

            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_address),
                value = formState.address,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateAddress(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constraints.Spacing.medium))

            CompanyAvatarSection(
                avatarState = avatarState,
                currentAvatar = currentAvatar,
                companyInitial = formState.companyName.take(1).ifBlank { "C" },
                onUploadAvatar = { avatarPicker.launch() },
                onResetAvatarState = { onIntent(WorkspaceSettingsIntent.ResetAvatarState) },
                imageLoader = imageLoader
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
            CompanyAvatarSection(
                avatarState = avatarState,
                currentAvatar = currentAvatar,
                companyInitial = formState.companyName.take(1).ifBlank { "C" },
                onUploadAvatar = { avatarPicker.launch() },
                onResetAvatarState = { onIntent(WorkspaceSettingsIntent.ResetAvatarState) },
                imageLoader = imageLoader
            )
        }
    }
}

/**
 * Extracted avatar section component.
 */
@Composable
private fun CompanyAvatarSection(
    avatarState: WorkspaceSettingsState.AvatarState,
    currentAvatar: Thumbnail?,
    companyInitial: String,
    onUploadAvatar: () -> Unit,
    onResetAvatarState: () -> Unit,
    imageLoader: ImageLoader
) {
    val isAvatarBusy =
        avatarState is WorkspaceSettingsState.AvatarState.Uploading ||
            avatarState is WorkspaceSettingsState.AvatarState.Deleting
    val uploadProgress = (avatarState as? WorkspaceSettingsState.AvatarState.Uploading)?.progress
    val editDescription = stringResource(
        if (currentAvatar != null) Res.string.action_change else Res.string.action_upload
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Constraints.Spacing.small),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = stringResource(Res.string.workspace_company_logo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.textMuted,
            modifier = Modifier.width(140.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            EditableAvatarSurface(
                onEditClick = onUploadAvatar,
                editContentDescription = editDescription,
                modifier = Modifier.size(EditableCompanyAvatarSize),
                shape = MaterialTheme.shapes.medium,
                enabled = !isAvatarBusy,
                isBusy = isAvatarBusy,
                progress = uploadProgress,
            ) {
                CompanyAvatarImage(
                    avatarUrl = rememberResolvedApiUrl(currentAvatar?.medium),
                    initial = companyInitial,
                    size = AvatarSize.Large,
                    sizeOverride = EditableCompanyAvatarSize,
                    imageLoader = imageLoader,
                    onClick = null,
                )
            }
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
    avatarState: WorkspaceSettingsState.AvatarState,
    onResetState: () -> Unit
) {
    when (avatarState) {
        is WorkspaceSettingsState.AvatarState.Uploading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                modifier = Modifier.padding(top = Constraints.Spacing.xSmall)
            ) {
                CircularProgressIndicator(
                    progress = { avatarState.progress },
                    modifier = Modifier.size(Constraints.IconSize.xSmall),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(Res.string.state_uploading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        is WorkspaceSettingsState.AvatarState.Deleting -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                modifier = Modifier.padding(top = Constraints.Spacing.xSmall)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Constraints.IconSize.xSmall),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(Res.string.state_removing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        is WorkspaceSettingsState.AvatarState.Error -> {
            Text(
                text = avatarState.error.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Constraints.Spacing.xSmall)
            )
        }

        is WorkspaceSettingsState.AvatarState.Success -> {
            LaunchedEffect(Unit) {
                onResetState()
            }
        }

        else -> {}
    }
}

@Preview
@Composable
private fun CompanyAvatarSectionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CompanyAvatarSection(
            avatarState = WorkspaceSettingsState.AvatarState.Idle,
            currentAvatar = null,
            companyInitial = "D",
            onUploadAvatar = {},
            onResetAvatarState = {},
            imageLoader = rememberAuthenticatedImageLoader(),
        )
    }
}
