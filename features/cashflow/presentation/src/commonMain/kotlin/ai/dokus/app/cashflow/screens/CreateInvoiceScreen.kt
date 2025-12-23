package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.invoice.InteractiveInvoiceDocument
import ai.dokus.app.cashflow.components.invoice.InvoiceSendOptionsPanel
import ai.dokus.app.cashflow.components.invoice.InvoiceSendOptionsStep
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceViewModel
import ai.dokus.app.cashflow.viewmodel.model.DatePickerTarget
import ai.dokus.app.cashflow.viewmodel.model.InvoiceCreationStep
import ai.dokus.app.contacts.components.ContactAutoFillData
import ai.dokus.app.contacts.components.ContactAutocomplete
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.PDatePickerDialog
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.constrains.Constrains
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.navigation.destinations.ContactsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import org.koin.compose.viewmodel.koinViewModel

/**
 * Screen for creating a new invoice using an interactive WYSIWYG editor.
 *
 * Desktop: Two-column layout with interactive invoice on left, send options on right.
 * Mobile: Two-step flow - edit invoice, then send options.
 */
@Composable
internal fun CreateInvoiceScreen(
    viewModel: CreateInvoiceViewModel = koinViewModel(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.current.isLarge

    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val createdInvoiceId by viewModel.createdInvoiceId.collectAsState()
    val invoiceNumberPreview by viewModel.invoiceNumberPreview.collectAsState()

    // Navigate back when invoice is created
    LaunchedEffect(createdInvoiceId) {
        if (createdInvoiceId != null) {
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
                    invoiceNumberPreview = invoiceNumberPreview,
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

                // Client selection side panel with ContactAutocomplete
                ContactSelectionPanel(
                    isVisible = uiState.isClientPanelOpen,
                    onDismiss = viewModel::closeClientPanel,
                    selectedContact = formState.selectedClient,
                    searchQuery = uiState.clientSearchQuery,
                    onSearchQueryChange = viewModel::updateClientSearchQuery,
                    onContactSelected = { autoFillData ->
                        // Select the contact
                        viewModel.selectClientAndClose(autoFillData.contact)

                        // Auto-fill due date from payment terms if available
                        val paymentTerms = autoFillData.defaultPaymentTerms
                        if (paymentTerms > 0) {
                            formState.issueDate?.let { issueDate ->
                                val newDueDate = issueDate.plus(paymentTerms, DateTimeUnit.DAY)
                                viewModel.updateDueDate(newDueDate)
                            }
                        }

                        // Auto-fill VAT rate for first item if contact has default VAT rate
                        autoFillData.defaultVatRate?.toIntOrNull()?.let { vatRate ->
                            formState.items.firstOrNull()?.let { firstItem ->
                                viewModel.updateItemVatRate(firstItem.id, vatRate)
                            }
                        }
                    },
                    onAddNewContact = {
                        viewModel.closeClientPanel()
                        navController.navigate(ContactsDestination.CreateContact)
                    }
                )
            } else {
                // Mobile: Two-step flow
                when (uiState.currentStep) {
                    InvoiceCreationStep.EDIT_INVOICE -> {
                        MobileEditLayout(
                            contentPadding = contentPadding,
                            invoiceNumberPreview = invoiceNumberPreview,
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

                        // Client selection side panel with ContactAutocomplete
                        ContactSelectionPanel(
                            isVisible = uiState.isClientPanelOpen,
                            onDismiss = viewModel::closeClientPanel,
                            selectedContact = formState.selectedClient,
                            searchQuery = uiState.clientSearchQuery,
                            onSearchQueryChange = viewModel::updateClientSearchQuery,
                            onContactSelected = { autoFillData ->
                                // Select the contact
                                viewModel.selectClientAndClose(autoFillData.contact)

                                // Auto-fill due date from payment terms if available
                                val paymentTerms = autoFillData.defaultPaymentTerms
                                if (paymentTerms > 0) {
                                    formState.issueDate?.let { issueDate ->
                                        val newDueDate =
                                            issueDate.plus(paymentTerms, DateTimeUnit.DAY)
                                        viewModel.updateDueDate(newDueDate)
                                    }
                                }

                                // Auto-fill VAT rate for first item if contact has default VAT rate
                                autoFillData.defaultVatRate?.toIntOrNull()?.let { vatRate ->
                                    formState.items.firstOrNull()?.let { firstItem ->
                                        viewModel.updateItemVatRate(firstItem.id, vatRate)
                                    }
                                }
                            },
                            onAddNewContact = {
                                viewModel.closeClientPanel()
                                navController.navigate(ContactsDestination.CreateContact)
                            }
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
                    else -> null
                }

                PDatePickerDialog(
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
    invoiceNumberPreview: String?,
    onBackPress: () -> Unit,
    invoiceContent: @Composable () -> Unit,
    sendOptionsContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left column: Interactive invoice
        Column(
            modifier = Modifier
                .weight(1.6f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(
                text = "Create Invoice",
                onBackPress = onBackPress
            )
            if (invoiceNumberPreview != null) {
                Text(
                    text = "Invoice #: $invoiceNumberPreview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Click on any element to edit it. The invoice updates in real-time.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            invoiceContent()
        }

        // Right column: Send options (weighted with min width)
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 320.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            sendOptionsContent()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MobileEditLayout(
    contentPadding: PaddingValues,
    invoiceNumberPreview: String?,
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
            if (invoiceNumberPreview != null) {
                Text(
                    text = "Invoice #: $invoiceNumberPreview",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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

/**
 * Side panel for selecting a contact with ContactAutocomplete.
 * Shows animated slide-in panel with autocomplete search functionality.
 * When a contact is selected, returns ContactAutoFillData for auto-filling invoice fields.
 */
@Composable
private fun ContactSelectionPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    selectedContact: ai.dokus.foundation.domain.model.ContactDto?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onContactSelected: (ContactAutoFillData) -> Unit,
    onAddNewContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Sidebar
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeIn(tween(300)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300)
            ) + fadeOut(tween(300)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BoxWithConstraints {
                val sidebarWidth = (maxWidth / 3).coerceIn(320.dp, 400.dp)

                Card(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume click to prevent backdrop dismissal */ }
                        ),
                    shape = MaterialTheme.shapes.large.copy(
                        topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                        bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Constrains.Spacing.medium)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Client",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

                        // Contact autocomplete search
                        ContactAutocomplete(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            selectedContact = selectedContact,
                            onContactSelected = { autoFillData ->
                                onContactSelected(autoFillData)
                            },
                            onAddNewContact = onAddNewContact,
                            placeholder = "Search by name, email, or VAT...",
                            label = "Search Contacts",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

                        // Help text
                        Text(
                            text = "Type to search contacts by name, email, or VAT number. Select a contact to auto-fill invoice details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Show selected contact info
                        if (selectedContact != null) {
                            Spacer(modifier = Modifier.height(Constrains.Spacing.large))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = 0.5f
                                    )
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(Constrains.Spacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
                                ) {
                                    Text(
                                        text = "Selected Contact",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = selectedContact.name.value,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    selectedContact.email?.let { email ->
                                        Text(
                                            text = email.value,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                    }
                                    selectedContact.vatNumber?.let { vat ->
                                        Text(
                                            text = "VAT: ${vat.value}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                                                alpha = 0.7f
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
