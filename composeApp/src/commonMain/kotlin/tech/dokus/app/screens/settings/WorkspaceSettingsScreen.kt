package tech.dokus.app.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.screens.settings.sections.BankingDetailsSection
import tech.dokus.app.screens.settings.sections.InvoiceFormatSection
import tech.dokus.app.screens.settings.sections.CompanyHeroSection
import tech.dokus.app.screens.settings.sections.PaymentTermsSection
import tech.dokus.app.screens.settings.sections.PeppolConnectionSection
import tech.dokus.app.screens.settings.sections.ProcessingHealthSection
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.settings_saved_successfully
import tech.dokus.aura.resources.workspace_settings_title
import tech.dokus.app.screens.settings.components.SettingsSkeleton
import tech.dokus.foundation.app.picker.FilePickerLauncher
import tech.dokus.foundation.app.picker.rememberImagePicker
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.common.DokusErrorBanner
import tech.dokus.foundation.aura.components.common.ErrorOverlay
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val MaxContentWidth = 640.dp
private val ContentPaddingH = 16.dp

/**
 * Workspace/Company settings screen with top bar.
 */
@Composable
internal fun WorkspaceSettingsScreen(
    state: WorkspaceSettingsState,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    onNavigateToPeppol: () -> Unit = {},
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    Scaffold(
        topBar = {
            if (!isLargeScreen) PTopAppBar(Res.string.workspace_settings_title)
        },
    ) { contentPadding ->
        Column(modifier = Modifier.padding(contentPadding)) {
            if (state.actionError != null) {
                DokusErrorBanner(
                    exception = state.actionError,
                    retryHandler = null,
                    modifier = Modifier.padding(horizontal = Constraints.Spacing.large),
                    onDismiss = { onIntent(WorkspaceSettingsIntent.DismissActionError) },
                )
            }

            WorkspaceSettingsContent(
                state = state,
                onIntent = onIntent,
                onNavigateToPeppol = onNavigateToPeppol,
            )
        }
    }
}

/**
 * Workspace settings content -- company hero + collapsible sections.
 */
@Composable
fun WorkspaceSettingsContent(
    state: WorkspaceSettingsState,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    onNavigateToPeppol: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Image picker - uploads directly without cropping
    val avatarPicker = rememberImagePicker { pickedImage ->
        onIntent(WorkspaceSettingsIntent.UploadAvatar(pickedImage.bytes, pickedImage.name))
    }

    val workspaceData = state.workspaceData

    ErrorOverlay(
        exception = if (workspaceData is DokusState.Error) workspaceData.exception else null,
        retryHandler = if (workspaceData is DokusState.Error) workspaceData.retryHandler else null,
    ) {
        when {
            workspaceData.isSuccess() -> {
                WorkspaceSettingsContentScreen(
                    state = state,
                    data = workspaceData.data,
                    onIntent = onIntent,
                    onNavigateToPeppol = onNavigateToPeppol,
                    avatarPicker = avatarPicker,
                    modifier = modifier,
                )
            }
            else -> {
                SettingsSkeleton(
                    sectionCount = 5,
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun WorkspaceSettingsContentScreen(
    state: WorkspaceSettingsState,
    data: WorkspaceSettingsState.WorkspaceData,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    onNavigateToPeppol: () -> Unit,
    avatarPicker: FilePickerLauncher,
    modifier: Modifier = Modifier,
) {
    val formState = state.form
    val saveState = state.saveState
    val avatarState = state.avatarState
    val currentAvatar = state.currentAvatar
    val peppolRegistration = data.peppolRegistration
    val peppolActivity = data.peppolActivity
    val editingSection = state.editingSection
    val isLegalIdentityLocked = state.isLegalIdentityLocked

    // Section expansion state
    var peppolExpanded by remember { mutableStateOf(false) }
    var bankingExpanded by remember { mutableStateOf(false) }
    var invoiceFormatExpanded by remember { mutableStateOf(false) }
    var paymentTermsExpanded by remember { mutableStateOf(false) }
    var processingHealthExpanded by remember { mutableStateOf(false) }

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
                .padding(top = Constraints.Spacing.medium),
        ) {
            // 1. Company Hero -- non-collapsible, centered avatar
            CompanyHeroSection(
                formState = formState,
                isLocked = isLegalIdentityLocked,
                editMode = editingSection == WorkspaceSettingsState.EditingSection.LegalIdentity,
                onEdit = {
                    onIntent(WorkspaceSettingsIntent.EnterEditMode(
                        WorkspaceSettingsState.EditingSection.LegalIdentity
                    ))
                },
                onSave = {
                    onIntent(WorkspaceSettingsIntent.SaveSection(
                        WorkspaceSettingsState.EditingSection.LegalIdentity
                    ))
                },
                onCancel = { onIntent(WorkspaceSettingsIntent.CancelEditMode) },
                onIntent = onIntent,
                avatarState = avatarState,
                currentAvatar = currentAvatar,
                avatarPicker = avatarPicker,
            )

            // 2. PEPPOL Connection -- collapsible
            PeppolConnectionSection(
                peppolRegistration = peppolRegistration,
                peppolActivity = peppolActivity,
                expanded = peppolExpanded,
                onToggle = { peppolExpanded = !peppolExpanded },
                onConfigurePeppol = onNavigateToPeppol,
            )

            // 3. Banking Details
            BankingDetailsSection(
                formState = formState,
                expanded = bankingExpanded,
                onToggle = { bankingExpanded = !bankingExpanded },
                editMode = editingSection == WorkspaceSettingsState.EditingSection.Banking,
                onEdit = {
                    onIntent(WorkspaceSettingsIntent.EnterEditMode(
                        WorkspaceSettingsState.EditingSection.Banking
                    ))
                },
                onSave = {
                    onIntent(WorkspaceSettingsIntent.SaveSection(
                        WorkspaceSettingsState.EditingSection.Banking
                    ))
                },
                onCancel = { onIntent(WorkspaceSettingsIntent.CancelEditMode) },
                onIntent = onIntent,
            )

            // 4. Invoice Format
            InvoiceFormatSection(
                formState = formState,
                expanded = invoiceFormatExpanded,
                onToggle = { invoiceFormatExpanded = !invoiceFormatExpanded },
                editMode = editingSection == WorkspaceSettingsState.EditingSection.InvoiceFormat,
                onEdit = {
                    onIntent(WorkspaceSettingsIntent.EnterEditMode(
                        WorkspaceSettingsState.EditingSection.InvoiceFormat
                    ))
                },
                onSave = {
                    onIntent(WorkspaceSettingsIntent.SaveSection(
                        WorkspaceSettingsState.EditingSection.InvoiceFormat
                    ))
                },
                onCancel = { onIntent(WorkspaceSettingsIntent.CancelEditMode) },
                onIntent = onIntent,
            )

            // 5. Payment Terms
            PaymentTermsSection(
                formState = formState,
                expanded = paymentTermsExpanded,
                onToggle = { paymentTermsExpanded = !paymentTermsExpanded },
                editMode = editingSection == WorkspaceSettingsState.EditingSection.PaymentTerms,
                onEdit = {
                    onIntent(WorkspaceSettingsIntent.EnterEditMode(
                        WorkspaceSettingsState.EditingSection.PaymentTerms
                    ))
                },
                onSave = {
                    onIntent(WorkspaceSettingsIntent.SaveSection(
                        WorkspaceSettingsState.EditingSection.PaymentTerms
                    ))
                },
                onCancel = { onIntent(WorkspaceSettingsIntent.CancelEditMode) },
                onIntent = onIntent,
            )

            // 6. Processing Health -- maintenance section
            ProcessingHealthSection(
                processingHealth = state.processingHealth,
                bulkReprocessState = state.bulkReprocessState,
                expanded = processingHealthExpanded,
                onToggle = { processingHealthExpanded = !processingHealthExpanded },
                onReprocess = { onIntent(WorkspaceSettingsIntent.ExecuteBulkReprocess) },
            )

            // Save State Feedback
            SaveStateFeedback(
                saveState = saveState,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(Modifier.height(Constraints.Spacing.xLarge))
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun WorkspaceSettingsContentLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        WorkspaceSettingsContent(
            state = WorkspaceSettingsState(),
            onIntent = {},
        )
    }
}

// =============================================================================
// SUPPORTING COMPONENTS
// =============================================================================

/**
 * Save state feedback component.
 */
@Composable
private fun SaveStateFeedback(
    saveState: WorkspaceSettingsState.SaveState,
    modifier: Modifier = Modifier
) {
    when (saveState) {
        is WorkspaceSettingsState.SaveState.Success -> {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(Res.string.settings_saved_successfully),
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }

        is WorkspaceSettingsState.SaveState.Error -> {
            Spacer(Modifier.height(12.dp))
            Text(
                text = saveState.error.localized,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier
            )
        }

        else -> {}
    }
}
