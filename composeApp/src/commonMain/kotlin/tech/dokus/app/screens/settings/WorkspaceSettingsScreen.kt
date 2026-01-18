package tech.dokus.app.screens.settings

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_change
import tech.dokus.aura.resources.action_remove
import tech.dokus.aura.resources.action_upload
import tech.dokus.aura.resources.save_changes
import tech.dokus.aura.resources.settings_saved_successfully
import tech.dokus.aura.resources.state_removing
import tech.dokus.aura.resources.state_retry
import tech.dokus.aura.resources.state_uploading
import tech.dokus.aura.resources.workspace_address
import tech.dokus.aura.resources.workspace_banking
import tech.dokus.aura.resources.workspace_bic
import tech.dokus.aura.resources.workspace_company_logo
import tech.dokus.aura.resources.workspace_company_name
import tech.dokus.aura.resources.workspace_iban
import tech.dokus.aura.resources.workspace_invoice_include_year
import tech.dokus.aura.resources.workspace_invoice_numbering
import tech.dokus.aura.resources.workspace_invoice_numbering_description
import tech.dokus.aura.resources.workspace_invoice_padding
import tech.dokus.aura.resources.workspace_invoice_prefix
import tech.dokus.aura.resources.workspace_invoice_prefix_description
import tech.dokus.aura.resources.workspace_invoice_preview
import tech.dokus.aura.resources.workspace_invoice_settings
import tech.dokus.aura.resources.workspace_invoice_settings_description
import tech.dokus.aura.resources.workspace_invoice_yearly_reset
import tech.dokus.aura.resources.workspace_legal_name
import tech.dokus.aura.resources.workspace_payment_days_description
import tech.dokus.aura.resources.workspace_payment_terms
import tech.dokus.aura.resources.workspace_payment_terms_description
import tech.dokus.aura.resources.workspace_payment_terms_section
import tech.dokus.aura.resources.workspace_payment_terms_text
import tech.dokus.aura.resources.workspace_settings_load_failed
import tech.dokus.aura.resources.workspace_settings_title
import tech.dokus.aura.resources.workspace_vat_number
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.model.PeppolActivityDto
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.domain.model.common.Thumbnail
import tech.dokus.foundation.app.picker.FilePickerLauncher
import tech.dokus.foundation.app.picker.rememberImagePicker
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.fields.PTextFieldFree
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.settings.DataRow
import tech.dokus.foundation.aura.components.settings.DataRowStatus
import tech.dokus.foundation.aura.components.settings.SettingsSection
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.textMuted

// Invoice padding configuration options (number of digits)
private const val InvoicePaddingMin = 3
private const val InvoicePaddingDefault = 4
private const val InvoicePaddingMedium = 5
private const val InvoicePaddingMax = 6
private val InvoicePaddingOptions = listOf(
    InvoicePaddingMin,
    InvoicePaddingDefault,
    InvoicePaddingMedium,
    InvoicePaddingMax
)

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
                CircularProgressIndicator()
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
// PEPPOL CONNECTION SECTION
// =============================================================================

@Composable
private fun PeppolConnectionSection(
    peppolRegistration: PeppolRegistrationDto?,
    peppolActivity: PeppolActivityDto?,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val status = peppolRegistration?.status
    val sectionStatus = when (status) {
        PeppolRegistrationStatus.Active -> DataRowStatus("Compliant", StatusDotType.Confirmed)
        PeppolRegistrationStatus.SendingOnly -> DataRowStatus("Sending Only", StatusDotType.Warning)
        PeppolRegistrationStatus.WaitingTransfer -> DataRowStatus("Awaiting Transfer", StatusDotType.Neutral)
        PeppolRegistrationStatus.Pending -> DataRowStatus("Pending", StatusDotType.Neutral)
        PeppolRegistrationStatus.Failed -> DataRowStatus("Error", StatusDotType.Error)
        PeppolRegistrationStatus.External -> DataRowStatus("External", StatusDotType.Neutral)
        PeppolRegistrationStatus.NotConfigured, null -> DataRowStatus("Not Configured", StatusDotType.Empty)
    }

    val subtitle = if (!expanded) {
        peppolRegistration?.peppolId ?: "Not configured"
    } else null

    SettingsSection(
        title = "PEPPOL Connection",
        subtitle = subtitle,
        status = sectionStatus,
        expanded = expanded,
        onToggle = onToggle,
        primary = true, // Elevated background
    ) {
        if (peppolRegistration != null && status != PeppolRegistrationStatus.NotConfigured) {
            // Participant ID
            DataRow(
                label = "Participant ID",
                value = peppolRegistration.peppolId,
                mono = true,
                locked = true,
                status = if (status == PeppolRegistrationStatus.Active) {
                    DataRowStatus("Verified", StatusDotType.Confirmed)
                } else null,
            )

            // Access Point
            DataRow(
                label = "Access Point",
                value = "Managed by Dokus",
                status = if (status == PeppolRegistrationStatus.Active) {
                    DataRowStatus("Connected", StatusDotType.Confirmed)
                } else null,
            )

            // Inbound status
            val inboundStatus = when {
                peppolRegistration.canReceive && peppolActivity?.lastInboundAt != null ->
                    DataRowStatus(formatRelativeTime(peppolActivity.lastInboundAt), StatusDotType.Confirmed)
                peppolRegistration.canReceive ->
                    DataRowStatus("Active", StatusDotType.Confirmed)
                status == PeppolRegistrationStatus.SendingOnly ->
                    DataRowStatus("Blocked", StatusDotType.Warning)
                else ->
                    DataRowStatus("Inactive", StatusDotType.Neutral)
            }
            DataRow(
                label = "Inbound",
                value = if (peppolRegistration.canReceive) "Active" else "Inactive",
                status = inboundStatus,
            )

            // Outbound status
            val outboundStatus = when {
                peppolRegistration.canSend && peppolActivity?.lastOutboundAt != null ->
                    DataRowStatus(formatRelativeTime(peppolActivity.lastOutboundAt), StatusDotType.Confirmed)
                peppolRegistration.canSend ->
                    DataRowStatus("Active", StatusDotType.Confirmed)
                else ->
                    DataRowStatus("Inactive", StatusDotType.Neutral)
            }
            DataRow(
                label = "Outbound",
                value = if (peppolRegistration.canSend) "Active" else "Inactive",
                status = outboundStatus,
            )

            // Compliance note
            if (status == PeppolRegistrationStatus.Active) {
                Spacer(Modifier.height(Constrains.Spacing.medium))
                Text(
                    text = "Belgium PEPPOL mandate effective January 1, 2026.\nYour business is compliant.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        } else {
            // Not configured
            Text(
                text = "PEPPOL e-invoicing is not configured for your workspace.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(Constrains.Spacing.small))
            Text(
                text = "Belgium requires PEPPOL for B2G invoicing from January 1, 2026.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }
    }
}

// =============================================================================
// LEGAL IDENTITY SECTION
// =============================================================================

@Composable
private fun LegalIdentitySection(
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
    val sectionStatus = if (isLocked) {
        DataRowStatus("Verified", StatusDotType.Confirmed)
    } else null

    SettingsSection(
        title = "Legal Identity",
        subtitle = subtitle,
        status = sectionStatus,
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
                    mono = true,
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
                mono = true,
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

// =============================================================================
// BANKING DETAILS SECTION
// =============================================================================

@Composable
private fun BankingDetailsSection(
    formState: WorkspaceSettingsState.Content.FormState,
    expanded: Boolean,
    onToggle: () -> Unit,
    editMode: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
) {
    val subtitle = if (!expanded && formState.iban.isNotBlank()) {
        formState.iban.take(10) + "..."
    } else null

    SettingsSection(
        title = stringResource(Res.string.workspace_banking),
        subtitle = subtitle,
        expanded = expanded,
        onToggle = onToggle,
        editMode = editMode,
        onEdit = onEdit,
        onSave = onSave,
        onCancel = onCancel,
    ) {
        if (editMode) {
            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_iban),
                value = formState.iban,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateIban(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constrains.Spacing.small))

            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_bic),
                value = formState.bic,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateBic(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            DataRow(
                label = stringResource(Res.string.workspace_iban),
                value = formState.iban,
                mono = true,
            )

            DataRow(
                label = stringResource(Res.string.workspace_bic),
                value = formState.bic,
                mono = true,
            )
        }
    }
}

// =============================================================================
// INVOICE FORMAT SECTION
// =============================================================================

@Composable
private fun InvoiceFormatSection(
    formState: WorkspaceSettingsState.Content.FormState,
    expanded: Boolean,
    onToggle: () -> Unit,
    editMode: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
) {
    val previewNumber = generateInvoiceNumberPreview(
        formState.invoicePrefix,
        formState.invoiceIncludeYear,
        formState.invoicePadding
    )
    val subtitle = if (!expanded) previewNumber else null

    SettingsSection(
        title = stringResource(Res.string.workspace_invoice_settings),
        subtitle = subtitle,
        expanded = expanded,
        onToggle = onToggle,
        editMode = editMode,
        onEdit = onEdit,
        onSave = onSave,
        onCancel = onCancel,
    ) {
        if (editMode) {
            Text(
                text = stringResource(Res.string.workspace_invoice_settings_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constrains.Spacing.medium))

            // Invoice Prefix
            Text(
                text = stringResource(Res.string.workspace_invoice_prefix_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )
            Spacer(Modifier.height(Constrains.Spacing.xSmall))
            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_invoice_prefix),
                value = formState.invoicePrefix,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateInvoicePrefix(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constrains.Spacing.medium))

            // Invoice Numbering Subsection
            Text(
                text = stringResource(Res.string.workspace_invoice_numbering),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(Res.string.workspace_invoice_numbering_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constrains.Spacing.small))

            // Include Year in Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = formState.invoiceIncludeYear,
                    onCheckedChange = { onIntent(WorkspaceSettingsIntent.UpdateInvoiceIncludeYear(it)) }
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
                    onCheckedChange = { onIntent(WorkspaceSettingsIntent.UpdateInvoiceYearlyReset(it)) }
                )
                Text(
                    text = stringResource(Res.string.workspace_invoice_yearly_reset),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Constrains.Spacing.small))

            // Number Padding Selector
            Text(
                text = stringResource(Res.string.workspace_invoice_padding),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.textMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
            ) {
                InvoicePaddingOptions.forEach { padding ->
                    TextButton(
                        onClick = { onIntent(WorkspaceSettingsIntent.UpdateInvoicePadding(padding)) }
                    ) {
                        Text(
                            text = padding.toString(),
                            color = if (formState.invoicePadding == padding) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.textMuted
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Constrains.Spacing.small))

            // Preview in highlighted card
            InvoicePreviewCard(previewNumber)
        } else {
            DataRow(
                label = stringResource(Res.string.workspace_invoice_prefix),
                value = formState.invoicePrefix,
            )

            DataRow(
                label = stringResource(Res.string.workspace_invoice_preview),
                value = previewNumber,
                mono = true,
            )

            DataRow(
                label = stringResource(Res.string.workspace_invoice_include_year),
                value = if (formState.invoiceIncludeYear) "Yes" else "No",
            )

            DataRow(
                label = stringResource(Res.string.workspace_invoice_yearly_reset),
                value = if (formState.invoiceYearlyReset) "Yes" else "No",
            )
        }
    }
}

@Composable
private fun InvoicePreviewCard(previewNumber: String) {
    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Row(
            modifier = Modifier.padding(Constrains.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.workspace_invoice_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.textMuted
            )
            Spacer(Modifier.width(Constrains.Spacing.small))
            Text(
                text = previewNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// =============================================================================
// PAYMENT TERMS SECTION
// =============================================================================

@Composable
private fun PaymentTermsSection(
    formState: WorkspaceSettingsState.Content.FormState,
    expanded: Boolean,
    onToggle: () -> Unit,
    editMode: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
) {
    val subtitle = if (!expanded) "${formState.defaultPaymentTerms} days" else null

    SettingsSection(
        title = stringResource(Res.string.workspace_payment_terms_section),
        subtitle = subtitle,
        expanded = expanded,
        onToggle = onToggle,
        editMode = editMode,
        onEdit = onEdit,
        onSave = onSave,
        onCancel = onCancel,
    ) {
        if (editMode) {
            Text(
                text = stringResource(Res.string.workspace_payment_terms_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constrains.Spacing.medium))

            // Payment Terms (Days)
            Text(
                text = stringResource(Res.string.workspace_payment_days_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )
            Spacer(Modifier.height(Constrains.Spacing.xSmall))
            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_payment_terms),
                value = formState.defaultPaymentTerms.toString(),
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateDefaultPaymentTerms(it)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constrains.Spacing.medium))

            // Payment Terms Text
            PTextFieldFree(
                fieldName = stringResource(Res.string.workspace_payment_terms_text),
                value = formState.paymentTermsText,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdatePaymentTermsText(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            DataRow(
                label = stringResource(Res.string.workspace_payment_terms),
                value = "${formState.defaultPaymentTerms} days",
            )

            if (formState.paymentTermsText.isNotBlank()) {
                DataRow(
                    label = stringResource(Res.string.workspace_payment_terms_text),
                    value = formState.paymentTermsText,
                )
            }
        }
    }
}

// =============================================================================
// SUPPORTING COMPONENTS
// =============================================================================

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

// =============================================================================
// UTILITY FUNCTIONS
// =============================================================================

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
        "$effectivePrefix-2026-$paddedNumber"
    } else {
        "$effectivePrefix-$paddedNumber"
    }
}

/**
 * Format a LocalDateTime as a relative time string (e.g., "2h ago", "5d ago").
 */
private fun formatRelativeTime(dateTime: LocalDateTime?): String {
    if (dateTime == null) return ""

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val nowInstant = Clock.System.now()

    // Simple calculation based on minutes/hours/days difference
    // This is approximate - a real implementation would use a proper duration library
    val diffMinutes = try {
        val nowHour = now.hour + now.dayOfYear * 24
        val thenHour = dateTime.hour + dateTime.dayOfYear * 24
        (nowHour - thenHour) * 60 + (now.minute - dateTime.minute)
    } catch (e: Exception) {
        0
    }

    return when {
        diffMinutes < 1 -> "just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
        diffMinutes < 10080 -> "${diffMinutes / 1440}d ago"
        else -> "${diffMinutes / 10080}w ago"
    }
}
