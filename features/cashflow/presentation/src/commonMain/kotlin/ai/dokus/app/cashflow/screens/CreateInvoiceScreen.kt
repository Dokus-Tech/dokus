@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.invoice.InteractiveInvoiceDocument
import ai.dokus.app.cashflow.components.invoice.InvoiceClientSidePanel
import ai.dokus.app.cashflow.components.invoice.InvoiceSendOptionsPanel
import ai.dokus.app.cashflow.components.invoice.InvoiceSendOptionsStep
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceViewModel
import ai.dokus.app.cashflow.viewmodel.DatePickerTarget
import ai.dokus.app.cashflow.viewmodel.InvoiceCreationStep
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.navigation.local.LocalNavController
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen for creating a new invoice using an interactive WYSIWYG editor.
 *
 * Desktop: Two-column layout with interactive invoice on left, send options on right.
 * Mobile: Two-step flow - edit invoice, then send options.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateInvoiceScreen(
    viewModel: CreateInvoiceViewModel = koinViewModel(),
    onInvoiceCreated: () -> Unit = {}
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.current.isLarge

    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val clientsState by viewModel.clientsState.collectAsState()
    val saveState by viewModel.state.collectAsState()
    val createdInvoiceId by viewModel.createdInvoiceId.collectAsState()

    // Navigate back when invoice is created
    LaunchedEffect(createdInvoiceId) {
        if (createdInvoiceId != null) {
            onInvoiceCreated()
            navController.popBackStack()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreen) {
                DesktopLayout(
                    contentPadding = contentPadding,
                    onBackPress = { navController.popBackStack() },
                    invoiceContent = {
                        InteractiveInvoiceDocument(
                            formState = formState,
                            uiState = uiState,
                            onClientClick = viewModel::openClientPanel,
                            onIssueDateClick = viewModel::openIssueDatePicker,
                            onDueDateClick = viewModel::openDueDatePicker,
                            onItemClick = viewModel::expandItem,
                            onItemCollapse = viewModel::collapseItem,
                            onAddItem = { viewModel.addLineItem() },
                            onRemoveItem = viewModel::removeLineItem,
                            onUpdateItemDescription = viewModel::updateItemDescription,
                            onUpdateItemQuantity = viewModel::updateItemQuantity,
                            onUpdateItemUnitPrice = viewModel::updateItemUnitPrice,
                            onUpdateItemVatRate = viewModel::updateItemVatRate
                        )
                    },
                    sendOptionsContent = {
                        InvoiceSendOptionsPanel(
                            formState = formState,
                            selectedMethod = uiState.selectedDeliveryMethod,
                            onMethodSelected = viewModel::selectDeliveryMethod,
                            onSaveAsDraft = viewModel::saveAsDraft,
                            isSaving = formState.isSaving
                        )
                    }
                )

                // Client selection side panel
                InvoiceClientSidePanel(
                    isVisible = uiState.isClientPanelOpen,
                    onDismiss = viewModel::closeClientPanel,
                    clientsState = clientsState,
                    selectedClient = formState.selectedClient,
                    searchQuery = uiState.clientSearchQuery,
                    onSearchQueryChange = viewModel::updateClientSearchQuery,
                    onSelectClient = viewModel::selectClientAndClose
                )
            } else {
                // Mobile: Two-step flow
                when (uiState.currentStep) {
                    InvoiceCreationStep.EDIT_INVOICE -> {
                        MobileEditLayout(
                            contentPadding = contentPadding,
                            onBackPress = { navController.popBackStack() },
                            invoiceContent = {
                                InteractiveInvoiceDocument(
                                    formState = formState,
                                    uiState = uiState,
                                    onClientClick = viewModel::openClientPanel,
                                    onIssueDateClick = viewModel::openIssueDatePicker,
                                    onDueDateClick = viewModel::openDueDatePicker,
                                    onItemClick = viewModel::expandItem,
                                    onItemCollapse = viewModel::collapseItem,
                                    onAddItem = { viewModel.addLineItem() },
                                    onRemoveItem = viewModel::removeLineItem,
                                    onUpdateItemDescription = viewModel::updateItemDescription,
                                    onUpdateItemQuantity = viewModel::updateItemQuantity,
                                    onUpdateItemUnitPrice = viewModel::updateItemUnitPrice,
                                    onUpdateItemVatRate = viewModel::updateItemVatRate
                                )
                            },
                            onNextClick = viewModel::goToSendOptions,
                            isNextEnabled = formState.isValid
                        )

                        // Client selection side panel
                        InvoiceClientSidePanel(
                            isVisible = uiState.isClientPanelOpen,
                            onDismiss = viewModel::closeClientPanel,
                            clientsState = clientsState,
                            selectedClient = formState.selectedClient,
                            searchQuery = uiState.clientSearchQuery,
                            onSearchQueryChange = viewModel::updateClientSearchQuery,
                            onSelectClient = viewModel::selectClientAndClose
                        )
                    }

                    InvoiceCreationStep.SEND_OPTIONS -> {
                        InvoiceSendOptionsStep(
                            formState = formState,
                            selectedMethod = uiState.selectedDeliveryMethod,
                            onMethodSelected = viewModel::selectDeliveryMethod,
                            onBackToEdit = viewModel::goBackToEditInvoice,
                            onSaveAsDraft = viewModel::saveAsDraft,
                            isSaving = formState.isSaving,
                            modifier = Modifier.padding(contentPadding)
                        )
                    }
                }
            }

            // Date picker dialog
            if (uiState.isDatePickerOpen != null) {
                val initialDate = when (uiState.isDatePickerOpen) {
                    DatePickerTarget.ISSUE_DATE -> formState.issueDate
                    DatePickerTarget.DUE_DATE -> formState.dueDate
                    null -> null
                }

                DatePickerModal(
                    initialDate = initialDate,
                    onDateSelected = { date ->
                        if (date != null) {
                            viewModel.selectDate(date)
                        } else {
                            viewModel.closeDatePicker()
                        }
                    },
                    onDismiss = viewModel::closeDatePicker
                )
            }
        }
    }
}

@Composable
private fun DesktopLayout(
    contentPadding: PaddingValues,
    onBackPress: () -> Unit,
    invoiceContent: @Composable () -> Unit,
    sendOptionsContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(32.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Left column: Interactive invoice
        Column(
            modifier = Modifier
                .weight(1.5f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle(
                text = "Create Invoice",
                onBackPress = onBackPress
            )
            Text(
                text = "Click on any element to edit it. The invoice updates in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            invoiceContent()
        }

        // Right column: Send options
        Column(
            modifier = Modifier
                .width(320.dp)
                .verticalScroll(rememberScrollState())
        ) {
            sendOptionsContent()
        }
    }
}

@Composable
private fun MobileEditLayout(
    contentPadding: PaddingValues,
    onBackPress: () -> Unit,
    invoiceContent: @Composable () -> Unit,
    onNextClick: () -> Unit,
    isNextEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(
                text = "Create Invoice",
                onBackPress = onBackPress
            )
            Text(
                text = "Tap any element to edit it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            invoiceContent()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bottom action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            PButton(
                text = "Next",
                variant = PButtonVariant.Default,
                onClick = onNextClick,
                isEnabled = isNextEnabled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = initialDate?.let {
        it.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selectedMillis = datePickerState.selectedDateMillis
                if (selectedMillis != null) {
                    val instant = kotlin.time.Instant.fromEpochMilliseconds(selectedMillis)
                    val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                    onDateSelected(localDate)
                } else {
                    onDateSelected(null)
                }
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
