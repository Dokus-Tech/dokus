package tech.dokus.app.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import tech.dokus.foundation.aura.components.common.DokusLoader
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
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.screens.settings.sections.BankingDetailsSection
import tech.dokus.app.screens.settings.sections.InvoiceFormatSection
import tech.dokus.app.screens.settings.sections.LegalIdentitySection
import tech.dokus.app.screens.settings.sections.PaymentTermsSection
import tech.dokus.app.screens.settings.sections.PeppolConnectionSection
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.settings_saved_successfully
import tech.dokus.aura.resources.state_retry
import tech.dokus.aura.resources.workspace_settings_load_failed
import tech.dokus.aura.resources.workspace_company_settings
import tech.dokus.aura.resources.workspace_settings_title
import tech.dokus.foundation.app.picker.FilePickerLauncher
import tech.dokus.foundation.app.picker.rememberImagePicker
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.textMuted

// Max content width for settings screen
private val MaxContentWidth = 800.dp

/**
 * Workspace/Company settings screen with top bar.
 * Pure UI composable that takes state and callbacks.
 */
@Composable
internal fun WorkspaceSettingsScreen(
    state: WorkspaceSettingsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (WorkspaceSettingsIntent) -> Unit
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    Scaffold(
        topBar = {
            if (!isLargeScreen) PTopAppBar(Res.string.workspace_settings_title)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { contentPadding ->
        WorkspaceSettingsContent(
            state = state,
            onIntent = onIntent,
            modifier = Modifier.padding(contentPadding)
        )
    }
}

/**
 * Workspace settings content without scaffold.
 * Can be embedded in split-pane layout for desktop.
 */
@Composable
fun WorkspaceSettingsContent(
    state: WorkspaceSettingsState,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    WorkspaceSettingsContentInternal(
        state = state,
        onIntent = onIntent,
        modifier = modifier.padding(contentPadding)
    )
}

@Composable
internal fun WorkspaceSettingsContentInternal(
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
                DokusLoader()
            }
        }

        is WorkspaceSettingsState.Content -> {
            WorkspaceSettingsContentScreen(
                state = state,
                onIntent = onIntent,
                avatarPicker = avatarPicker,
                modifier = modifier,
            )
        }

        is WorkspaceSettingsState.Error -> {
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

@Composable
private fun WorkspaceSettingsContentScreen(
    state: WorkspaceSettingsState.Content,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
    avatarPicker: FilePickerLauncher,
    modifier: Modifier = Modifier,
) {
    val formState = state.form
    val saveState = state.saveState
    val avatarState = state.avatarState
    val currentAvatar = state.currentAvatar
    val peppolRegistration = state.peppolRegistration
    val peppolActivity = state.peppolActivity
    val editingSection = state.editingSection
    val isLegalIdentityLocked = state.isLegalIdentityLocked

    // Section expansion state
    var peppolExpanded by remember { mutableStateOf(true) }
    var legalIdentityExpanded by remember { mutableStateOf(!isLegalIdentityLocked) }
    var bankingExpanded by remember { mutableStateOf(false) }
    var invoiceFormatExpanded by remember { mutableStateOf(false) }
    var paymentTermsExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .withContentPaddingForScrollable(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier.widthIn(max = MaxContentWidth),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
        ) {
            // Page Header with company info
            Column {
                Text(
                    text = stringResource(Res.string.workspace_company_settings),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(Constrains.Spacing.xSmall))
                Text(
                    text = "${formState.legalName} · ${formState.vatNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }

            Spacer(Modifier.height(Constrains.Spacing.large))

            // 1. PEPPOL Connection Section (always first, primary visual weight)
            PeppolConnectionSection(
                peppolRegistration = peppolRegistration,
                peppolActivity = peppolActivity,
                expanded = peppolExpanded,
                onToggle = { peppolExpanded = !peppolExpanded },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

            // 2. Legal Identity Section
            LegalIdentitySection(
                formState = formState,
                isLocked = isLegalIdentityLocked,
                expanded = legalIdentityExpanded,
                onToggle = { legalIdentityExpanded = !legalIdentityExpanded },
                editMode = editingSection == WorkspaceSettingsState.Content.EditingSection.LegalIdentity,
                onEdit = {
                    onIntent(WorkspaceSettingsIntent.EnterEditMode(
                        WorkspaceSettingsState.Content.EditingSection.LegalIdentity
                    ))
                },
                onSave = {
                    onIntent(WorkspaceSettingsIntent.SaveSection(
                        WorkspaceSettingsState.Content.EditingSection.LegalIdentity
                    ))
                },
                onCancel = { onIntent(WorkspaceSettingsIntent.CancelEditMode) },
                onIntent = onIntent,
                avatarState = avatarState,
                currentAvatar = currentAvatar,
                avatarPicker = avatarPicker,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

            // 3. Banking Details Section
            BankingDetailsSection(
                formState = formState,
                expanded = bankingExpanded,
                onToggle = { bankingExpanded = !bankingExpanded },
                editMode = editingSection == WorkspaceSettingsState.Content.EditingSection.Banking,
                onEdit = {
                    onIntent(WorkspaceSettingsIntent.EnterEditMode(
                        WorkspaceSettingsState.Content.EditingSection.Banking
                    ))
                },
                onSave = {
                    onIntent(WorkspaceSettingsIntent.SaveSection(
                        WorkspaceSettingsState.Content.EditingSection.Banking
                    ))
                },
                onCancel = { onIntent(WorkspaceSettingsIntent.CancelEditMode) },
                onIntent = onIntent,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

            // 4. Invoice Format Section
            InvoiceFormatSection(
                formState = formState,
                expanded = invoiceFormatExpanded,
                onToggle = { invoiceFormatExpanded = !invoiceFormatExpanded },
                editMode = editingSection == WorkspaceSettingsState.Content.EditingSection.InvoiceFormat,
                onEdit = {
                    onIntent(WorkspaceSettingsIntent.EnterEditMode(
                        WorkspaceSettingsState.Content.EditingSection.InvoiceFormat
                    ))
                },
                onSave = {
                    onIntent(WorkspaceSettingsIntent.SaveSection(
                        WorkspaceSettingsState.Content.EditingSection.InvoiceFormat
                    ))
                },
                onCancel = { onIntent(WorkspaceSettingsIntent.CancelEditMode) },
                onIntent = onIntent,
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

            // 5. Payment Terms Section
            PaymentTermsSection(
                formState = formState,
                expanded = paymentTermsExpanded,
                onToggle = { paymentTermsExpanded = !paymentTermsExpanded },
                editMode = editingSection == WorkspaceSettingsState.Content.EditingSection.PaymentTerms,
                onEdit = {
                    onIntent(WorkspaceSettingsIntent.EnterEditMode(
                        WorkspaceSettingsState.Content.EditingSection.PaymentTerms
                    ))
                },
                onSave = {
                    onIntent(WorkspaceSettingsIntent.SaveSection(
                        WorkspaceSettingsState.Content.EditingSection.PaymentTerms
                    ))
                },
                onCancel = { onIntent(WorkspaceSettingsIntent.CancelEditMode) },
                onIntent = onIntent,
            )

            // Save State Feedback
            SaveStateFeedback(
                saveState = saveState,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(Constrains.Spacing.xLarge))
        }
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
    saveState: WorkspaceSettingsState.Content.SaveState,
    modifier: Modifier = Modifier
) {
    when (saveState) {
        is WorkspaceSettingsState.Content.SaveState.Success -> {
            Spacer(Modifier.height(Constrains.Spacing.medium))
            Text(
                text = stringResource(Res.string.settings_saved_successfully),
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }

        is WorkspaceSettingsState.Content.SaveState.Error -> {
            Spacer(Modifier.height(Constrains.Spacing.medium))
            Text(
                text = saveState.error.localized,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier
            )
        }

        else -> {}
    }
}
