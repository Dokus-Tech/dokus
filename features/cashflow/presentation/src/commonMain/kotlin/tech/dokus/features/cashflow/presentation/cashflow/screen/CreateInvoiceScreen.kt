package tech.dokus.features.cashflow.presentation.cashflow.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.enums.InvoiceDueDateMode
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.CreateInvoiceState
import tech.dokus.features.cashflow.mvi.model.DatePickerTarget
import tech.dokus.features.cashflow.mvi.model.InvoiceResolvedAction
import tech.dokus.features.cashflow.mvi.model.InvoiceSection
import tech.dokus.features.cashflow.mvi.model.LatestInvoiceSuggestion
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.ContactSelectionPanel
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.InteractiveInvoiceDocument
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.InvoiceDatesSection
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.InvoiceLineItemsSection
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.InvoiceClientSection
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.Mocks
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PDatePickerDialog
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.layout.PCollapsibleSection
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun CreateInvoiceScreen(
    state: CreateInvoiceState,
    snackbarHostState: SnackbarHostState,
    onIntent: (CreateInvoiceIntent) -> Unit,
    onNavigateToCreateContact: () -> Unit
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    val formState = state.formState
    val uiState = state.uiState

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isLargeScreen) {
                MobileCommandFooter(
                    primaryLabel = primaryActionLabel(uiState.resolvedDeliveryAction.action),
                    isSaving = formState.isSaving,
                    onSaveDraft = { onIntent(CreateInvoiceIntent.SaveAsDraft) },
                    onPrimary = { onIntent(CreateInvoiceIntent.SubmitWithResolvedDelivery) }
                )
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreen) {
                DesktopInvoiceCreateContent(
                    state = state,
                    contentPadding = contentPadding,
                    onIntent = onIntent
                )
            } else {
                MobileInvoiceCreateContent(
                    state = state,
                    contentPadding = contentPadding,
                    onIntent = onIntent
                )
            }

            ContactSelectionPanel(
                isVisible = uiState.isClientPanelOpen,
                onDismiss = { onIntent(CreateInvoiceIntent.CloseClientPanel) },
                selectedContact = formState.selectedClient,
                searchQuery = uiState.clientSearchQuery,
                onSearchQueryChange = { onIntent(CreateInvoiceIntent.UpdateClientSearchQuery(it)) },
                onContactSelected = { autoFillData ->
                    onIntent(CreateInvoiceIntent.SelectClient(autoFillData.contact))
                    val paymentTerms = autoFillData.defaultPaymentTerms
                    if (paymentTerms > 0) {
                        onIntent(CreateInvoiceIntent.UpdatePaymentTermsDays(paymentTerms))
                        formState.issueDate?.let { issueDate ->
                            onIntent(CreateInvoiceIntent.UpdateDueDate(issueDate.plus(paymentTerms, DateTimeUnit.DAY)))
                        }
                    }
                    autoFillData.defaultVatRate?.toIntOrNull()?.let { vatRate ->
                        formState.items.firstOrNull()?.let { firstItem ->
                            onIntent(CreateInvoiceIntent.UpdateItemVatRate(firstItem.id, vatRate))
                        }
                    }
                },
                onAddNewContact = {
                    onIntent(CreateInvoiceIntent.CloseClientPanel)
                    onNavigateToCreateContact()
                }
            )

            if (uiState.isDatePickerOpen != null) {
                val initialDate = when (uiState.isDatePickerOpen) {
                    DatePickerTarget.IssueDate -> formState.issueDate
                    DatePickerTarget.DueDate -> formState.dueDate
                }
                PDatePickerDialog(
                    initialDate = initialDate,
                    onDateSelected = { date ->
                        if (date != null) {
                            onIntent(CreateInvoiceIntent.SelectDate(date))
                        } else {
                            onIntent(CreateInvoiceIntent.CloseDatePicker)
                        }
                    },
                    onDismiss = { onIntent(CreateInvoiceIntent.CloseDatePicker) }
                )
            }

            if (uiState.isPreviewVisible) {
                InvoicePreviewDialog(
                    state = state,
                    onDismiss = { onIntent(CreateInvoiceIntent.SetPreviewVisible(false)) }
                )
            }
        }
    }
}

@Composable
private fun DesktopInvoiceCreateContent(
    state: CreateInvoiceState,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onIntent: (CreateInvoiceIntent) -> Unit
) {
    val formState = state.formState
    val uiState = state.uiState
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 28.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(
                text = "New Invoice",
                onBackPress = { onIntent(CreateInvoiceIntent.BackClicked) }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PButton(
                    text = "Preview",
                    variant = PButtonVariant.Outline,
                    onClick = { onIntent(CreateInvoiceIntent.SetPreviewVisible(true)) }
                )
                state.invoiceNumberPreview?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        InteractiveInvoiceDocument(
            formState = formState,
            uiState = uiState,
            onClientClick = { onIntent(CreateInvoiceIntent.OpenClientPanel) },
            onIssueDateClick = { onIntent(CreateInvoiceIntent.OpenIssueDatePicker) },
            onDueDateClick = { onIntent(CreateInvoiceIntent.OpenDueDatePicker) },
            onItemClick = { onIntent(CreateInvoiceIntent.ExpandItem(it)) },
            onItemCollapse = { onIntent(CreateInvoiceIntent.CollapseItem) },
            onAddItem = { onIntent(CreateInvoiceIntent.AddLineItem) },
            onRemoveItem = { onIntent(CreateInvoiceIntent.RemoveLineItem(it)) },
            onUpdateItemDescription = { id, desc ->
                onIntent(CreateInvoiceIntent.UpdateItemDescription(id, desc))
            },
            onUpdateItemQuantity = { id, qty ->
                onIntent(CreateInvoiceIntent.UpdateItemQuantity(id, qty))
            },
            onUpdateItemUnitPrice = { id, price ->
                onIntent(CreateInvoiceIntent.UpdateItemUnitPrice(id, price))
            },
            onUpdateItemVatRate = { id, rate ->
                onIntent(CreateInvoiceIntent.UpdateItemVatRate(id, rate))
            }
        )

        uiState.latestInvoiceSuggestion?.let { suggestion ->
            LatestInvoiceSuggestionStrip(
                suggestion = suggestion,
                onApply = { onIntent(CreateInvoiceIntent.ApplyLatestInvoiceLines) },
                onDismiss = { onIntent(CreateInvoiceIntent.DismissLatestInvoiceSuggestion) }
            )
        }

        PaymentDeliveryEditor(
            state = state,
            onIntent = onIntent
        )

        DatesTermsEditor(formState = formState, onIntent = onIntent)

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PButton(
                    text = "Save draft",
                    variant = PButtonVariant.Outline,
                    isEnabled = !formState.isSaving,
                    onClick = { onIntent(CreateInvoiceIntent.SaveAsDraft) }
                )
                PButton(
                    text = primaryActionLabel(uiState.resolvedDeliveryAction.action),
                    isEnabled = !formState.isSaving,
                    isLoading = formState.isSaving,
                    onClick = { onIntent(CreateInvoiceIntent.SubmitWithResolvedDelivery) }
                )
            }
        }
    }
}

@Composable
private fun MobileInvoiceCreateContent(
    state: CreateInvoiceState,
    contentPadding: androidx.compose.foundation.layout.PaddingValues,
    onIntent: (CreateInvoiceIntent) -> Unit
) {
    val formState = state.formState
    val uiState = state.uiState
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionTitle(
            text = "New Invoice",
            onBackPress = { onIntent(CreateInvoiceIntent.BackClicked) }
        )
        state.invoiceNumberPreview?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        AccordionSection(
            title = "Client",
            section = InvoiceSection.Client,
            state = uiState,
            onIntent = onIntent
        ) {
            InvoiceClientSection(
                client = formState.selectedClient,
                onClick = { onIntent(CreateInvoiceIntent.OpenClientPanel) }
            )
            if (formState.peppolStatusLoading) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text("Checking PEPPOL status...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        AccordionSection(
            title = "Line items",
            section = InvoiceSection.LineItems,
            state = uiState,
            onIntent = onIntent
        ) {
            uiState.latestInvoiceSuggestion?.let { suggestion ->
                LatestInvoiceSuggestionStrip(
                    suggestion = suggestion,
                    onApply = { onIntent(CreateInvoiceIntent.ApplyLatestInvoiceLines) },
                    onDismiss = { onIntent(CreateInvoiceIntent.DismissLatestInvoiceSuggestion) }
                )
            }
            InvoiceLineItemsSection(
                items = formState.items,
                onAddItem = { onIntent(CreateInvoiceIntent.AddLineItem) },
                onRemoveItem = { onIntent(CreateInvoiceIntent.RemoveLineItem(it)) },
                onUpdateDescription = { id, desc -> onIntent(CreateInvoiceIntent.UpdateItemDescription(id, desc)) },
                onUpdateQuantity = { id, qty -> onIntent(CreateInvoiceIntent.UpdateItemQuantity(id, qty)) },
                onUpdateUnitPrice = { id, value -> onIntent(CreateInvoiceIntent.UpdateItemUnitPrice(id, value)) },
                onUpdateVatRate = { id, rate -> onIntent(CreateInvoiceIntent.UpdateItemVatRate(id, rate)) },
                error = formState.errors["items"]
            )
        }

        AccordionSection(
            title = "Payment & delivery",
            section = InvoiceSection.PaymentDelivery,
            state = uiState,
            onIntent = onIntent
        ) {
            PaymentDeliveryEditor(state, onIntent)
        }

        AccordionSection(
            title = "Dates & terms",
            section = InvoiceSection.DatesTerms,
            state = uiState,
            onIntent = onIntent
        ) {
            DatesTermsEditor(formState = formState, onIntent = onIntent)
        }

        PButton(
            text = "Preview",
            variant = PButtonVariant.Outline,
            onClick = { onIntent(CreateInvoiceIntent.SetPreviewVisible(true)) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 84.dp)
        )
    }
}

@Composable
private fun LatestInvoiceSuggestionStrip(
    suggestion: LatestInvoiceSuggestion,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Dense
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Last invoice: ${suggestion.issueDate}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${suggestion.lines.size} line item(s) available",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PButton(
                    text = "Reuse lines",
                    variant = PButtonVariant.Outline,
                    onClick = onApply
                )
                PButton(
                    text = "Dismiss",
                    variant = PButtonVariant.Outline,
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun AccordionSection(
    title: String,
    section: InvoiceSection,
    state: tech.dokus.features.cashflow.mvi.model.CreateInvoiceUiState,
    onIntent: (CreateInvoiceIntent) -> Unit,
    content: @Composable () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Dense
    ) {
        PCollapsibleSection(
            title = title,
            isExpanded = state.expandedSections.contains(section),
            onToggle = { onIntent(CreateInvoiceIntent.ToggleSection(section)) },
            right = if (state.suggestedSection == section) "Suggested" else null
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun PaymentDeliveryEditor(
    state: CreateInvoiceState,
    onIntent: (CreateInvoiceIntent) -> Unit
) {
    val formState = state.formState
    val uiState = state.uiState
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Delivery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            DeliveryOptionRow(
                title = "PEPPOL",
                selected = uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.Peppol,
                onClick = { onIntent(CreateInvoiceIntent.SelectDeliveryPreference(InvoiceDeliveryMethod.Peppol)) }
            )
            DeliveryOptionRow(
                title = "PDF export",
                selected = uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.PdfExport,
                onClick = { onIntent(CreateInvoiceIntent.SelectDeliveryPreference(InvoiceDeliveryMethod.PdfExport)) }
            )
            if (uiState.resolvedDeliveryAction.action == InvoiceResolvedAction.PdfExport &&
                uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.Peppol
            ) {
                Text(
                    text = uiState.resolvedDeliveryAction.reason ?: "PEPPOL unavailable. Falling back to PDF export.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            PTextFieldStandard(
                fieldName = "Sender IBAN",
                value = formState.senderIban,
                onValueChange = { onIntent(CreateInvoiceIntent.UpdateSenderIban(it)) },
                modifier = Modifier.fillMaxWidth()
            )
            PTextFieldStandard(
                fieldName = "Sender BIC",
                value = formState.senderBic,
                onValueChange = { onIntent(CreateInvoiceIntent.UpdateSenderBic(it)) },
                modifier = Modifier.fillMaxWidth()
            )
            PTextFieldStandard(
                fieldName = "Structured reference",
                value = formState.structuredCommunication,
                onValueChange = { onIntent(CreateInvoiceIntent.UpdateStructuredCommunication(it)) },
                modifier = Modifier.fillMaxWidth()
            )
            PTextFieldStandard(
                fieldName = "Note",
                value = formState.notes,
                singleLine = false,
                onValueChange = { onIntent(CreateInvoiceIntent.UpdateNotes(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DeliveryOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun DatesTermsEditor(
    formState: tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState,
    onIntent: (CreateInvoiceIntent) -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Dates & terms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            InvoiceDatesSection(
                issueDate = formState.issueDate,
                dueDate = formState.dueDate,
                onIssueDateClick = { onIntent(CreateInvoiceIntent.OpenIssueDatePicker) },
                onDueDateClick = { onIntent(CreateInvoiceIntent.OpenDueDatePicker) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = formState.dueDateMode == InvoiceDueDateMode.Terms,
                    onClick = { onIntent(CreateInvoiceIntent.UpdateDueDateMode(InvoiceDueDateMode.Terms)) },
                    label = { Text("Terms") }
                )
                FilterChip(
                    selected = formState.dueDateMode == InvoiceDueDateMode.FixedDate,
                    onClick = { onIntent(CreateInvoiceIntent.UpdateDueDateMode(InvoiceDueDateMode.FixedDate)) },
                    label = { Text("Fixed date") }
                )
            }
            PTextFieldStandard(
                fieldName = "Payment terms (days)",
                value = formState.paymentTermsDays.toString(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                onValueChange = { value ->
                    val parsed = value.toIntOrNull()
                    if (parsed != null) onIntent(CreateInvoiceIntent.UpdatePaymentTermsDays(parsed))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MobileCommandFooter(
    primaryLabel: String,
    isSaving: Boolean,
    onSaveDraft: () -> Unit,
    onPrimary: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PButton(
            text = "Save draft",
            variant = PButtonVariant.Outline,
            isEnabled = !isSaving,
            onClick = onSaveDraft,
            modifier = Modifier.weight(1f)
        )
        PButton(
            text = primaryLabel,
            isEnabled = !isSaving,
            isLoading = isSaving,
            onClick = onPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun InvoicePreviewDialog(
    state: CreateInvoiceState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invoice preview") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(state.invoiceNumberPreview ?: "New invoice")
                Text("Client: ${state.formState.selectedClient?.name?.value ?: "-"}")
                Text("Issue date: ${state.formState.issueDate ?: "-"}")
                Text("Due date: ${state.formState.dueDate ?: "-"}")
                Text("Total: ${state.formState.total}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun primaryActionLabel(action: InvoiceResolvedAction): String {
    return when (action) {
        InvoiceResolvedAction.Peppol -> "Send via PEPPOL"
        InvoiceResolvedAction.PdfExport -> "Export PDF"
    }
}

@Preview(name = "Invoice Footer Mobile", widthDp = 390, heightDp = 120)
@Composable
private fun MobileCommandFooterPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        MobileCommandFooter(
            primaryLabel = "Send via PEPPOL",
            isSaving = false,
            onSaveDraft = {},
            onPrimary = {}
        )
    }
}

@Preview(name = "Payment Delivery Editor", widthDp = 390, heightDp = 900)
@Composable
private fun PaymentDeliveryEditorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PaymentDeliveryEditor(
            state = CreateInvoiceState(
                formState = Mocks.sampleFormStateWithWarning,
                uiState = Mocks.sampleUiState.copy(
                    selectedDeliveryPreference = InvoiceDeliveryMethod.Peppol,
                    resolvedDeliveryAction = tech.dokus.features.cashflow.mvi.model.DeliveryResolution(
                        action = InvoiceResolvedAction.PdfExport,
                        reason = "PEPPOL unavailable for selected client."
                    )
                )
            ),
            onIntent = {}
        )
    }
}

@Preview(name = "Dates Terms Editor", widthDp = 390, heightDp = 420)
@Composable
private fun DatesTermsEditorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DatesTermsEditor(
            formState = Mocks.sampleFormState,
            onIntent = {}
        )
    }
}

private val PreviewExpandedSections = setOf(
    InvoiceSection.Client,
    InvoiceSection.LineItems,
    InvoiceSection.PaymentDelivery,
    InvoiceSection.DatesTerms
)

@Preview(name = "CreateInvoice Mobile", widthDp = 390, heightDp = 1600)
@Composable
private fun CreateInvoiceScreenMobilePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = CreateInvoiceState(
                formState = Mocks.sampleFormState,
                uiState = Mocks.sampleUiState.copy(
                    expandedSections = PreviewExpandedSections,
                    selectedDeliveryPreference = InvoiceDeliveryMethod.Peppol,
                    resolvedDeliveryAction = tech.dokus.features.cashflow.mvi.model.DeliveryResolution(
                        action = InvoiceResolvedAction.Peppol
                    )
                ),
                invoiceNumberPreview = "INV-2026-0003"
            ),
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            onIntent = {},
            onNavigateToCreateContact = {},
        )
    }
}

@Preview(name = "CreateInvoice Mobile Fallback", widthDp = 390, heightDp = 1600)
@Composable
private fun CreateInvoiceScreenMobileFallbackPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = CreateInvoiceState(
                formState = Mocks.sampleFormStateWithWarning,
                uiState = Mocks.sampleUiState.copy(
                    expandedSections = PreviewExpandedSections,
                    selectedDeliveryPreference = InvoiceDeliveryMethod.Peppol,
                    resolvedDeliveryAction = tech.dokus.features.cashflow.mvi.model.DeliveryResolution(
                        action = InvoiceResolvedAction.PdfExport,
                        reason = "PEPPOL unavailable for selected client."
                    )
                ),
                invoiceNumberPreview = "INV-2026-0003"
            ),
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            onIntent = {},
            onNavigateToCreateContact = {},
        )
    }
}

@Preview(name = "CreateInvoice Desktop", widthDp = 1200, heightDp = 1000)
@Composable
private fun CreateInvoiceScreenDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = CreateInvoiceState(
                formState = Mocks.sampleFormState,
                uiState = Mocks.sampleUiState.copy(
                    selectedDeliveryPreference = InvoiceDeliveryMethod.Peppol,
                    resolvedDeliveryAction = tech.dokus.features.cashflow.mvi.model.DeliveryResolution(
                        action = InvoiceResolvedAction.Peppol
                    )
                ),
                invoiceNumberPreview = "INV-2026-0003"
            ),
            snackbarHostState = androidx.compose.material3.SnackbarHostState(),
            onIntent = {},
            onNavigateToCreateContact = {},
        )
    }
}
