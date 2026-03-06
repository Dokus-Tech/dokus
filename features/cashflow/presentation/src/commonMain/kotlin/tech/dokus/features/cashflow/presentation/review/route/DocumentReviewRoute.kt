package tech.dokus.features.cashflow.presentation.review.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_confirm
import tech.dokus.aura.resources.cashflow_discard_changes_message
import tech.dokus.aura.resources.cashflow_discard_changes_title
import tech.dokus.aura.resources.cashflow_document_confirmed
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.cashflow.presentation.documents.route.DOCUMENTS_REFRESH_REQUIRED_RESULT_KEY
import tech.dokus.features.cashflow.presentation.review.DocumentReviewAction
import tech.dokus.features.cashflow.presentation.review.DocumentReviewContainer
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewQueueState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewSuccess
import tech.dokus.features.cashflow.presentation.review.components.ContactEditSheet
import tech.dokus.features.cashflow.presentation.review.components.DocumentReviewDesktopSplit
import tech.dokus.features.cashflow.presentation.review.components.FeedbackDialog
import tech.dokus.features.cashflow.presentation.review.components.RecordPaymentDialog
import tech.dokus.features.cashflow.presentation.review.components.RejectDocumentDialog
import tech.dokus.features.cashflow.presentation.review.components.SourceEvidenceDialog
import tech.dokus.features.cashflow.presentation.review.screen.DocumentReviewScreen
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.ContactCreateOrigin
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

private const val CONTACT_RESULT_KEY = "documentReview_contactId"

@Composable
internal fun DocumentReviewRoute(
    route: CashFlowDestination.DocumentReview,
    container: DocumentReviewContainer = container {
        parametersOf(
            DocumentId.parse(route.documentId),
            route.toRouteContextOrNull(),
        )
    },
    listContacts: ListContactsUseCase = org.koin.compose.koinInject(),
) {
    val accessContext = LocalUserAccessContext.current
    val isAccountantReadOnly = accessContext.isBookkeeperConsoleDrillDown
    val navController = LocalNavController.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val initialDocumentId = remember(route.documentId) { DocumentId.parse(route.documentId) }

    fun markDocumentsRefreshRequired() {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set(DOCUMENTS_REFRESH_REQUIRED_RESULT_KEY, true)
    }

    fun dispatchIntent(intent: DocumentReviewIntent) {
        if (isAccountantReadOnly && intent.isBlockedForAccountantReadOnly()) return
        container.store.intent(intent)
    }

    val pendingContactId = backStackEntry?.savedStateHandle?.get<String>(CONTACT_RESULT_KEY)

    LaunchedEffect(pendingContactId) {
        if (pendingContactId != null) {
            dispatchIntent(
                DocumentReviewIntent.ContactCreated(ContactId.parse(pendingContactId))
            )
            backStackEntry?.savedStateHandle?.remove<String>(CONTACT_RESULT_KEY)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSuccess by remember { mutableStateOf<DocumentReviewSuccess?>(null) }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var contactsState by remember { mutableStateOf<DokusState<List<ContactDto>>>(DokusState.idle()) }

    val successMessage = pendingSuccess?.let { success ->
        when (success) {
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

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is DocumentReviewAction.NavigateBack -> {
                markDocumentsRefreshRequired()
                navController.popBackStack()
            }
            is DocumentReviewAction.NavigateToChat -> {
                navController.navigateTo(CashFlowDestination.DocumentChat(action.documentId.toString()))
            }
            is DocumentReviewAction.NavigateToEntity -> {
                markDocumentsRefreshRequired()
                navController.popBackStack()
            }
            is DocumentReviewAction.NavigateToCashflowEntry -> {
                navController.navigateTo(
                    CashFlowDestination.CashflowLedger(action.entryId.toString())
                )
            }
            is DocumentReviewAction.ShowError -> pendingError = action.error
            is DocumentReviewAction.ShowSuccess -> {
                pendingSuccess = action.success
                markDocumentsRefreshRequired()
            }
        }
    }

    // Auto-poll every 3s while processing so the UI transitions when extraction completes/fails
    val shouldPoll = when (state) {
        is DocumentReviewState.AwaitingExtraction -> true
        is DocumentReviewState.Content -> (state as DocumentReviewState.Content).isProcessing
        else -> false
    }

    LaunchedEffect(shouldPoll) {
        if (!shouldPoll) return@LaunchedEffect
        while (true) {
            delay(3_000L)
            dispatchIntent(DocumentReviewIntent.Refresh)
        }
    }

    val isLargeScreen = LocalScreenSize.isLarge
    val contentState = state as? DocumentReviewState.Content
    val navigateBack: () -> Unit = {
        markDocumentsRefreshRequired()
        navController.popBackStack()
    }
    val requestBackNavigation: () -> Unit = {
        if (contentState?.hasUnsyncedLocalChanges == true) {
            showDiscardDialog = true
        } else {
            navigateBack()
        }
    }

    // Load contacts when contact sheet opens
    LaunchedEffect(contentState?.showContactSheet) {
        if (contentState?.showContactSheet == true && contactsState !is DokusState.Success) {
            contactsState = DokusState.loading()
            listContacts(limit = 100).fold(
                onSuccess = { contacts ->
                    contactsState = DokusState.success(contacts)
                },
                onFailure = { error ->
                    contactsState = DokusState.error(
                        exception = tech.dokus.domain.exceptions.DokusException.Unknown(error),
                        retryHandler = { /* no retry */ },
                    )
                },
            )
        }
    }

    val reviewContent: @Composable () -> Unit = {
        DocumentReviewScreen(
            state = state,
            isLargeScreen = isLargeScreen,
            isAccountantReadOnly = isAccountantReadOnly,
            onIntent = { dispatchIntent(it) },
            onBackClick = requestBackNavigation,
            onOpenChat = { dispatchIntent(DocumentReviewIntent.OpenChat) },
            onOpenSource = { sourceId ->
                val activeDocumentId = (state as? DocumentReviewState.Content)
                    ?.documentId
                    ?.toString()
                    ?: route.documentId
                navController.navigateTo(
                    CashFlowDestination.DocumentSourceViewer(
                        documentId = activeDocumentId,
                        sourceId = sourceId.toString(),
                    )
                )
            },
            onCorrectContact = { _ ->
                // Open the contact sheet instead of navigating away
                dispatchIntent(DocumentReviewIntent.OpenContactSheet)
            },
            onCreateContact = { counterparty ->
                if (isAccountantReadOnly) return@DocumentReviewScreen
                dispatchIntent(DocumentReviewIntent.SetCounterpartyIntent(CounterpartyIntent.Pending))
                navController.navigateTo(
                    ContactsDestination.CreateContact(
                        prefillCompanyName = counterparty.name,
                        prefillVat = counterparty.vatNumber,
                        prefillAddress = counterparty.address,
                        origin = ContactCreateOrigin.DocumentReview.name,
                    )
                )
            },
            snackbarHostState = snackbarHostState,
        )
    }

    val queueState = state.queueStateOrNull()
    val desktopQueueState = queueState?.takeIf { isLargeScreen && it.items.isNotEmpty() }
    val selectedDocumentId = state.selectedQueueDocumentIdOrDefault(initialDocumentId)
    val selectedDoc = queueState?.items?.firstOrNull { it.id == selectedDocumentId }

    Box(modifier = Modifier.fillMaxSize()) {
        if (desktopQueueState != null) {
            DocumentReviewDesktopSplit(
                documents = desktopQueueState.items,
                selectedDocumentId = selectedDocumentId,
                selectedDoc = selectedDoc,
                hasMore = desktopQueueState.hasMore,
                isLoadingMore = desktopQueueState.isLoadingMore,
                onSelectDocument = { selectedDocumentIdCandidate ->
                    if (selectedDocumentIdCandidate != selectedDocumentId) {
                        dispatchIntent(
                            DocumentReviewIntent.SelectQueueDocument(selectedDocumentIdCandidate)
                        )
                    }
                },
                onLoadMore = {
                    dispatchIntent(DocumentReviewIntent.LoadMoreQueue)
                },
                onExit = requestBackNavigation,
                content = reviewContent,
            )
        } else {
            reviewContent()
        }
    }

    // Contact Edit Sheet
    contentState?.let { content ->
        ContactEditSheet(
            isVisible = content.showContactSheet && !isAccountantReadOnly,
            onDismiss = { dispatchIntent(DocumentReviewIntent.CloseContactSheet) },
            suggestions = content.contactSuggestions,
            contactsState = contactsState,
            selectedContactId = content.selectedContactId,
            searchQuery = content.contactSheetSearchQuery,
            onSearchQueryChange = { query ->
                dispatchIntent(DocumentReviewIntent.UpdateContactSheetSearch(query))
            },
            onSelectContact = { contactId ->
                dispatchIntent(DocumentReviewIntent.SelectContact(contactId))
                dispatchIntent(DocumentReviewIntent.CloseContactSheet)
            },
            onCreateNewContact = {
                // Close sheet and navigate to contact creation
                if (isAccountantReadOnly) return@ContactEditSheet
                dispatchIntent(DocumentReviewIntent.CloseContactSheet)
                dispatchIntent(
                    DocumentReviewIntent.SetCounterpartyIntent(CounterpartyIntent.Pending)
                )
                val counterparty = tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo(content)
                navController.navigateTo(
                    ContactsDestination.CreateContact(
                        prefillCompanyName = counterparty.name,
                        prefillVat = counterparty.vatNumber,
                        prefillAddress = counterparty.address,
                        origin = ContactCreateOrigin.DocumentReview.name,
                    )
                )
            },
        )
    }

    // Discard changes confirmation dialog
    if (showDiscardDialog) {
        DokusDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = stringResource(Res.string.cashflow_discard_changes_title),
            content = {
                Text(stringResource(Res.string.cashflow_discard_changes_message))
            },
            primaryAction = DokusDialogAction(
                text = stringResource(Res.string.action_confirm),
                onClick = {
                    showDiscardDialog = false
                    navigateBack()
                },
                isDestructive = true,
            ),
            secondaryAction = DokusDialogAction(
                text = stringResource(Res.string.action_cancel),
                onClick = { showDiscardDialog = false },
            ),
        )
    }

    // Feedback dialog (correction-first "Something's wrong" flow)
    (state as? DocumentReviewState.Content)?.feedbackDialogState?.let { dialogState ->
        if (isAccountantReadOnly) return@let
        FeedbackDialog(
            state = dialogState,
            onFeedbackChanged = { text ->
                dispatchIntent(DocumentReviewIntent.UpdateFeedbackText(text))
            },
            onSubmit = {
                dispatchIntent(DocumentReviewIntent.SubmitFeedback)
            },
            onRejectInstead = {
                dispatchIntent(DocumentReviewIntent.DismissFeedbackDialog)
                dispatchIntent(DocumentReviewIntent.ShowRejectDialog)
            },
            onDismiss = {
                dispatchIntent(DocumentReviewIntent.DismissFeedbackDialog)
            },
        )
    }

    // Reject document dialog (state-driven)
    (state as? DocumentReviewState.Content)?.rejectDialogState?.let { dialogState ->
        if (isAccountantReadOnly) return@let
        RejectDocumentDialog(
            state = dialogState,
            onReasonSelected = { reason ->
                dispatchIntent(DocumentReviewIntent.SelectRejectReason(reason))
            },
            onNoteChanged = { note ->
                dispatchIntent(DocumentReviewIntent.UpdateRejectNote(note))
            },
            onConfirm = {
                dispatchIntent(DocumentReviewIntent.ConfirmReject)
            },
            onDismiss = {
                dispatchIntent(DocumentReviewIntent.DismissRejectDialog)
            },
        )
    }

    val content = state as? DocumentReviewState.Content
    val viewerState = content?.sourceViewerState
    if (isLargeScreen && content != null && viewerState != null) {
        SourceEvidenceDialog(
            contentState = content,
            viewerState = viewerState,
            onClose = { dispatchIntent(DocumentReviewIntent.CloseSourceModal) },
            onToggleTechnicalDetails = {
                dispatchIntent(DocumentReviewIntent.ToggleSourceTechnicalDetails)
            },
            onRetry = { dispatchIntent(DocumentReviewIntent.OpenSourceModal(viewerState.sourceId)) },
        )
    }
    content?.paymentSheetState?.let { paymentState ->
        if (isAccountantReadOnly) return@let
        val currencySign = when (val data = content.draftData) {
            is tech.dokus.domain.model.InvoiceDraftData -> data.currency.displaySign
            is tech.dokus.domain.model.CreditNoteDraftData -> data.currency.displaySign
            else -> "\u20AC"
        }
        RecordPaymentDialog(
            sheetState = paymentState,
            currencySign = currencySign,
            onPaidAtChange = { paidAt ->
                container.store.intent(DocumentReviewIntent.UpdatePaymentPaidAt(paidAt))
            },
            onAmountChange = { amount ->
                dispatchIntent(DocumentReviewIntent.UpdatePaymentAmountText(amount))
            },
            onNoteChange = { note ->
                dispatchIntent(DocumentReviewIntent.UpdatePaymentNote(note))
            },
            onOpenTransactionPicker = {
                dispatchIntent(DocumentReviewIntent.OpenPaymentTransactionPicker)
            },
            onCloseTransactionPicker = {
                dispatchIntent(DocumentReviewIntent.ClosePaymentTransactionPicker)
            },
            onSelectTransaction = { transactionId ->
                dispatchIntent(DocumentReviewIntent.SelectPaymentTransaction(transactionId))
            },
            onClearSelectedTransaction = {
                dispatchIntent(DocumentReviewIntent.ClearPaymentTransactionSelection)
            },
            onSubmit = { dispatchIntent(DocumentReviewIntent.SubmitPayment) },
            onDismiss = { dispatchIntent(DocumentReviewIntent.ClosePaymentSheet) },
        )
    }
}

private fun DocumentReviewIntent.isBlockedForAccountantReadOnly(): Boolean = when (this) {
    is DocumentReviewIntent.OpenPaymentSheet,
    is DocumentReviewIntent.UpdatePaymentAmountText,
    is DocumentReviewIntent.UpdatePaymentPaidAt,
    is DocumentReviewIntent.UpdatePaymentNote,
    is DocumentReviewIntent.SubmitPayment,
    is DocumentReviewIntent.SelectContact,
    is DocumentReviewIntent.AcceptSuggestedContact,
    is DocumentReviewIntent.ClearSelectedContact,
    is DocumentReviewIntent.ContactCreated,
    is DocumentReviewIntent.SetCounterpartyIntent,
    is DocumentReviewIntent.OpenContactSheet,
    is DocumentReviewIntent.CloseContactSheet,
    is DocumentReviewIntent.UpdateContactSheetSearch,
    is DocumentReviewIntent.AddLineItem,
    is DocumentReviewIntent.UpdateLineItem,
    is DocumentReviewIntent.RemoveLineItem,
    is DocumentReviewIntent.Confirm,
    is DocumentReviewIntent.ShowRejectDialog,
    is DocumentReviewIntent.DismissRejectDialog,
    is DocumentReviewIntent.SelectRejectReason,
    is DocumentReviewIntent.UpdateRejectNote,
    is DocumentReviewIntent.ConfirmReject,
    is DocumentReviewIntent.ShowFeedbackDialog,
    is DocumentReviewIntent.DismissFeedbackDialog,
    is DocumentReviewIntent.UpdateFeedbackText,
    is DocumentReviewIntent.SubmitFeedback,
    is DocumentReviewIntent.RequestAmendment,
    is DocumentReviewIntent.RetryAnalysis,
    is DocumentReviewIntent.ResolvePossibleMatchSame,
    is DocumentReviewIntent.ResolvePossibleMatchDifferent,
    is DocumentReviewIntent.SelectDocumentType,
    is DocumentReviewIntent.SelectDirection -> true
    else -> false
}

private fun DocumentReviewState.queueStateOrNull(): DocumentReviewQueueState? = when (this) {
    is DocumentReviewState.Loading -> queueState
    is DocumentReviewState.AwaitingExtraction -> queueState
    is DocumentReviewState.Content -> queueState
    is DocumentReviewState.Error -> null
}

private fun DocumentReviewState.selectedQueueDocumentIdOrDefault(defaultDocumentId: DocumentId): DocumentId = when (this) {
    is DocumentReviewState.Loading -> selectedQueueDocumentId ?: defaultDocumentId
    is DocumentReviewState.AwaitingExtraction -> selectedQueueDocumentId ?: documentId
    is DocumentReviewState.Content -> selectedQueueDocumentId ?: documentId
    is DocumentReviewState.Error -> defaultDocumentId
}
