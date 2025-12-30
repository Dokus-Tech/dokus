package ai.dokus.app.cashflow.screens

import ai.dokus.app.cashflow.components.invoice.InteractiveInvoiceDocument
import ai.dokus.app.cashflow.components.invoice.InvoiceSendOptionsPanel
import ai.dokus.app.cashflow.components.invoice.InvoiceSendOptionsStep
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceAction
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceContainer
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceIntent
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceState
import ai.dokus.app.cashflow.viewmodel.model.DatePickerTarget
import ai.dokus.app.cashflow.viewmodel.model.InvoiceCreationStep
import tech.dokus.contacts.components.ContactAutoFillData
import tech.dokus.contacts.components.ContactAutocomplete
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.action_next
import tech.dokus.aura.resources.cashflow_create_invoice
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.invoice_contact_search_help
import tech.dokus.aura.resources.invoice_contact_search_label
import tech.dokus.aura.resources.invoice_contact_search_placeholder
import tech.dokus.aura.resources.invoice_edit_hint_desktop
import tech.dokus.aura.resources.invoice_edit_hint_mobile
import tech.dokus.aura.resources.invoice_number_preview
import tech.dokus.aura.resources.invoice_select_client
import tech.dokus.aura.resources.invoice_selected_contact
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PDatePickerDialog
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.local.LocalScreenSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.plus
import pro.respawn.flowmvi.api.IntentReceiver
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.mvi.container

/**
 * Screen for creating a new invoice using an interactive WYSIWYG editor.
 *
 * Desktop: Two-column layout with interactive invoice on left, send options on right.
 * Mobile: Two-step flow - edit invoice, then send options.
 */
@Composable
internal fun CreateInvoiceScreen(
    container: CreateInvoiceContainer = container(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.current.isLarge

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is CreateInvoiceAction.NavigateBack -> navController.popBackStack()
            is CreateInvoiceAction.NavigateToCreateContact -> {
                navController.navigate(ContactsDestination.CreateContact)
            }
            is CreateInvoiceAction.NavigateToInvoice -> {
                // Invoice created, navigate back
                navController.popBackStack()
            }
            is CreateInvoiceAction.ShowValidationError -> {
                // Could show a snackbar, for now handled via form state errors
            }
            CreateInvoiceAction.ShowSuccess -> {
                // Could show a success snackbar
            }
            is CreateInvoiceAction.ShowError -> {
                // Could show an error snackbar
            }
        }
    }

    val formState = state.formState
    val uiState = state.uiState
    val invoiceNumberPreview = (state as? CreateInvoiceState.Editing)?.invoiceNumberPreview

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLargeScreen) {
                with(container.store) {
                    DesktopLayout(
                        contentPadding = contentPadding,
                        invoiceNumberPreview = invoiceNumberPreview,
                        onBackPress = { intent(CreateInvoiceIntent.BackClicked) },
                        invoiceContent = {
                            InteractiveInvoiceDocument(
                                formState = formState,
                                uiState = uiState,
                                onClientClick = { intent(CreateInvoiceIntent.OpenClientPanel) },
                                onIssueDateClick = { intent(CreateInvoiceIntent.OpenIssueDatePicker) },
                                onDueDateClick = { intent(CreateInvoiceIntent.OpenDueDatePicker) },
                                onItemClick = { intent(CreateInvoiceIntent.ExpandItem(it)) },
                                onItemCollapse = { intent(CreateInvoiceIntent.CollapseItem) },
                                onAddItem = { intent(CreateInvoiceIntent.AddLineItem) },
                                onRemoveItem = { intent(CreateInvoiceIntent.RemoveLineItem(it)) },
                                onUpdateItemDescription = { id, desc -> intent(CreateInvoiceIntent.UpdateItemDescription(id, desc)) },
                                onUpdateItemQuantity = { id, qty -> intent(CreateInvoiceIntent.UpdateItemQuantity(id, qty)) },
                                onUpdateItemUnitPrice = { id, price -> intent(CreateInvoiceIntent.UpdateItemUnitPrice(id, price)) },
                                onUpdateItemVatRate = { id, rate -> intent(CreateInvoiceIntent.UpdateItemVatRate(id, rate)) }
                            )
                        },
                        sendOptionsContent = {
                            InvoiceSendOptionsPanel(
                                formState = formState,
                                selectedMethod = uiState.selectedDeliveryMethod,
                                onMethodSelected = { intent(CreateInvoiceIntent.SelectDeliveryMethod(it)) },
                                onSaveAsDraft = { intent(CreateInvoiceIntent.SaveAsDraft) },
                                isSaving = formState.isSaving
                            )
                        }
                    )

                    // Client selection side panel with ContactAutocomplete
                    ContactSelectionPanel(
                        isVisible = uiState.isClientPanelOpen,
                        onDismiss = { intent(CreateInvoiceIntent.CloseClientPanel) },
                        selectedContact = formState.selectedClient,
                        searchQuery = uiState.clientSearchQuery,
                        onSearchQueryChange = { intent(CreateInvoiceIntent.UpdateClientSearchQuery(it)) },
                        onContactSelected = { autoFillData ->
                            // Select the contact
                            intent(CreateInvoiceIntent.SelectClient(autoFillData.contact))

                            // Auto-fill due date from payment terms if available
                            val paymentTerms = autoFillData.defaultPaymentTerms
                            if (paymentTerms > 0) {
                                formState.issueDate?.let { issueDate ->
                                    val newDueDate = issueDate.plus(paymentTerms, DateTimeUnit.DAY)
                                    intent(CreateInvoiceIntent.UpdateDueDate(newDueDate))
                                }
                            }

                            // Auto-fill VAT rate for first item if contact has default VAT rate
                            autoFillData.defaultVatRate?.toIntOrNull()?.let { vatRate ->
                                formState.items.firstOrNull()?.let { firstItem ->
                                    intent(CreateInvoiceIntent.UpdateItemVatRate(firstItem.id, vatRate))
                                }
                            }
                        },
                        onAddNewContact = {
                            intent(CreateInvoiceIntent.CloseClientPanel)
                            navController.navigate(ContactsDestination.CreateContact)
                        }
                    )
                }
            } else {
                with(container.store) {
                    // Mobile: Two-step flow
                    when (uiState.currentStep) {
                        InvoiceCreationStep.EDIT_INVOICE -> {
                            MobileEditLayout(
                                contentPadding = contentPadding,
                                invoiceNumberPreview = invoiceNumberPreview,
                                onBackPress = { intent(CreateInvoiceIntent.BackClicked) },
                                invoiceContent = {
                                    InteractiveInvoiceDocument(
                                        formState = formState,
                                        uiState = uiState,
                                        onClientClick = { intent(CreateInvoiceIntent.OpenClientPanel) },
                                        onIssueDateClick = { intent(CreateInvoiceIntent.OpenIssueDatePicker) },
                                        onDueDateClick = { intent(CreateInvoiceIntent.OpenDueDatePicker) },
                                        onItemClick = { intent(CreateInvoiceIntent.ExpandItem(it)) },
                                        onItemCollapse = { intent(CreateInvoiceIntent.CollapseItem) },
                                        onAddItem = { intent(CreateInvoiceIntent.AddLineItem) },
                                        onRemoveItem = { intent(CreateInvoiceIntent.RemoveLineItem(it)) },
                                        onUpdateItemDescription = { id, desc -> intent(CreateInvoiceIntent.UpdateItemDescription(id, desc)) },
                                        onUpdateItemQuantity = { id, qty -> intent(CreateInvoiceIntent.UpdateItemQuantity(id, qty)) },
                                        onUpdateItemUnitPrice = { id, price -> intent(CreateInvoiceIntent.UpdateItemUnitPrice(id, price)) },
                                        onUpdateItemVatRate = { id, rate -> intent(CreateInvoiceIntent.UpdateItemVatRate(id, rate)) }
                                    )
                                },
                                onNextClick = { intent(CreateInvoiceIntent.GoToSendOptions) },
                                isNextEnabled = formState.isValid
                            )

                            // Client selection side panel with ContactAutocomplete
                            ContactSelectionPanel(
                                isVisible = uiState.isClientPanelOpen,
                                onDismiss = { intent(CreateInvoiceIntent.CloseClientPanel) },
                                selectedContact = formState.selectedClient,
                                searchQuery = uiState.clientSearchQuery,
                                onSearchQueryChange = { intent(CreateInvoiceIntent.UpdateClientSearchQuery(it)) },
                                onContactSelected = { autoFillData ->
                                    // Select the contact
                                    intent(CreateInvoiceIntent.SelectClient(autoFillData.contact))

                                    // Auto-fill due date from payment terms if available
                                    val paymentTerms = autoFillData.defaultPaymentTerms
                                    if (paymentTerms > 0) {
                                        formState.issueDate?.let { issueDate ->
                                            val newDueDate =
                                                issueDate.plus(paymentTerms, DateTimeUnit.DAY)
                                            intent(CreateInvoiceIntent.UpdateDueDate(newDueDate))
                                        }
                                    }

                                    // Auto-fill VAT rate for first item if contact has default VAT rate
                                    autoFillData.defaultVatRate?.toIntOrNull()?.let { vatRate ->
                                        formState.items.firstOrNull()?.let { firstItem ->
                                            intent(CreateInvoiceIntent.UpdateItemVatRate(firstItem.id, vatRate))
                                        }
                                    }
                                },
                                onAddNewContact = {
                                    intent(CreateInvoiceIntent.CloseClientPanel)
                                    navController.navigate(ContactsDestination.CreateContact)
                                }
                            )
                        }

                        InvoiceCreationStep.SEND_OPTIONS -> {
                            InvoiceSendOptionsStep(
                                formState = formState,
                                selectedMethod = uiState.selectedDeliveryMethod,
                                onMethodSelected = { intent(CreateInvoiceIntent.SelectDeliveryMethod(it)) },
                                onBackToEdit = { intent(CreateInvoiceIntent.GoBackToEdit) },
                                onSaveAsDraft = { intent(CreateInvoiceIntent.SaveAsDraft) },
                                isSaving = formState.isSaving,
                                modifier = Modifier.padding(contentPadding)
                            )
                        }
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
                            container.store.intent(CreateInvoiceIntent.SelectDate(date))
                        } else {
                            container.store.intent(CreateInvoiceIntent.CloseDatePicker)
                        }
                    },
                    onDismiss = { container.store.intent(CreateInvoiceIntent.CloseDatePicker) }
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
                text = stringResource(Res.string.cashflow_create_invoice),
                onBackPress = onBackPress
            )
            if (invoiceNumberPreview != null) {
                Text(
                    text = stringResource(Res.string.invoice_number_preview, invoiceNumberPreview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(Res.string.invoice_edit_hint_desktop),
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
                text = stringResource(Res.string.cashflow_create_invoice),
                onBackPress = onBackPress
            )
            if (invoiceNumberPreview != null) {
                Text(
                    text = stringResource(Res.string.invoice_number_preview, invoiceNumberPreview),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = stringResource(Res.string.invoice_edit_hint_mobile),
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
                text = stringResource(Res.string.action_next),
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
private fun IntentReceiver<CreateInvoiceIntent>.ContactSelectionPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    selectedContact: ContactDto?,
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
                                text = stringResource(Res.string.invoice_select_client),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.action_close),
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
                            placeholder = stringResource(Res.string.invoice_contact_search_placeholder),
                            label = stringResource(Res.string.invoice_contact_search_label),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

                        // Help text
                        Text(
                            text = stringResource(Res.string.invoice_contact_search_help),
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
                                        text = stringResource(Res.string.invoice_selected_contact),
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
                                            text = stringResource(Res.string.common_vat_value, vat.value),
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
