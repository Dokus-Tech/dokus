package tech.dokus.app.screens.settings

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.action_change
import ai.dokus.app.resources.generated.action_remove
import ai.dokus.app.resources.generated.action_upload
import ai.dokus.app.resources.generated.save_changes
import ai.dokus.app.resources.generated.settings_saved_successfully
import ai.dokus.app.resources.generated.state_removing
import ai.dokus.app.resources.generated.state_uploading
import ai.dokus.app.resources.generated.workspace_address
import ai.dokus.app.resources.generated.workspace_banking
import ai.dokus.app.resources.generated.workspace_bic
import ai.dokus.app.resources.generated.workspace_company_info
import ai.dokus.app.resources.generated.workspace_company_logo
import ai.dokus.app.resources.generated.workspace_company_name
import ai.dokus.app.resources.generated.workspace_iban
import ai.dokus.app.resources.generated.workspace_invoice_include_year
import ai.dokus.app.resources.generated.workspace_invoice_numbering
import ai.dokus.app.resources.generated.workspace_invoice_numbering_description
import ai.dokus.app.resources.generated.workspace_invoice_padding
import ai.dokus.app.resources.generated.workspace_invoice_prefix
import ai.dokus.app.resources.generated.workspace_invoice_prefix_description
import ai.dokus.app.resources.generated.workspace_invoice_preview
import ai.dokus.app.resources.generated.workspace_invoice_settings
import ai.dokus.app.resources.generated.workspace_invoice_settings_description
import ai.dokus.app.resources.generated.workspace_invoice_yearly_reset
import ai.dokus.app.resources.generated.workspace_legal_name
import ai.dokus.app.resources.generated.workspace_payment_days_description
import ai.dokus.app.resources.generated.workspace_payment_terms
import ai.dokus.app.resources.generated.workspace_payment_terms_description
import ai.dokus.app.resources.generated.workspace_payment_terms_section
import ai.dokus.app.resources.generated.workspace_payment_terms_text
import ai.dokus.app.resources.generated.workspace_settings_title
import ai.dokus.app.resources.generated.workspace_vat_number
import ai.dokus.foundation.design.components.AvatarSize
import ai.dokus.foundation.design.components.CompanyAvatarImage
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.common.PTopAppBar
import ai.dokus.foundation.design.components.fields.PTextFieldFree
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.withContentPaddingForScrollable
import tech.dokus.domain.model.common.Thumbnail
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.viewmodel.WorkspaceSettingsAction
import tech.dokus.app.viewmodel.WorkspaceSettingsContainer
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.picker.FilePickerLauncher
import tech.dokus.foundation.app.picker.rememberImagePicker

/**
 * Workspace/Company settings screen with top bar using FlowMVI Container pattern.
 * For mobile navigation flow.
 */
@Composable
internal fun WorkspaceSettingsScreen(
    container: WorkspaceSettingsContainer = container()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is WorkspaceSettingsAction.ShowSuccess -> {
                snackbarHostState.showSnackbar(action.message)
            }

            is WorkspaceSettingsAction.ShowError -> {
                snackbarHostState.showSnackbar(action.message)
            }
        }
    }

    // Load settings on first composition
    LaunchedEffect(Unit) {
        container.store.intent(WorkspaceSettingsIntent.Load)
    }

    Scaffold(
        topBar = {
            PTopAppBar(
                title = stringResource(Res.string.workspace_settings_title)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        WorkspaceSettingsContentInternal(
            state = state,
            onIntent = { container.store.intent(it) },
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/**
 * Workspace settings content without scaffold using FlowMVI Container pattern.
 * Can be embedded in split-pane layout for desktop or used in full-screen for mobile.
 */
@Composable
internal fun WorkspaceSettingsContent(
    container: WorkspaceSettingsContainer = container(),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is WorkspaceSettingsAction.ShowSuccess -> {
                snackbarHostState.showSnackbar(action.message)
            }

            is WorkspaceSettingsAction.ShowError -> {
                snackbarHostState.showSnackbar(action.message)
            }
        }
    }

    // Load settings on first composition
    LaunchedEffect(Unit) {
        container.store.intent(WorkspaceSettingsIntent.Load)
    }

    WorkspaceSettingsContentInternal(
        state = state,
        onIntent = { container.store.intent(it) },
        modifier = modifier.padding(contentPadding)
    )
}

@Composable
private fun WorkspaceSettingsContentInternal(
    state: WorkspaceSettingsState,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // Image picker - uploads directly without cropping
    val avatarPicker = rememberImagePicker { pickedImage ->
        onIntent(WorkspaceSettingsIntent.UploadAvatar(pickedImage.bytes, pickedImage.name))
    }

    when (state) {
        is WorkspaceSettingsState.Loading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is WorkspaceSettingsState.Content -> {
            val formState = state.form
            val saveState = state.saveState
            val avatarState = state.avatarState
            val currentAvatar = state.currentAvatar

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .withContentPaddingForScrollable(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Company Information Section
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(Res.string.workspace_company_info),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(12.dp))

                        // Company Avatar Section
                        CompanyAvatarSection(
                            avatarState = avatarState,
                            currentAvatar = currentAvatar,
                            companyInitial = formState.companyName.take(1).ifBlank { "C" },
                            avatarPicker = avatarPicker,
                            onDeleteAvatar = { onIntent(WorkspaceSettingsIntent.DeleteAvatar) },
                            onResetAvatarState = { onIntent(WorkspaceSettingsIntent.ResetAvatarState) }
                        )

                        Spacer(Modifier.height(12.dp))

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

                        Spacer(Modifier.height(8.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_company_name),
                            value = formState.companyName,
                            onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateCompanyName(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_vat_number),
                            value = formState.vatNumber,
                            onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateVatNumber(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_address),
                            value = formState.address,
                            onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateAddress(it)) },
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

                        Spacer(Modifier.height(12.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_iban),
                            value = formState.iban,
                            onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateIban(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_bic),
                            value = formState.bic,
                            onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateBic(it)) },
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
                        Text(
                            text = stringResource(Res.string.workspace_invoice_settings_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))

                        // Invoice Prefix
                        Text(
                            text = stringResource(Res.string.workspace_invoice_prefix_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_invoice_prefix),
                            value = formState.invoicePrefix,
                            onValueChange = {
                                onIntent(
                                    WorkspaceSettingsIntent.UpdateInvoicePrefix(
                                        it
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        // Payment Terms (Days)
                        Text(
                            text = stringResource(Res.string.workspace_payment_days_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.workspace_payment_terms),
                            value = formState.defaultPaymentTerms.toString(),
                            onValueChange = {
                                onIntent(
                                    WorkspaceSettingsIntent.UpdateDefaultPaymentTerms(
                                        it
                                    )
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Divider
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Invoice Numbering Subsection
                        Text(
                            text = stringResource(Res.string.workspace_invoice_numbering),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(Res.string.workspace_invoice_numbering_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        // Include Year in Number
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = formState.invoiceIncludeYear,
                                onCheckedChange = {
                                    onIntent(
                                        WorkspaceSettingsIntent.UpdateInvoiceIncludeYear(
                                            it
                                        )
                                    )
                                }
                            )
                            Text(
                                text = stringResource(Res.string.workspace_invoice_include_year),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Reset Numbering Each Year
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = formState.invoiceYearlyReset,
                                onCheckedChange = {
                                    onIntent(
                                        WorkspaceSettingsIntent.UpdateInvoiceYearlyReset(
                                            it
                                        )
                                    )
                                }
                            )
                            Text(
                                text = stringResource(Res.string.workspace_invoice_yearly_reset),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // Number Padding Selector
                        Text(
                            text = stringResource(Res.string.workspace_invoice_padding),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(3, 4, 5, 6).forEach { padding ->
                                TextButton(
                                    onClick = {
                                        onIntent(
                                            WorkspaceSettingsIntent.UpdateInvoicePadding(
                                                padding
                                            )
                                        )
                                    }
                                ) {
                                    Text(
                                        text = padding.toString(),
                                        color = if (formState.invoicePadding == padding)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Preview in highlighted card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(Res.string.workspace_invoice_preview),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = generateInvoiceNumberPreview(
                                        formState.invoicePrefix,
                                        formState.invoiceIncludeYear,
                                        formState.invoicePadding
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Divider
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        // Payment Terms Subsection
                        Text(
                            text = stringResource(Res.string.workspace_payment_terms_section),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(Res.string.workspace_payment_terms_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        PTextFieldFree(
                            fieldName = stringResource(Res.string.workspace_payment_terms_text),
                            value = formState.paymentTermsText,
                            onValueChange = {
                                onIntent(
                                    WorkspaceSettingsIntent.UpdatePaymentTermsText(
                                        it
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Save Button
                PPrimaryButton(
                    text = stringResource(Res.string.save_changes),
                    enabled = saveState !is WorkspaceSettingsState.Content.SaveState.Saving,
                    onClick = { onIntent(WorkspaceSettingsIntent.SaveSettings) },
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

        is WorkspaceSettingsState.Error -> {
            // Error state - show retry option
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(Res.string.workspace_settings_load_failed),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    PPrimaryButton(
                        text = stringResource(Res.string.state_retry),
                        onClick = { onIntent(WorkspaceSettingsIntent.Load) }
                    )
                }
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

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.workspace_company_logo),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            // Avatar action buttons
            val isActionInProgress =
                avatarState is WorkspaceSettingsState.Content.AvatarState.Uploading ||
                        avatarState is WorkspaceSettingsState.Content.AvatarState.Deleting
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
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(Res.string.action_remove))
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
    avatarState: WorkspaceSettingsState.Content.AvatarState,
    onResetState: () -> Unit
) {
    when (avatarState) {
        is WorkspaceSettingsState.Content.AvatarState.Uploading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    progress = { avatarState.progress },
                    modifier = Modifier.size(16.dp),
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
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
                text = avatarState.message,
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

/**
 * Save state feedback component.
 * Uses smart casting by receiving saveState as a parameter.
 */
@Composable
private fun SaveStateFeedback(
    saveState: WorkspaceSettingsState.Content.SaveState,
    modifier: Modifier = Modifier
) {
    when (saveState) {
        is WorkspaceSettingsState.Content.SaveState.Success -> {
            Text(
                text = stringResource(Res.string.settings_saved_successfully),
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }

        is WorkspaceSettingsState.Content.SaveState.Error -> {
            Text(
                text = saveState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier
            )
        }

        else -> {}
    }
}

/**
 * Generate a preview of the invoice number format based on current settings.
 */
private fun generateInvoiceNumberPreview(
    prefix: String,
    includeYear: Boolean,
    padding: Int
): String {
    val effectivePrefix = prefix.ifBlank { "INV" }
    val paddedNumber = "1".padStart(padding, '0')
    return if (includeYear) {
        "$effectivePrefix-2025-$paddedNumber"
    } else {
        "$effectivePrefix-$paddedNumber"
    }
}
