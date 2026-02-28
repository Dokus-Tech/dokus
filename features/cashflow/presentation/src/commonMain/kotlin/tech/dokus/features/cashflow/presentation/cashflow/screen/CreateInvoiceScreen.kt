package tech.dokus.features.cashflow.presentation.cashflow.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.enums.InvoiceDueDateMode
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraft
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.ids.Iban
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.CreateInvoiceState
import tech.dokus.features.cashflow.mvi.model.ClientSuggestion
import tech.dokus.features.cashflow.mvi.model.DeliveryResolution
import tech.dokus.features.cashflow.mvi.model.DatePickerTarget
import tech.dokus.features.cashflow.mvi.model.ExternalClientCandidate
import tech.dokus.features.cashflow.mvi.model.InvoiceResolvedAction
import tech.dokus.features.cashflow.mvi.model.InvoiceSection
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.InvoiceLineItemsSection
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.Mocks
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop.DesktopCreateInvoiceWorkspace
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop.InvoiceClientLookup
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop.formatDate
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop.primaryActionLabel
import tech.dokus.features.cashflow.presentation.review.components.CanonicalInvoiceDocumentCard
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PDatePickerDialog
import tech.dokus.foundation.aura.components.common.PSelectableCommandCard
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.layout.PCollapsibleSection
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val PreviewDocumentMinHeight = 620.dp

@Composable
internal fun CreateInvoiceScreen(
    state: CreateInvoiceState,
    snackbarHostState: SnackbarHostState,
    onIntent: (CreateInvoiceIntent) -> Unit
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!isLargeScreen) {
                MobileCommandFooter(
                    primaryLabel = primaryActionLabel(state.uiState.resolvedDeliveryAction.action),
                    isSaving = state.formState.isSaving,
                    onSaveDraft = { onIntent(CreateInvoiceIntent.SaveAsDraft) },
                    onPrimary = { onIntent(CreateInvoiceIntent.SubmitWithResolvedDelivery) }
                )
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreen) {
                DesktopCreateInvoiceWorkspace(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.padding(contentPadding)
                )
            } else {
                MobileCreateInvoiceContent(
                    state = state,
                    onIntent = onIntent,
                    modifier = Modifier.padding(contentPadding)
                )
            }

            if (state.uiState.isDatePickerOpen != null) {
                val initialDate = when (state.uiState.isDatePickerOpen) {
                    DatePickerTarget.IssueDate -> state.formState.issueDate
                    DatePickerTarget.DueDate -> state.formState.dueDate
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

            if (state.uiState.isPreviewVisible) {
                InvoicePreviewDialog(
                    state = state,
                    onDismiss = { onIntent(CreateInvoiceIntent.SetPreviewVisible(false)) }
                )
            }
        }
    }
}

@Composable
private fun MobileCreateInvoiceContent(
    state: CreateInvoiceState,
    onIntent: (CreateInvoiceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val formState = state.formState
    val uiState = state.uiState

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "New Invoice",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold
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
            InvoiceClientLookup(
                lookupState = uiState.clientLookupState,
                onIntent = onIntent
            )
        }

        AccordionSection(
            title = "Line items",
            section = InvoiceSection.LineItems,
            state = uiState,
            onIntent = onIntent
        ) {
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
            PTextFieldStandard(
                fieldName = "Bank account",
                value = formState.senderIban,
                onValueChange = { onIntent(CreateInvoiceIntent.UpdateSenderIban(it)) },
                modifier = Modifier.fillMaxWidth()
            )
            PTextFieldStandard(
                fieldName = "BIC",
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
                onValueChange = { onIntent(CreateInvoiceIntent.UpdateNotes(it)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false
            )

            PSelectableCommandCard(
                title = "PEPPOL",
                subtitle = "E-invoice to client",
                selected = uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.Peppol,
                onClick = { onIntent(CreateInvoiceIntent.SelectDeliveryPreference(InvoiceDeliveryMethod.Peppol)) },
                reason = if (
                    uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.Peppol &&
                        uiState.resolvedDeliveryAction.action == InvoiceResolvedAction.PdfExport
                ) {
                    uiState.resolvedDeliveryAction.reason
                } else {
                    null
                }
            )
            PSelectableCommandCard(
                title = "PDF",
                subtitle = "Download locally",
                selected = uiState.selectedDeliveryPreference == InvoiceDeliveryMethod.PdfExport,
                onClick = { onIntent(CreateInvoiceIntent.SelectDeliveryPreference(InvoiceDeliveryMethod.PdfExport)) }
            )
        }

        AccordionSection(
            title = "Dates & terms",
            section = InvoiceSection.DatesTerms,
            state = uiState,
            onIntent = onIntent
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DateLabel("Issue", formatDate(formState.issueDate)) {
                    onIntent(CreateInvoiceIntent.OpenIssueDatePicker)
                }
                DateLabel("Due", formatDate(formState.dueDate)) {
                    onIntent(CreateInvoiceIntent.OpenDueDatePicker)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PButton(
                    text = "Terms",
                    variant = if (formState.dueDateMode == InvoiceDueDateMode.Terms) PButtonVariant.Default else PButtonVariant.Outline,
                    onClick = { onIntent(CreateInvoiceIntent.UpdateDueDateMode(InvoiceDueDateMode.Terms)) }
                )
                PButton(
                    text = "Fixed date",
                    variant = if (formState.dueDateMode == InvoiceDueDateMode.FixedDate) PButtonVariant.Default else PButtonVariant.Outline,
                    onClick = { onIntent(CreateInvoiceIntent.UpdateDueDateMode(InvoiceDueDateMode.FixedDate)) }
                )
            }
            PTextFieldStandard(
                fieldName = "Payment terms (days)",
                value = formState.paymentTermsDays.toString(),
                onValueChange = {
                    it.toIntOrNull()?.let { days ->
                        onIntent(CreateInvoiceIntent.UpdatePaymentTermsDays(days))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        PButton(
            text = "Preview",
            variant = PButtonVariant.Outline,
            onClick = { onIntent(CreateInvoiceIntent.SetPreviewVisible(true)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 90.dp)
        )
    }
}

@Composable
private fun DateLabel(label: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                content()
            }
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
    val selectedClient = state.formState.selectedClient
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.width(Constraints.DocumentDetail.previewMaxWidth),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Invoice preview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                PButton(
                    text = "Close",
                    variant = PButtonVariant.Outline,
                    onClick = onDismiss
                )
            }

            CanonicalInvoiceDocumentCard(
                draft = state.toInvoicePreviewDraft(),
                counterpartyName = selectedClient?.name?.value ?: "Click to select a client",
                counterpartyAddress = selectedClient?.toPreviewAddressLine(),
                modifier = Modifier
                    .width(Constraints.DocumentDetail.previewMaxWidth)
                    .heightIn(min = PreviewDocumentMinHeight)
            )
        }
    }
}

@Preview(name = "CreateInvoice Desktop Base", widthDp = 1200, heightDp = 1300)
@Composable
private fun CreateInvoiceDesktopBasePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = createDesktopPreviewState(),
            snackbarHostState = SnackbarHostState(),
            onIntent = {}
        )
    }
}

@Preview(name = "CreateInvoice Desktop Lookup Local", widthDp = 1200, heightDp = 1300)
@Composable
private fun CreateInvoiceDesktopLookupLocalPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val localSuggestion = ClientSuggestion.LocalContact(Mocks.sampleClient)
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = createDesktopPreviewState(
                lookupQuery = "Acme",
                lookupExpanded = true,
                suggestions = listOf(localSuggestion)
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {}
        )
    }
}

@Preview(name = "CreateInvoice Desktop Lookup Mixed", widthDp = 1200, heightDp = 1300)
@Composable
private fun CreateInvoiceDesktopLookupMixedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val localSuggestion = ClientSuggestion.LocalContact(Mocks.sampleClient)
    val externalSuggestion = ClientSuggestion.ExternalCompany(
        ExternalClientCandidate(
            name = "Colruyt Group NV",
            vatNumber = VatNumber("BE0400378485"),
            enterpriseNumber = "0400378485",
            prefillAddress = "Edingensesteenweg 196, 1500 Halle"
        )
    )
    val manualSuggestion = ClientSuggestion.CreateManual("Col")
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = createDesktopPreviewState(
                lookupQuery = "Col",
                lookupExpanded = true,
                suggestions = listOf(localSuggestion, externalSuggestion, manualSuggestion)
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {}
        )
    }
}

@Preview(name = "CreateInvoice Desktop Delivery Fallback", widthDp = 1200, heightDp = 1300)
@Composable
private fun CreateInvoiceDesktopDeliveryFallbackPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = createDesktopPreviewState(
                selectedDelivery = InvoiceDeliveryMethod.Peppol,
                resolvedAction = DeliveryResolution(
                    action = InvoiceResolvedAction.PdfExport,
                    reason = "Client is not PEPPOL-eligible."
                )
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {}
        )
    }
}

@Preview(name = "CreateInvoice Desktop PDF Primary", widthDp = 1200, heightDp = 1300)
@Composable
private fun CreateInvoiceDesktopPdfPrimaryPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = createDesktopPreviewState(
                selectedDelivery = InvoiceDeliveryMethod.PdfExport,
                resolvedAction = DeliveryResolution(action = InvoiceResolvedAction.PdfExport)
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {}
        )
    }
}

@Preview(name = "CreateInvoice Desktop Preview Dialog", widthDp = 1200, heightDp = 1300)
@Composable
private fun CreateInvoiceDesktopPreviewDialogPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = createDesktopPreviewState(previewVisible = true),
            snackbarHostState = SnackbarHostState(),
            onIntent = {}
        )
    }
}

@Preview(name = "CreateInvoice Desktop Preview Dialog Empty Client", widthDp = 1200, heightDp = 1300)
@Composable
private fun CreateInvoiceDesktopPreviewDialogNoClientPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CreateInvoiceScreen(
            state = createDesktopPreviewState(
                previewVisible = true,
                clearSelectedClient = true
            ),
            snackbarHostState = SnackbarHostState(),
            onIntent = {}
        )
    }
}

private fun createDesktopPreviewState(
    lookupQuery: String = "",
    lookupExpanded: Boolean = false,
    suggestions: List<ClientSuggestion> = emptyList(),
    selectedDelivery: InvoiceDeliveryMethod = InvoiceDeliveryMethod.Peppol,
    resolvedAction: DeliveryResolution = DeliveryResolution(InvoiceResolvedAction.Peppol),
    previewVisible: Boolean = false,
    clearSelectedClient: Boolean = false
): CreateInvoiceState {
    return CreateInvoiceState(
        formState = Mocks.sampleFormState.copy(
            selectedClient = if (clearSelectedClient) null else Mocks.sampleFormState.selectedClient,
            notes = "Payment within 30 days of invoice date. Late payments incur interest at 8% per annum plus €40 recovery costs per Belgian law.",
            senderIban = "BE68 5390 0754 7034",
            senderBic = "TRIOBEBB",
            structuredCommunication = "+++315/838/230/22+++"
        ),
        uiState = Mocks.sampleUiState.copy(
            clientLookupState = Mocks.sampleUiState.clientLookupState.copy(
                query = lookupQuery,
                isExpanded = lookupExpanded,
                mergedSuggestions = suggestions
            ),
            senderCompanyName = "INVOID VISION",
            senderCompanyVat = "BE0777.887.045",
            selectedDeliveryPreference = selectedDelivery,
            resolvedDeliveryAction = resolvedAction,
            isPreviewVisible = previewVisible
        ),
        invoiceNumberPreview = "INV-2026-0003"
    )
}

private fun CreateInvoiceState.toInvoicePreviewDraft(): InvoiceDraftData {
    val lines = formState.items
        .filterNot { it.isEmpty }
        .map { line ->
            FinancialLineItem(
                description = line.description,
                quantity = line.quantity.toLong().takeIf { it > 0L },
                unitPrice = Money.fromDouble(line.unitPriceDouble).minor.takeIf { it > 0L },
                vatRate = line.vatRatePercent,
                netAmount = Money.fromDouble(line.lineTotalDouble).minor.takeIf { it > 0L }
            )
        }

    val selectedClient = formState.selectedClient
    val sellerVat = uiState.senderCompanyVat
        ?.let(VatNumber::from)
        ?.takeIf { it.isValid }

    return InvoiceDraftData(
        direction = DocumentDirection.Outbound,
        invoiceNumber = invoiceNumberPreview,
        issueDate = formState.issueDate,
        dueDate = formState.dueDate,
        subtotalAmount = Money.fromDouble(formState.items.sumOf { it.lineTotalDouble }),
        vatAmount = Money.fromDouble(formState.items.sumOf { it.vatAmountDouble }),
        totalAmount = Money.fromDouble(formState.items.sumOf { it.lineTotalDouble + it.vatAmountDouble }),
        lineItems = lines,
        iban = Iban.from(formState.senderIban),
        notes = formState.notes.takeIf { it.isNotBlank() },
        seller = PartyDraft(
            name = uiState.senderCompanyName.takeIf { it.isNotBlank() },
            vat = sellerVat
        ),
        buyer = selectedClient?.toPartyDraft() ?: PartyDraft()
    )
}

private fun ContactDto.toPartyDraft(): PartyDraft {
    return PartyDraft(
        name = name.value,
        vat = vatNumber,
        email = email,
        iban = iban,
        streetLine1 = addressLine1,
        streetLine2 = addressLine2,
        postalCode = postalCode,
        city = city,
        country = country
    )
}

private fun ContactDto.toPreviewAddressLine(): String? {
    return listOfNotNull(
        addressLine1?.takeIf { it.isNotBlank() },
        addressLine2?.takeIf { it.isNotBlank() },
        listOfNotNull(
            postalCode?.takeIf { it.isNotBlank() },
            city?.takeIf { it.isNotBlank() }
        ).takeIf { it.isNotEmpty() }?.joinToString(" "),
        country?.takeIf { it.isNotBlank() }
    ).takeIf { it.isNotEmpty() }?.joinToString(", ")
}
