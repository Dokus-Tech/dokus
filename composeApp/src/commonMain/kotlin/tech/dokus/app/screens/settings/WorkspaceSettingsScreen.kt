package tech.dokus.app.screens.settings

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.save_changes
import ai.dokus.app.resources.generated.workspace_address
import ai.dokus.app.resources.generated.workspace_banking
import ai.dokus.app.resources.generated.workspace_bic
import ai.dokus.app.resources.generated.workspace_company_info
import ai.dokus.app.resources.generated.workspace_company_name
import ai.dokus.app.resources.generated.workspace_iban
import ai.dokus.app.resources.generated.workspace_invoice_prefix
import ai.dokus.app.resources.generated.workspace_invoice_settings
import ai.dokus.app.resources.generated.workspace_legal_name
import ai.dokus.app.resources.generated.workspace_payment_terms
import ai.dokus.app.resources.generated.workspace_settings_title
import ai.dokus.app.resources.generated.workspace_vat_number
import ai.dokus.foundation.design.components.AvatarSize
import ai.dokus.foundation.design.components.CompanyAvatarImage
import ai.dokus.foundation.design.components.ImageCropperDialog
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import ai.dokus.foundation.domain.model.CompanyAvatar
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tech.dokus.app.viewmodel.AvatarState
import tech.dokus.app.viewmodel.SaveState
import tech.dokus.app.viewmodel.WorkspaceSettingsViewModel
import tech.dokus.foundation.app.picker.FilePickerLauncher
import tech.dokus.foundation.app.picker.rememberImagePicker
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess

/**
 * Workspace/Company settings screen with top bar.
 * For mobile navigation flow.
 */
@Composable
fun WorkspaceSettingsScreen(
    viewModel: WorkspaceSettingsViewModel = koinViewModel()
) {
    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.workspace_settings_title)
            )
        }
    ) { contentPadding ->
        WorkspaceSettingsContent(
            viewModel = viewModel,
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/**
 * Workspace settings content without scaffold.
 * Can be embedded in split-pane layout for desktop or used in full-screen for mobile.
 */
@Composable
fun WorkspaceSettingsContent(
    viewModel: WorkspaceSettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val state by viewModel.state.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val avatarState by viewModel.avatarState.collectAsState()
    val currentAvatar by viewModel.currentAvatar.collectAsState()

    // Image picker
    val avatarPicker = rememberImagePicker { pickedImage ->
        viewModel.onImageSelected(pickedImage.bytes)
    }

    LaunchedEffect(viewModel) {
        viewModel.loadWorkspaceSettings()
    }

    // Image cropper dialog
    if (avatarState is AvatarState.Cropping) {
        ImageCropperDialog(
            imageData = (avatarState as AvatarState.Cropping).imageBytes,
            onCropComplete = { croppedBytes ->
                viewModel.onCropComplete(croppedBytes)
            },
            onDismiss = { viewModel.cancelCrop() }
        )
    }

    when {
        state.isLoading() -> {
            Box(
                modifier = modifier.fillMaxSize().padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        state.isSuccess() -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(contentPadding)
                    .withContentPaddingForScrollable(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Company Information Section
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.workspace_company_info),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(16.dp))

                        // Company Avatar Section
                        CompanyAvatarSection(
                            avatarState = avatarState,
                            currentAvatar = currentAvatar,
                            companyInitial = formState.companyName.take(1).ifBlank { "C" },
                            avatarPicker = avatarPicker,
                            onDeleteAvatar = { viewModel.deleteAvatar() },
                            onResetAvatarState = { viewModel.resetAvatarState() }
                        )

                        Spacer(Modifier.height(16.dp))

                        // Legal Name (read-only)
                        Text(
                            text = stringResource(Res.string.workspace_legal_name),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formState.legalName,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(12.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_company_name),
                            value = formState.companyName,
                            onValueChange = { viewModel.updateCompanyName(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_vat_number),
                            value = formState.vatNumber,
                            onValueChange = { viewModel.updateVatNumber(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_address),
                            value = formState.address,
                            onValueChange = { viewModel.updateAddress(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Banking Details Section
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.workspace_banking),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(16.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_iban),
                            value = formState.iban,
                            onValueChange = { viewModel.updateIban(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_bic),
                            value = formState.bic,
                            onValueChange = { viewModel.updateBic(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Invoice Settings Section
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.workspace_invoice_settings),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(16.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_invoice_prefix),
                            value = formState.invoicePrefix,
                            onValueChange = { viewModel.updateInvoicePrefix(it) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_payment_terms),
                            value = formState.defaultPaymentTerms.toString(),
                            onValueChange = { viewModel.updateDefaultPaymentTerms(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Save Button
                PPrimaryButton(
                    text = stringResource(Res.string.save_changes),
                    enabled = saveState !is SaveState.Saving,
                    onClick = { viewModel.saveWorkspaceSettings() },
                    modifier = Modifier.fillMaxWidth()
                )

                // Save State Feedback
                SaveStateFeedback(
                    saveState = saveState,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Extracted avatar section component.
 * Takes avatarState as parameter to ensure safe type casting via smart casting.
 */
@Composable
private fun CompanyAvatarSection(
    avatarState: AvatarState,
    currentAvatar: CompanyAvatar?,
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

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Company Logo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            // Avatar action buttons
            val isActionInProgress = avatarState is AvatarState.Uploading || avatarState is AvatarState.Deleting
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { avatarPicker.launch() },
                    enabled = !isActionInProgress
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (currentAvatar != null) "Change" else "Upload")
                }

                if (currentAvatar != null) {
                    TextButton(
                        onClick = onDeleteAvatar,
                        enabled = !isActionInProgress
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            }

            // Avatar upload/delete progress indicator - uses smart casting
            AvatarStateIndicator(
                avatarState = avatarState,
                onResetState = onResetAvatarState
            )
        }
    }
}

/**
 * Avatar state indicator component.
 * Uses smart casting by receiving avatarState as a parameter.
 */
@Composable
private fun AvatarStateIndicator(
    avatarState: AvatarState,
    onResetState: () -> Unit
) {
    when (avatarState) {
        is AvatarState.Uploading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    progress = { avatarState.progress }, // Smart cast works here
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Uploading...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        is AvatarState.Deleting -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Removing...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        is AvatarState.Error -> {
            Text(
                text = avatarState.message, // Smart cast works here
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        is AvatarState.Success -> {
            LaunchedEffect(Unit) {
                onResetState()
            }
        }
        else -> {}
    }
}

/**
 * Save state feedback component.
 * Uses smart casting by receiving saveState as a parameter.
 */
@Composable
private fun SaveStateFeedback(
    saveState: SaveState,
    modifier: Modifier = Modifier
) {
    when (saveState) {
        is SaveState.Success -> {
            Text(
                text = "Settings saved successfully",
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
        is SaveState.Error -> {
            Text(
                text = saveState.message, // Smart cast works here
                color = MaterialTheme.colorScheme.error,
                modifier = modifier
            )
        }
        else -> {}
    }
}
