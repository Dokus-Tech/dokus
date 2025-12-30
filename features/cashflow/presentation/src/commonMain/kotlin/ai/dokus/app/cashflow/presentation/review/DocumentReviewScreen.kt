package ai.dokus.app.cashflow.presentation.review

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_collapse
import tech.dokus.aura.resources.action_confirm
import tech.dokus.aura.resources.action_discard
import tech.dokus.aura.resources.action_expand
import tech.dokus.aura.resources.action_reject
import tech.dokus.aura.resources.action_save
import tech.dokus.aura.resources.action_select
import tech.dokus.aura.resources.action_select_date
import tech.dokus.aura.resources.cashflow_bill_details_section
import tech.dokus.aura.resources.cashflow_chat_with_document
import tech.dokus.aura.resources.cashflow_client_information
import tech.dokus.aura.resources.cashflow_client_name
import tech.dokus.aura.resources.cashflow_confidence_badge
import tech.dokus.aura.resources.cashflow_deductible_percentage
import tech.dokus.aura.resources.cashflow_document_confirmed
import tech.dokus.aura.resources.cashflow_document_review_title
import tech.dokus.aura.resources.cashflow_draft_saved
import tech.dokus.aura.resources.cashflow_expense_details_section
import tech.dokus.aura.resources.cashflow_invoice_details_section
import tech.dokus.aura.resources.cashflow_invoice_number
import tech.dokus.aura.resources.cashflow_is_deductible
import tech.dokus.aura.resources.cashflow_loading_document
import tech.dokus.aura.resources.cashflow_merchant
import tech.dokus.aura.resources.cashflow_merchant_information
import tech.dokus.aura.resources.cashflow_payment_method
import tech.dokus.aura.resources.cashflow_receipt_number
import tech.dokus.aura.resources.cashflow_section_additional_information
import tech.dokus.aura.resources.cashflow_section_amounts
import tech.dokus.aura.resources.cashflow_select_category
import tech.dokus.aura.resources.cashflow_select_payment_method
import tech.dokus.aura.resources.cashflow_suggested_contacts
import tech.dokus.aura.resources.cashflow_supplier_information
import tech.dokus.aura.resources.cashflow_supplier_name
import tech.dokus.aura.resources.cashflow_tax_deductibility
import tech.dokus.aura.resources.cashflow_unknown_document_type
import tech.dokus.aura.resources.cashflow_vat_amount
import tech.dokus.aura.resources.common_bank_account
import tech.dokus.aura.resources.common_currency
import tech.dokus.aura.resources.common_date
import tech.dokus.aura.resources.common_notes
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.expense_category_hardware
import tech.dokus.aura.resources.expense_category_insurance
import tech.dokus.aura.resources.expense_category_marketing
import tech.dokus.aura.resources.expense_category_meals
import tech.dokus.aura.resources.expense_category_office_supplies
import tech.dokus.aura.resources.expense_category_other
import tech.dokus.aura.resources.expense_category_professional_services
import tech.dokus.aura.resources.expense_category_rent
import tech.dokus.aura.resources.expense_category_software
import tech.dokus.aura.resources.expense_category_telecommunications
import tech.dokus.aura.resources.expense_category_travel
import tech.dokus.aura.resources.expense_category_utilities
import tech.dokus.aura.resources.expense_category_vehicle
import tech.dokus.aura.resources.invoice_amount
import tech.dokus.aura.resources.invoice_category
import tech.dokus.aura.resources.invoice_description
import tech.dokus.aura.resources.invoice_details
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.aura.resources.invoice_subtotal
import tech.dokus.aura.resources.invoice_total_amount
import tech.dokus.aura.resources.invoice_vat_rate
import tech.dokus.aura.resources.payment_method_bank_transfer
import tech.dokus.aura.resources.payment_method_cash
import tech.dokus.aura.resources.payment_method_check
import tech.dokus.aura.resources.payment_method_credit_card
import tech.dokus.aura.resources.payment_method_debit_card
import tech.dokus.aura.resources.payment_method_other
import tech.dokus.aura.resources.payment_method_paypal
import tech.dokus.aura.resources.payment_method_stripe
import tech.dokus.aura.resources.state_confirming
import tech.dokus.aura.resources.state_saving
import tech.dokus.aura.resources.state_unsaved_changes
import tech.dokus.foundation.aura.components.DraftStatusBadge
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.components.PDatePickerDialog
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Message
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Calendar
import compose.icons.feathericons.ChevronDown
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.ids.ContactId
import tech.dokus.foundation.app.mvi.container

/**
 * Document Review Screen for reviewing and editing AI-extracted document data.
 *
 * Layout:
 * - Desktop: Two-pane layout with document preview on left, editable form on right
 * - Mobile: Single column with collapsible sections
 *
 * Features:
 * - View AI-extracted data with confidence indicators
 * - Edit and correct extracted fields
 * - See provenance info (where data was extracted from)
 * - Save draft or confirm to create entity
 * - Navigate to chat for document Q&A
 */
@Composable
internal fun DocumentReviewScreen(
    documentId: DocumentId,
    container: DocumentReviewContainer = container(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.isLarge
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<DocumentReviewSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
            DocumentReviewSuccess.DraftSaved ->
                stringResource(Res.string.cashflow_draft_saved)
            DocumentReviewSuccess.DocumentConfirmed ->
                stringResource(Res.string.cashflow_document_confirmed)
        }
    }
    val errorMessage = pendingError?.localized

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            snackbarHostState.showSnackbar(successMessage)
            pendingSuccess = null
        }
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    // Subscribe to state and handle actions
    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is DocumentReviewAction.NavigateBack -> {
                navController.popBackStack()
            }
            is DocumentReviewAction.NavigateToChat -> {
                // TODO: Navigate to chat screen with processingId
            }
            is DocumentReviewAction.NavigateToEntity -> {
                // TODO: Navigate to entity detail screen
                navController.popBackStack()
            }
            is DocumentReviewAction.ShowError -> {
                pendingError = action.error
            }
            is DocumentReviewAction.ShowSuccess -> {
                pendingSuccess = action.success
            }
            is DocumentReviewAction.ShowDiscardConfirmation -> {
                // TODO: Show discard confirmation dialog
            }
            is DocumentReviewAction.ShowRejectConfirmation -> {
                // TODO: Show reject confirmation dialog
            }
        }
    }

    // Load document on first composition
    LaunchedEffect(documentId) {
        container.store.intent(DocumentReviewIntent.LoadDocument(documentId))
    }

    Scaffold(
        topBar = {
            ReviewTopBar(
                state = state,
                onBackClick = { navController.popBackStack() },
                onChatClick = { container.store.intent(DocumentReviewIntent.OpenChat) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        when (state) {
            is DocumentReviewState.Loading -> {
                LoadingContent(contentPadding)
            }

            is DocumentReviewState.Content -> {
                val content = state as DocumentReviewState.Content
                if (isLargeScreen) {
                    DesktopReviewContent(
                        state = content,
                        contentPadding = contentPadding,
                        onIntent = { container.store.intent(it) }
                    )
                } else {
                    MobileReviewContent(
                        state = content,
                        contentPadding = contentPadding,
                        onIntent = { container.store.intent(it) }
                    )
                }
            }

            is DocumentReviewState.Error -> {
                val error = state as DocumentReviewState.Error
                ErrorContent(
                    error = error,
                    contentPadding = contentPadding
                )
            }
        }
    }
}

// ============================================================================
// TOP BAR
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewTopBar(
    state: DocumentReviewState,
    onBackClick: () -> Unit,
    onChatClick: () -> Unit,
) {
    val content = state as? DocumentReviewState.Content

    Column {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = content?.document?.document?.filename
                            ?: stringResource(Res.string.cashflow_document_review_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (content != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Show draft status badge
                            content.document.draft?.draftStatus?.let { draftStatus ->
                                DraftStatusBadge(status = draftStatus)
                            }
                            if (content.showConfidence) {
                                ConfidenceBadge(percent = content.confidencePercent)
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                PBackButton(onBackPress = onBackClick)
            },
            actions = {
                // Chat button - only visible when document is confirmed
                if (content != null && content.isDocumentConfirmed) {
                    IconButton(onClick = onChatClick) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = stringResource(Res.string.cashflow_chat_with_document),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = Constrains.Stroke.thin
        )
    }
}

@Composable
private fun ConfidenceBadge(percent: Int) {
    val color = when {
        percent >= 80 -> MaterialTheme.colorScheme.tertiary
        percent >= 50 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = stringResource(Res.string.cashflow_confidence_badge, percent),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

// ============================================================================
// LOADING STATE
// ============================================================================

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.cashflow_loading_document),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// ERROR STATE
// ============================================================================

@Composable
private fun ErrorContent(
    error: DocumentReviewState.Error,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Constrains.Spacing.large),
        contentAlignment = Alignment.Center
    ) {
        DokusErrorContent(
            exception = error.exception,
            retryHandler = error.retryHandler
        )
    }
}

// ============================================================================
// DESKTOP LAYOUT
// ============================================================================

@Composable
private fun DesktopReviewContent(
    state: DocumentReviewState.Content,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Constrains.Spacing.large),
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.large)
    ) {
        // Left side: Document preview
        DocumentPreviewPane(
            previewState = state.previewState,
            selectedFieldPath = state.selectedFieldPath,
            onLoadMore = { maxPages -> onIntent(DocumentReviewIntent.LoadMorePages(maxPages)) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )

        // Right side: Editable form
        EditableFormPane(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .width(420.dp)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun DocumentPreviewPane(
    previewState: DocumentPreviewState,
    selectedFieldPath: String?,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        PdfPreviewPane(
            state = previewState,
            selectedFieldPath = selectedFieldPath,
            onLoadMore = onLoadMore,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun EditableFormPane(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = Constrains.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            // Unsaved changes indicator (hidden when confirmed)
            AnimatedVisibility(
                visible = state.hasUnsavedChanges && !state.isDocumentConfirmed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                UnsavedChangesBar(
                    isSaving = state.isSaving,
                    onSave = { onIntent(DocumentReviewIntent.SaveDraft) },
                    onDiscard = { onIntent(DocumentReviewIntent.DiscardChanges) }
                )
            }

            // Contact Selection Section (Invoice/Bill only)
            if (state.isContactRequired) {
                ContactSelectionSection(
                    documentType = state.editableData.documentType,
                    selectionState = state.contactSelectionState,
                    selectedContactSnapshot = state.selectedContactSnapshot,
                    isBindingContact = state.isBindingContact,
                    isReadOnly = state.isDocumentConfirmed,
                    validationError = state.contactValidationError,
                    onAcceptSuggestion = { onIntent(DocumentReviewIntent.AcceptSuggestedContact) },
                    onChooseDifferent = { onIntent(DocumentReviewIntent.OpenContactPicker) },
                    onSelectContact = { onIntent(DocumentReviewIntent.OpenContactPicker) },
                    onClearContact = { onIntent(DocumentReviewIntent.ClearSelectedContact) },
                    onCreateNewContact = { onIntent(DocumentReviewIntent.OpenCreateContactSheet) },
                )
            }

            // Document type specific form
            when (state.editableData.documentType) {
                DocumentType.Invoice -> InvoiceForm(
                    fields = state.editableData.invoice ?: EditableInvoiceFields(),
                    onFieldUpdate = { field, value ->
                        onIntent(DocumentReviewIntent.UpdateInvoiceField(field, value))
                    },
                    onFieldFocus = { fieldPath ->
                        onIntent(DocumentReviewIntent.SelectFieldForProvenance(fieldPath))
                    },
                    contactSuggestions = state.contactSuggestions,
                    onContactSelect = { onIntent(DocumentReviewIntent.SelectContact(it)) },
                    isReadOnly = state.isDocumentConfirmed,
                )
                DocumentType.Bill -> BillForm(
                    fields = state.editableData.bill ?: EditableBillFields(),
                    onFieldUpdate = { field, value ->
                        onIntent(DocumentReviewIntent.UpdateBillField(field, value))
                    },
                    onFieldFocus = { fieldPath ->
                        onIntent(DocumentReviewIntent.SelectFieldForProvenance(fieldPath))
                    },
                    contactSuggestions = state.contactSuggestions,
                    onContactSelect = { onIntent(DocumentReviewIntent.SelectContact(it)) },
                    isReadOnly = state.isDocumentConfirmed,
                )
                DocumentType.Expense -> ExpenseForm(
                    fields = state.editableData.expense ?: EditableExpenseFields(),
                    onFieldUpdate = { field, value ->
                        onIntent(DocumentReviewIntent.UpdateExpenseField(field, value))
                    },
                    onFieldFocus = { fieldPath ->
                        onIntent(DocumentReviewIntent.SelectFieldForProvenance(fieldPath))
                    },
                    isReadOnly = state.isDocumentConfirmed,
                )
                else -> {
                    Text(
                        text = stringResource(Res.string.cashflow_unknown_document_type),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Footer with action buttons
        DocumentReviewFooter(
            canConfirm = state.canConfirm,
            isConfirming = state.isConfirming,
            isSaving = state.isSaving,
            isBindingContact = state.isBindingContact,
            hasUnsavedChanges = state.hasUnsavedChanges,
            isDocumentConfirmed = state.isDocumentConfirmed,
            confirmBlockedReason = state.confirmBlockedReason,
            onConfirm = { onIntent(DocumentReviewIntent.Confirm) },
            onSaveChanges = { onIntent(DocumentReviewIntent.SaveDraft) },
            onReject = { onIntent(DocumentReviewIntent.Reject) },
            onOpenChat = { onIntent(DocumentReviewIntent.OpenChat) },
        )
    }

    // Contact Create Sheet
    ContactCreateSheet(
        isVisible = state.showCreateContactSheet,
        onDismiss = { onIntent(DocumentReviewIntent.CloseCreateContactSheet) },
        preFillData = state.createContactPreFill,
        onContactCreated = { contactId -> onIntent(DocumentReviewIntent.ContactCreated(contactId)) },
    )
}

// ============================================================================
// MOBILE LAYOUT
// ============================================================================

@Composable
private fun MobileReviewContent(
    state: DocumentReviewState.Content,
    contentPadding: PaddingValues,
    onIntent: (DocumentReviewIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(Constrains.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            // Unsaved changes indicator (hidden when confirmed)
            AnimatedVisibility(
                visible = state.hasUnsavedChanges && !state.isDocumentConfirmed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                UnsavedChangesBar(
                    isSaving = state.isSaving,
                    onSave = { onIntent(DocumentReviewIntent.SaveDraft) },
                    onDiscard = { onIntent(DocumentReviewIntent.DiscardChanges) }
                )
            }

            // PDF Preview row (thumbnail + tap to open sheet)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                PdfPreviewRow(
                    previewState = state.previewState,
                    onClick = { onIntent(DocumentReviewIntent.OpenPreviewSheet) },
                )
            }

            // Contact Selection Section (Invoice/Bill only)
            if (state.isContactRequired) {
                ContactSelectionSection(
                    documentType = state.editableData.documentType,
                    selectionState = state.contactSelectionState,
                    selectedContactSnapshot = state.selectedContactSnapshot,
                    isBindingContact = state.isBindingContact,
                    isReadOnly = state.isDocumentConfirmed,
                    validationError = state.contactValidationError,
                    onAcceptSuggestion = { onIntent(DocumentReviewIntent.AcceptSuggestedContact) },
                    onChooseDifferent = { onIntent(DocumentReviewIntent.OpenContactPicker) },
                    onSelectContact = { onIntent(DocumentReviewIntent.OpenContactPicker) },
                    onClearContact = { onIntent(DocumentReviewIntent.ClearSelectedContact) },
                    onCreateNewContact = { onIntent(DocumentReviewIntent.OpenCreateContactSheet) },
                )
            }

            // Form section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(Constrains.Spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
                ) {
                    Text(
                        text = stringResource(Res.string.invoice_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    when (state.editableData.documentType) {
                        DocumentType.Invoice -> InvoiceForm(
                            fields = state.editableData.invoice ?: EditableInvoiceFields(),
                            onFieldUpdate = { field, value ->
                                onIntent(DocumentReviewIntent.UpdateInvoiceField(field, value))
                            },
                            onFieldFocus = { fieldPath ->
                                onIntent(DocumentReviewIntent.SelectFieldForProvenance(fieldPath))
                            },
                            contactSuggestions = state.contactSuggestions,
                            onContactSelect = { onIntent(DocumentReviewIntent.SelectContact(it)) },
                            isReadOnly = state.isDocumentConfirmed,
                        )
                        DocumentType.Bill -> BillForm(
                            fields = state.editableData.bill ?: EditableBillFields(),
                            onFieldUpdate = { field, value ->
                                onIntent(DocumentReviewIntent.UpdateBillField(field, value))
                            },
                            onFieldFocus = { fieldPath ->
                                onIntent(DocumentReviewIntent.SelectFieldForProvenance(fieldPath))
                            },
                            contactSuggestions = state.contactSuggestions,
                            onContactSelect = { onIntent(DocumentReviewIntent.SelectContact(it)) },
                            isReadOnly = state.isDocumentConfirmed,
                        )
                        DocumentType.Expense -> ExpenseForm(
                            fields = state.editableData.expense ?: EditableExpenseFields(),
                            onFieldUpdate = { field, value ->
                                onIntent(DocumentReviewIntent.UpdateExpenseField(field, value))
                            },
                            onFieldFocus = { fieldPath ->
                                onIntent(DocumentReviewIntent.SelectFieldForProvenance(fieldPath))
                            },
                            isReadOnly = state.isDocumentConfirmed,
                        )
                        else -> {
                            Text(
                                text = stringResource(Res.string.cashflow_unknown_document_type),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // Footer with action buttons
        DocumentReviewFooter(
            canConfirm = state.canConfirm,
            isConfirming = state.isConfirming,
            isSaving = state.isSaving,
            isBindingContact = state.isBindingContact,
            hasUnsavedChanges = state.hasUnsavedChanges,
            isDocumentConfirmed = state.isDocumentConfirmed,
            confirmBlockedReason = state.confirmBlockedReason,
            onConfirm = { onIntent(DocumentReviewIntent.Confirm) },
            onSaveChanges = { onIntent(DocumentReviewIntent.SaveDraft) },
            onReject = { onIntent(DocumentReviewIntent.Reject) },
            onOpenChat = { onIntent(DocumentReviewIntent.OpenChat) },
        )
    }

    // PDF Preview Bottom Sheet
    PdfPreviewBottomSheet(
        isVisible = state.showPreviewSheet,
        onDismiss = { onIntent(DocumentReviewIntent.ClosePreviewSheet) },
        previewState = state.previewState,
        onLoadMore = { maxPages -> onIntent(DocumentReviewIntent.LoadMorePages(maxPages)) },
    )

    // Contact Create Sheet
    ContactCreateSheet(
        isVisible = state.showCreateContactSheet,
        onDismiss = { onIntent(DocumentReviewIntent.CloseCreateContactSheet) },
        preFillData = state.createContactPreFill,
        onContactCreated = { contactId -> onIntent(DocumentReviewIntent.ContactCreated(contactId)) },
    )
}

@Composable
private fun CollapsibleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(Constrains.Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = stringResource(
                        if (isExpanded) Res.string.action_collapse else Res.string.action_expand
                    )
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(Constrains.Spacing.medium)) {
                    content()
                }
            }
        }
    }
}

// ============================================================================
// FORM COMPONENTS
// ============================================================================

@Composable
private fun UnsavedChangesBar(
    isSaving: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.state_unsaved_changes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)) {
                OutlinedButton(
                    onClick = onDiscard,
                    enabled = !isSaving,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        stringResource(Res.string.action_discard),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                POutlinedButton(
                    text = if (isSaving) {
                        stringResource(Res.string.state_saving)
                    } else {
                        stringResource(Res.string.action_save)
                    },
                    enabled = !isSaving,
                    isLoading = isSaving,
                    onClick = onSave
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    canConfirm: Boolean,
    isConfirming: Boolean,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        POutlinedButton(
            text = stringResource(Res.string.action_reject),
            modifier = Modifier.weight(1f),
            enabled = !isConfirming,
            onClick = onReject
        )
        PPrimaryButton(
            text = if (isConfirming) {
                stringResource(Res.string.state_confirming)
            } else {
                stringResource(Res.string.action_confirm)
            },
            modifier = Modifier.weight(1f),
            enabled = canConfirm,
            isLoading = isConfirming,
            onClick = onConfirm
        )
    }
}

// ============================================================================
// INVOICE FORM
// ============================================================================

@Composable
private fun InvoiceForm(
    fields: EditableInvoiceFields,
    onFieldUpdate: (InvoiceField, Any?) -> Unit,
    onFieldFocus: (String) -> Unit,
    contactSuggestions: List<ContactSuggestion>,
    onContactSelect: (ContactId) -> Unit,
    isReadOnly: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        // Client section
        SectionHeader(stringResource(Res.string.cashflow_client_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_client_name),
            value = fields.clientName,
            onValueChange = { onFieldUpdate(InvoiceField.CLIENT_NAME, it) },
            modifier = Modifier.fillMaxWidth()
        )

        ContactSuggestionsChips(
            suggestions = contactSuggestions,
            selectedContactId = fields.selectedContactId,
            onSelect = onContactSelect
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_vat_number),
            value = fields.clientVatNumber,
            onValueChange = { onFieldUpdate(InvoiceField.CLIENT_VAT_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_email),
            value = fields.clientEmail,
            onValueChange = { onFieldUpdate(InvoiceField.CLIENT_EMAIL, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_address),
            value = fields.clientAddress,
            onValueChange = { onFieldUpdate(InvoiceField.CLIENT_ADDRESS, it) },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Invoice details section
        SectionHeader(stringResource(Res.string.cashflow_invoice_details_section))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_invoice_number),
            value = fields.invoiceNumber,
            onValueChange = { onFieldUpdate(InvoiceField.INVOICE_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        DatePickerField(
            label = stringResource(Res.string.invoice_issue_date),
            value = fields.issueDate,
            onValueChange = { onFieldUpdate(InvoiceField.ISSUE_DATE, it) }
        )

        DatePickerField(
            label = stringResource(Res.string.invoice_due_date),
            value = fields.dueDate,
            onValueChange = { onFieldUpdate(InvoiceField.DUE_DATE, it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Amounts section
        SectionHeader(stringResource(Res.string.cashflow_section_amounts))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_subtotal),
            value = fields.subtotalAmount,
            onValueChange = { onFieldUpdate(InvoiceField.SUBTOTAL_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_vat_amount),
            value = fields.vatAmount,
            onValueChange = { onFieldUpdate(InvoiceField.VAT_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_total_amount),
            value = fields.totalAmount,
            onValueChange = { onFieldUpdate(InvoiceField.TOTAL_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_currency),
            value = fields.currency,
            onValueChange = { onFieldUpdate(InvoiceField.CURRENCY, it) },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Additional info
        SectionHeader(stringResource(Res.string.cashflow_section_additional_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_payment_terms),
            value = fields.paymentTerms,
            onValueChange = { onFieldUpdate(InvoiceField.PAYMENT_TERMS, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_bank_account),
            value = fields.bankAccount,
            onValueChange = { onFieldUpdate(InvoiceField.BANK_ACCOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_notes),
            value = fields.notes,
            onValueChange = { onFieldUpdate(InvoiceField.NOTES, it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================================
// BILL FORM
// ============================================================================

@Composable
private fun BillForm(
    fields: EditableBillFields,
    onFieldUpdate: (BillField, Any?) -> Unit,
    onFieldFocus: (String) -> Unit,
    contactSuggestions: List<ContactSuggestion>,
    onContactSelect: (ContactId) -> Unit,
    isReadOnly: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        // Supplier section
        SectionHeader(stringResource(Res.string.cashflow_supplier_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_supplier_name),
            value = fields.supplierName,
            onValueChange = { onFieldUpdate(BillField.SUPPLIER_NAME, it) },
            modifier = Modifier.fillMaxWidth()
        )

        ContactSuggestionsChips(
            suggestions = contactSuggestions,
            selectedContactId = fields.selectedContactId,
            onSelect = onContactSelect
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_vat_number),
            value = fields.supplierVatNumber,
            onValueChange = { onFieldUpdate(BillField.SUPPLIER_VAT_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_address),
            value = fields.supplierAddress,
            onValueChange = { onFieldUpdate(BillField.SUPPLIER_ADDRESS, it) },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Bill details section
        SectionHeader(stringResource(Res.string.cashflow_bill_details_section))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_invoice_number),
            value = fields.invoiceNumber,
            onValueChange = { onFieldUpdate(BillField.INVOICE_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        DatePickerField(
            label = stringResource(Res.string.invoice_issue_date),
            value = fields.issueDate,
            onValueChange = { onFieldUpdate(BillField.ISSUE_DATE, it) }
        )

        DatePickerField(
            label = stringResource(Res.string.invoice_due_date),
            value = fields.dueDate,
            onValueChange = { onFieldUpdate(BillField.DUE_DATE, it) }
        )

        CategoryDropdown(
            label = stringResource(Res.string.invoice_category),
            value = fields.category,
            onValueChange = { onFieldUpdate(BillField.CATEGORY, it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Amounts section
        SectionHeader(stringResource(Res.string.cashflow_section_amounts))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_amount),
            value = fields.amount,
            onValueChange = { onFieldUpdate(BillField.AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_vat_amount),
            value = fields.vatAmount,
            onValueChange = { onFieldUpdate(BillField.VAT_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_vat_rate),
            value = fields.vatRate,
            onValueChange = { onFieldUpdate(BillField.VAT_RATE, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_currency),
            value = fields.currency,
            onValueChange = { onFieldUpdate(BillField.CURRENCY, it) },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Additional info
        SectionHeader(stringResource(Res.string.cashflow_section_additional_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_description),
            value = fields.description,
            onValueChange = { onFieldUpdate(BillField.DESCRIPTION, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_notes),
            value = fields.notes,
            onValueChange = { onFieldUpdate(BillField.NOTES, it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================================
// EXPENSE FORM
// ============================================================================

@Composable
private fun ExpenseForm(
    fields: EditableExpenseFields,
    onFieldUpdate: (ExpenseField, Any?) -> Unit,
    onFieldFocus: (String) -> Unit,
    isReadOnly: Boolean = false,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        // Merchant section
        SectionHeader(stringResource(Res.string.cashflow_merchant_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_merchant),
            value = fields.merchant,
            onValueChange = { onFieldUpdate(ExpenseField.MERCHANT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_address),
            value = fields.merchantAddress,
            onValueChange = { onFieldUpdate(ExpenseField.MERCHANT_ADDRESS, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_vat_number),
            value = fields.merchantVatNumber,
            onValueChange = { onFieldUpdate(ExpenseField.MERCHANT_VAT_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Expense details section
        SectionHeader(stringResource(Res.string.cashflow_expense_details_section))

        DatePickerField(
            label = stringResource(Res.string.common_date),
            value = fields.date,
            onValueChange = { onFieldUpdate(ExpenseField.DATE, it) }
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_receipt_number),
            value = fields.receiptNumber,
            onValueChange = { onFieldUpdate(ExpenseField.RECEIPT_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        CategoryDropdown(
            label = stringResource(Res.string.invoice_category),
            value = fields.category,
            onValueChange = { onFieldUpdate(ExpenseField.CATEGORY, it) }
        )

        PaymentMethodDropdown(
            label = stringResource(Res.string.cashflow_payment_method),
            value = fields.paymentMethod,
            onValueChange = { onFieldUpdate(ExpenseField.PAYMENT_METHOD, it) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Amounts section
        SectionHeader(stringResource(Res.string.cashflow_section_amounts))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_amount),
            value = fields.amount,
            onValueChange = { onFieldUpdate(ExpenseField.AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_vat_amount),
            value = fields.vatAmount,
            onValueChange = { onFieldUpdate(ExpenseField.VAT_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_vat_rate),
            value = fields.vatRate,
            onValueChange = { onFieldUpdate(ExpenseField.VAT_RATE, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_currency),
            value = fields.currency,
            onValueChange = { onFieldUpdate(ExpenseField.CURRENCY, it) },
            modifier = Modifier.fillMaxWidth()
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Deductibility section
        SectionHeader(stringResource(Res.string.cashflow_tax_deductibility))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.cashflow_is_deductible),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = fields.isDeductible,
                onCheckedChange = { onFieldUpdate(ExpenseField.IS_DEDUCTIBLE, it) }
            )
        }

        if (fields.isDeductible) {
            PTextFieldStandard(
                fieldName = stringResource(Res.string.cashflow_deductible_percentage),
                value = fields.deductiblePercentage,
                onValueChange = { onFieldUpdate(ExpenseField.DEDUCTIBLE_PERCENTAGE, it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Constrains.Spacing.small))

        // Additional info
        SectionHeader(stringResource(Res.string.cashflow_section_additional_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_description),
            value = fields.description,
            onValueChange = { onFieldUpdate(ExpenseField.DESCRIPTION, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_notes),
            value = fields.notes,
            onValueChange = { onFieldUpdate(ExpenseField.NOTES, it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================================================
// SHARED FORM COMPONENTS
// ============================================================================

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Constrains.Spacing.small)
    )
}

@Composable
private fun DatePickerField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate?) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .border(
                    width = Constrains.Stroke.thin,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small
                )
                .clickable { showDatePicker = true }
                .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value?.toString() ?: stringResource(Res.string.action_select_date),
                style = MaterialTheme.typography.bodyMedium,
                color = if (value != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Icon(
                imageVector = FeatherIcons.Calendar,
                contentDescription = stringResource(Res.string.action_select_date),
                modifier = Modifier.size(Constrains.IconSize.small),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showDatePicker) {
        PDatePickerDialog(
            initialDate = value,
            onDateSelected = { selectedDate ->
                onValueChange(selectedDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
private fun CategoryDropdown(
    label: String,
    value: ExpenseCategory?,
    onValueChange: (ExpenseCategory?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .border(
                        width = Constrains.Stroke.thin,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.small
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value?.let { categoryDisplayName(it) }
                        ?: stringResource(Res.string.cashflow_select_category),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Icon(
                    imageVector = FeatherIcons.ChevronDown,
                    contentDescription = stringResource(Res.string.action_select),
                    modifier = Modifier.size(Constrains.IconSize.small),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                ExpenseCategory.entries.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(categoryDisplayName(category)) },
                        onClick = {
                            onValueChange(category)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodDropdown(
    label: String,
    value: PaymentMethod?,
    onValueChange: (PaymentMethod?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .border(
                        width = Constrains.Stroke.thin,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.small
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value?.let { paymentMethodDisplayName(it) }
                        ?: stringResource(Res.string.cashflow_select_payment_method),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (value != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Icon(
                    imageVector = FeatherIcons.ChevronDown,
                    contentDescription = stringResource(Res.string.action_select),
                    modifier = Modifier.size(Constrains.IconSize.small),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                PaymentMethod.entries.forEach { method ->
                    DropdownMenuItem(
                        text = { Text(paymentMethodDisplayName(method)) },
                        onClick = {
                            onValueChange(method)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactSuggestionsChips(
    suggestions: List<ContactSuggestion>,
    selectedContactId: ContactId?,
    onSelect: (ContactId) -> Unit,
) {
    if (suggestions.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
    ) {
        Text(
            text = stringResource(Res.string.cashflow_suggested_contacts),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
            modifier = Modifier.fillMaxWidth()
        ) {
            suggestions.forEach { suggestion ->
                val isSelected = suggestion.contactId == selectedContactId
                val displayName = suggestion.name.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.common_unknown)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                        .clickable { onSelect(suggestion.contactId) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun categoryDisplayName(category: ExpenseCategory): String {
    val labelRes = when (category) {
        ExpenseCategory.OfficeSupplies -> Res.string.expense_category_office_supplies
        ExpenseCategory.Travel -> Res.string.expense_category_travel
        ExpenseCategory.Meals -> Res.string.expense_category_meals
        ExpenseCategory.Software -> Res.string.expense_category_software
        ExpenseCategory.Hardware -> Res.string.expense_category_hardware
        ExpenseCategory.Utilities -> Res.string.expense_category_utilities
        ExpenseCategory.Rent -> Res.string.expense_category_rent
        ExpenseCategory.Insurance -> Res.string.expense_category_insurance
        ExpenseCategory.Marketing -> Res.string.expense_category_marketing
        ExpenseCategory.ProfessionalServices -> Res.string.expense_category_professional_services
        ExpenseCategory.Telecommunications -> Res.string.expense_category_telecommunications
        ExpenseCategory.Vehicle -> Res.string.expense_category_vehicle
        ExpenseCategory.Other -> Res.string.expense_category_other
    }

    return stringResource(labelRes)
}

@Composable
private fun paymentMethodDisplayName(method: PaymentMethod): String {
    val labelRes = when (method) {
        PaymentMethod.BankTransfer -> Res.string.payment_method_bank_transfer
        PaymentMethod.CreditCard -> Res.string.payment_method_credit_card
        PaymentMethod.DebitCard -> Res.string.payment_method_debit_card
        PaymentMethod.PayPal -> Res.string.payment_method_paypal
        PaymentMethod.Stripe -> Res.string.payment_method_stripe
        PaymentMethod.Cash -> Res.string.payment_method_cash
        PaymentMethod.Check -> Res.string.payment_method_check
        PaymentMethod.Other -> Res.string.payment_method_other
    }

    return stringResource(labelRes)
}
