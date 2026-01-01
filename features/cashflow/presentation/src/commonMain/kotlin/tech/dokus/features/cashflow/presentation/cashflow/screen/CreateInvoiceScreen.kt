package tech.dokus.features.cashflow.presentation.cashflow.screen

import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.ContactSelectionPanel
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.DesktopInvoiceLayout
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.InteractiveInvoiceDocument
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.InvoiceSendOptionsPanel
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.InvoiceSendOptionsStep
import tech.dokus.features.cashflow.presentation.cashflow.components.invoice.MobileInvoiceEditLayout
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.CreateInvoiceState
import tech.dokus.features.cashflow.mvi.model.DatePickerTarget
import tech.dokus.features.cashflow.mvi.model.InvoiceCreationStep
import tech.dokus.foundation.aura.components.PDatePickerDialog
import tech.dokus.foundation.aura.local.LocalScreenSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus

/**
 * Screen for creating a new invoice using an interactive WYSIWYG editor.
 *
 * Desktop: Two-column layout with interactive invoice on left, send options on right.
 * Mobile: Two-step flow - edit invoice, then send options.
 */
@Composable
internal fun CreateInvoiceScreen(
    state: CreateInvoiceState,
    onIntent: (CreateInvoiceIntent) -> Unit,
    onNavigateToCreateContact: () -> Unit
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    val formState = state.formState
    val uiState = state.uiState
    val invoiceNumberPreview = (state as? CreateInvoiceState.Editing)?.invoiceNumberPreview

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreen) {
                DesktopInvoiceLayout(
                    contentPadding = contentPadding,
                    invoiceNumberPreview = invoiceNumberPreview,
                    onBackPress = { onIntent(CreateInvoiceIntent.BackClicked) },
                    invoiceContent = {
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
                            onUpdateItemDescription = { id, desc -> onIntent(CreateInvoiceIntent.UpdateItemDescription(id, desc)) },
                            onUpdateItemQuantity = { id, qty -> onIntent(CreateInvoiceIntent.UpdateItemQuantity(id, qty)) },
                            onUpdateItemUnitPrice = { id, price -> onIntent(CreateInvoiceIntent.UpdateItemUnitPrice(id, price)) },
                            onUpdateItemVatRate = { id, rate -> onIntent(CreateInvoiceIntent.UpdateItemVatRate(id, rate)) }
                        )
                    },
                    sendOptionsContent = {
                        InvoiceSendOptionsPanel(
                            formState = formState,
                            selectedMethod = uiState.selectedDeliveryMethod,
                            onMethodSelected = { onIntent(CreateInvoiceIntent.SelectDeliveryMethod(it)) },
                            onSaveAsDraft = { onIntent(CreateInvoiceIntent.SaveAsDraft) },
                            isSaving = formState.isSaving
                        )
                    }
                )

                // Client selection side panel with ContactAutocomplete
                ContactSelectionPanel(
                    isVisible = uiState.isClientPanelOpen,
                    onDismiss = { onIntent(CreateInvoiceIntent.CloseClientPanel) },
                    selectedContact = formState.selectedClient,
                    searchQuery = uiState.clientSearchQuery,
                    onSearchQueryChange = { onIntent(CreateInvoiceIntent.UpdateClientSearchQuery(it)) },
                    onContactSelected = { autoFillData ->
                        // Select the contact
                        onIntent(CreateInvoiceIntent.SelectClient(autoFillData.contact))

                        // Auto-fill due date from payment terms if available
                        val paymentTerms = autoFillData.defaultPaymentTerms
                        if (paymentTerms > 0) {
                            formState.issueDate?.let { issueDate ->
                                val newDueDate = issueDate.plus(paymentTerms, DateTimeUnit.DAY)
                                onIntent(CreateInvoiceIntent.UpdateDueDate(newDueDate))
                            }
                        }

                        // Auto-fill VAT rate for first item if contact has default VAT rate
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
            } else {
                // Mobile: Two-step flow
                when (uiState.currentStep) {
                    InvoiceCreationStep.EDIT_INVOICE -> {
                        MobileInvoiceEditLayout(
                            contentPadding = contentPadding,
                            invoiceNumberPreview = invoiceNumberPreview,
                            onBackPress = { onIntent(CreateInvoiceIntent.BackClicked) },
                            invoiceContent = {
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
                                    onUpdateItemDescription = { id, desc -> onIntent(CreateInvoiceIntent.UpdateItemDescription(id, desc)) },
                                    onUpdateItemQuantity = { id, qty -> onIntent(CreateInvoiceIntent.UpdateItemQuantity(id, qty)) },
                                    onUpdateItemUnitPrice = { id, price -> onIntent(CreateInvoiceIntent.UpdateItemUnitPrice(id, price)) },
                                    onUpdateItemVatRate = { id, rate -> onIntent(CreateInvoiceIntent.UpdateItemVatRate(id, rate)) }
                                )
                            },
                            onNextClick = { onIntent(CreateInvoiceIntent.GoToSendOptions) },
                            isNextEnabled = formState.isValid
                        )

                        // Client selection side panel with ContactAutocomplete
                        ContactSelectionPanel(
                            isVisible = uiState.isClientPanelOpen,
                            onDismiss = { onIntent(CreateInvoiceIntent.CloseClientPanel) },
                            selectedContact = formState.selectedClient,
                            searchQuery = uiState.clientSearchQuery,
                            onSearchQueryChange = { onIntent(CreateInvoiceIntent.UpdateClientSearchQuery(it)) },
                            onContactSelected = { autoFillData ->
                                // Select the contact
                                onIntent(CreateInvoiceIntent.SelectClient(autoFillData.contact))

                                // Auto-fill due date from payment terms if available
                                val paymentTerms = autoFillData.defaultPaymentTerms
                                if (paymentTerms > 0) {
                                    formState.issueDate?.let { issueDate ->
                                        val newDueDate =
                                            issueDate.plus(paymentTerms, DateTimeUnit.DAY)
                                        onIntent(CreateInvoiceIntent.UpdateDueDate(newDueDate))
                                    }
                                }

                                // Auto-fill VAT rate for first item if contact has default VAT rate
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
                    }

                    InvoiceCreationStep.SEND_OPTIONS -> {
                        InvoiceSendOptionsStep(
                            formState = formState,
                            selectedMethod = uiState.selectedDeliveryMethod,
                            onMethodSelected = { onIntent(CreateInvoiceIntent.SelectDeliveryMethod(it)) },
                            onBackToEdit = { onIntent(CreateInvoiceIntent.GoBackToEdit) },
                            onSaveAsDraft = { onIntent(CreateInvoiceIntent.SaveAsDraft) },
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
        }
    }
}
