package tech.dokus.app.screens.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_change
import tech.dokus.aura.resources.action_edit
import tech.dokus.aura.resources.action_save
import tech.dokus.aura.resources.action_upload
import tech.dokus.aura.resources.state_removing
import tech.dokus.aura.resources.state_uploading
import tech.dokus.aura.resources.workspace_address
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
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val HeroAvatarSize = Constraints.AvatarSize.large // 128dp

/**
 * Non-collapsable company hero section with centered avatar.
 * Modeled after ProfileHero, with header-row Edit/Save/Cancel pattern.
 */
@Composable
internal fun CompanyHeroSection(
    formState: WorkspaceSettingsState.FormState,
    isLocked: Boolean,
    editMode: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    avatarState: WorkspaceSettingsState.AvatarState,
    currentAvatar: Thumbnail?,
    avatarPicker: FilePickerLauncher,
    modifier: Modifier = Modifier,
) {
    val imageLoader = rememberAuthenticatedImageLoader()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Constraints.Spacing.small),
    ) {
        // Header row with Edit/Save/Cancel
        HeroActionRow(
            editMode = editMode,
            isLocked = isLocked,
            onEdit = onEdit,
            onSave = onSave,
            onCancel = onCancel,
        )

        if (editMode) {
            CompanyHeroEditMode(
                formState = formState,
                isLocked = isLocked,
                onIntent = onIntent,
                avatarState = avatarState,
                currentAvatar = currentAvatar,
                avatarPicker = avatarPicker,
                imageLoader = imageLoader,
            )
        } else {
            CompanyHeroViewMode(
                formState = formState,
                isLocked = isLocked,
                avatarState = avatarState,
                currentAvatar = currentAvatar,
                avatarPicker = avatarPicker,
                imageLoader = imageLoader,
                onResetAvatarState = { onIntent(WorkspaceSettingsIntent.ResetAvatarState) },
            )
        }
    }
}

@Composable
private fun HeroActionRow(
    editMode: Boolean,
    isLocked: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Constraints.Spacing.xSmall),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            editMode -> {
                TextButton(onClick = onCancel) {
                    Text(
                        text = stringResource(Res.string.action_cancel),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                TextButton(onClick = onSave) {
                    Text(
                        text = stringResource(Res.string.action_save),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            !isLocked -> {
                TextButton(onClick = onEdit) {
                    Text(
                        text = stringResource(Res.string.action_edit),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompanyHeroViewMode(
    formState: WorkspaceSettingsState.FormState,
    isLocked: Boolean,
    avatarState: WorkspaceSettingsState.AvatarState,
    currentAvatar: Thumbnail?,
    avatarPicker: FilePickerLauncher,
    imageLoader: ImageLoader,
    onResetAvatarState: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CenteredAvatar(
            avatarState = avatarState,
            currentAvatar = currentAvatar,
            companyInitial = formState.companyName.take(1).ifBlank { "C" },
            onUploadAvatar = { avatarPicker.launch() },
            imageLoader = imageLoader,
        )

        Spacer(Modifier.height(Constraints.Spacing.medium))

        // Company name
        Text(
            text = formState.companyName.ifBlank { formState.legalName },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        // Legal name (only if different from company name)
        if (formState.legalName.isNotBlank() && formState.legalName != formState.companyName) {
            Spacer(Modifier.height(Constraints.Spacing.xxSmall))
            Text(
                text = formState.legalName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }

        // VAT number
        if (formState.vatNumber.isNotBlank()) {
            Spacer(Modifier.height(Constraints.Spacing.xxSmall))
            Text(
                text = formState.vatNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }

        // Address
        if (formState.address.isNotBlank()) {
            Spacer(Modifier.height(Constraints.Spacing.xxSmall))
            Text(
                text = formState.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }

        AvatarStateIndicator(
            avatarState = avatarState,
            onResetState = onResetAvatarState,
        )

        Spacer(Modifier.height(Constraints.Spacing.medium))
    }
}

@Composable
private fun CompanyHeroEditMode(
    formState: WorkspaceSettingsState.FormState,
    isLocked: Boolean,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    avatarState: WorkspaceSettingsState.AvatarState,
    currentAvatar: Thumbnail?,
    avatarPicker: FilePickerLauncher,
    imageLoader: ImageLoader,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CenteredAvatar(
            avatarState = avatarState,
            currentAvatar = currentAvatar,
            companyInitial = formState.companyName.take(1).ifBlank { "C" },
            onUploadAvatar = { avatarPicker.launch() },
            imageLoader = imageLoader,
        )

        Spacer(Modifier.height(Constraints.Spacing.medium))
    }

    // Form fields (left-aligned)
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
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(Modifier.height(Constraints.Spacing.small))

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
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(Modifier.height(Constraints.Spacing.small))

    PTextFieldStandard(
        fieldName = stringResource(Res.string.workspace_address),
        value = formState.address,
        onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateAddress(it)) },
        modifier = Modifier.fillMaxWidth(),
    )

    AvatarStateIndicator(
        avatarState = avatarState,
        onResetState = { onIntent(WorkspaceSettingsIntent.ResetAvatarState) },
    )
}

@Composable
private fun CenteredAvatar(
    avatarState: WorkspaceSettingsState.AvatarState,
    currentAvatar: Thumbnail?,
    companyInitial: String,
    onUploadAvatar: () -> Unit,
    imageLoader: ImageLoader,
) {
    val isAvatarBusy =
        avatarState is WorkspaceSettingsState.AvatarState.Uploading ||
            avatarState is WorkspaceSettingsState.AvatarState.Deleting
    val uploadProgress = (avatarState as? WorkspaceSettingsState.AvatarState.Uploading)?.progress
    val editDescription = stringResource(
        if (currentAvatar != null) Res.string.action_change else Res.string.action_upload
    )

    EditableAvatarSurface(
        onEditClick = onUploadAvatar,
        editContentDescription = editDescription,
        modifier = Modifier.size(HeroAvatarSize),
        shape = MaterialTheme.shapes.medium,
        enabled = !isAvatarBusy,
        isBusy = isAvatarBusy,
        progress = uploadProgress,
    ) {
        CompanyAvatarImage(
            avatarUrl = rememberResolvedApiUrl(currentAvatar?.medium),
            initial = companyInitial,
            size = AvatarSize.Large,
            sizeOverride = HeroAvatarSize,
            imageLoader = imageLoader,
            onClick = null,
        )
    }
}

@Composable
private fun AvatarStateIndicator(
    avatarState: WorkspaceSettingsState.AvatarState,
    onResetState: () -> Unit,
) {
    when (avatarState) {
        is WorkspaceSettingsState.AvatarState.Uploading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                modifier = Modifier.padding(top = Constraints.Spacing.xSmall),
            ) {
                CircularProgressIndicator(
                    progress = { avatarState.progress },
                    modifier = Modifier.size(Constraints.IconSize.xSmall),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(Res.string.state_uploading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        is WorkspaceSettingsState.AvatarState.Deleting -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                modifier = Modifier.padding(top = Constraints.Spacing.xSmall),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Constraints.IconSize.xSmall),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = stringResource(Res.string.state_removing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        is WorkspaceSettingsState.AvatarState.Error -> {
            Text(
                text = avatarState.error.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Constraints.Spacing.xSmall),
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

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun CompanyHeroSectionViewPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompanyHeroViewMode(
            formState = WorkspaceSettingsState.FormState(
                companyName = "INVOID VISION",
                legalName = "INVOID VISION",
                vatNumber = "BE0777887045",
                address = "Balegemstraat 17, Box 7, 9860 Oosterzele, BE",
            ),
            isLocked = false,
            avatarState = WorkspaceSettingsState.AvatarState.Idle,
            currentAvatar = null,
            avatarPicker = rememberNoOpPicker(),
            imageLoader = rememberAuthenticatedImageLoader(),
            onResetAvatarState = {},
        )
    }
}

@Preview
@Composable
private fun CompanyHeroSectionEditPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        CompanyHeroEditMode(
            formState = WorkspaceSettingsState.FormState(
                companyName = "INVOID VISION",
                legalName = "INVOID VISION",
                vatNumber = "BE0777887045",
                address = "Balegemstraat 17, Box 7, 9860 Oosterzele, BE",
            ),
            isLocked = true,
            onIntent = {},
            avatarState = WorkspaceSettingsState.AvatarState.Idle,
            currentAvatar = null,
            avatarPicker = rememberNoOpPicker(),
            imageLoader = rememberAuthenticatedImageLoader(),
        )
    }
}

@Composable
private fun rememberNoOpPicker(): FilePickerLauncher {
    return tech.dokus.foundation.app.picker.rememberImagePicker { }
}
