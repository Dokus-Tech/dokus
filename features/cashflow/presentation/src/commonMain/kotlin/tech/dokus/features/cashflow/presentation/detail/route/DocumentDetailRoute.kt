package tech.dokus.features.cashflow.presentation.detail.route

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_confirm
import tech.dokus.aura.resources.cashflow_discard_changes_message
import tech.dokus.aura.resources.cashflow_discard_changes_title
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentRecordStreamEvent
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.presentation.documents.route.DOCUMENTS_REFRESH_REQUIRED_RESULT_KEY
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailAction
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailContainer
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailIntent
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailQueueState
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.components.ContactEditSheet
import tech.dokus.features.cashflow.presentation.detail.components.DocumentDetailDesktopSplit
import tech.dokus.features.cashflow.presentation.detail.components.FeedbackDialog
import tech.dokus.features.cashflow.presentation.detail.components.RecordPaymentDialog
import tech.dokus.features.cashflow.presentation.detail.components.RejectDocumentDialog
import tech.dokus.features.cashflow.presentation.detail.models.DocumentUiData
import tech.dokus.features.cashflow.presentation.detail.mvi.payment.DocumentPaymentIntent
import tech.dokus.features.cashflow.presentation.detail.screen.DocumentDetailScreen
import tech.dokus.features.cashflow.usecases.ObserveDocumentCollectionChangesUseCase
import tech.dokus.features.cashflow.usecases.ObserveDocumentRecordEventsUseCase
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.shell.LocalUserAccessContext
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.ContactCreateOrigin
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo

private const val CONTACT_RESULT_KEY = "documentDetail_contactId"

@Composable
internal fun DocumentDetailRoute(
    route: CashFlowDestination.DocumentDetail,
    container: DocumentDetailContainer = container {
        parametersOf(
            DocumentId.parse(route.documentId),
            route.queueSource,
        )
    },
    listContacts: ListContactsUseCase = koinInject(),
    observeDocumentRecordEvents: ObserveDocumentRecordEventsUseCase = koinInject(),
    observeDocumentCollectionChanges: ObserveDocumentCollectionChangesUseCase = koinInject(),
) {
    val accessContext = LocalUserAccessContext.current
    val isAccountantReadOnly = accessContext.isBookkeeperConsoleDrillDown
    val navController = LocalNavController.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val initialDocumentId = remember(route.documentId) { DocumentId.parse(route.documentId) }
    val backLabel = route.queueSource.localized

    fun markDocumentsRefreshRequired() {
        navController.previousBackStackEntry
            ?.savedStateHandle
            ?.set(DOCUMENTS_REFRESH_REQUIRED_RESULT_KEY, true)
    }

    fun dispatchIntent(intent: DocumentDetailIntent) {
        if (isAccountantReadOnly && intent.isBlockedForAccountantReadOnly()) return
        container.store.intent(intent)
    }

    val pendingContactId = backStackEntry?.savedStateHandle?.get<String>(CONTACT_RESULT_KEY)

    LaunchedEffect(pendingContactId) {
        if (pendingContactId != null) {
            dispatchIntent(
                DocumentDetailIntent.ContactCreated(ContactId.parse(pendingContactId))
            )
            backStackEntry?.savedStateHandle?.remove<String>(CONTACT_RESULT_KEY)
        }
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var contactsState by remember { mutableStateOf<DokusState<List<ContactDto>>>(DokusState.idle()) }

    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is DocumentDetailAction.NavigateBack -> {
                markDocumentsRefreshRequired()
                navController.popBackStack()
            }
            is DocumentDetailAction.NavigateToEntity -> {
                markDocumentsRefreshRequired()
                navController.popBackStack()
            }
            is DocumentDetailAction.NavigateToCashflowEntry -> {
                navController.navigateTo(
                    CashFlowDestination.CashFlowOverview(action.entryId.toString())
                )
            }
        }
    }

    val detailStreamDocumentId = state.documentId

    LaunchedEffect(detailStreamDocumentId) {
        val activeDocumentId = detailStreamDocumentId ?: return@LaunchedEffect
        observeDocumentRecordEvents(activeDocumentId).collect { event ->
            when (event) {
                is DocumentRecordStreamEvent.Snapshot -> {
                    dispatchIntent(DocumentDetailIntent.ApplyRemoteSnapshot(event.record))
                }
                DocumentRecordStreamEvent.Deleted -> {
                    dispatchIntent(DocumentDetailIntent.HandleRemoteDeletion)
                }
            }
        }
    }

    LaunchedEffect(route.queueSource) {
        observeDocumentCollectionChanges().collect {
            dispatchIntent(DocumentDetailIntent.RefreshQueue)
        }
    }

    val isLargeScreen = LocalScreenSize.isLarge
    val navigateBack: () -> Unit = {
        markDocumentsRefreshRequired()
        navController.popBackStack()
    }
    val requestBackNavigation: () -> Unit = {
        if (state.hasContent && state.hasUnsyncedLocalChanges) {
            showDiscardDialog = true
        } else {
            navigateBack()
        }
    }

    // Load contacts when contact sheet opens
    LaunchedEffect(state.showContactSheet) {
        if (state.showContactSheet && contactsState !is DokusState.Success) {
            contactsState = DokusState.loading()
            listContacts(limit = 100).fold(
                onSuccess = { contacts ->
                    contactsState = DokusState.success(contacts)
                },
                onFailure = { error ->
                    contactsState = DokusState.error(
                        exception = DokusException.Unknown(error),
                        retryHandler = { /* no retry */ },
                    )
                },
            )
        }
    }

    val reviewContent: @Composable () -> Unit = {
        DocumentDetailScreen(
            state = state,
            isLargeScreen = isLargeScreen,
            isAccountantReadOnly = isAccountantReadOnly,
            onIntent = { dispatchIntent(it) },
            onBackClick = requestBackNavigation,
            backLabel = backLabel,
            onOpenSource = { sourceId ->
                val activeDocumentId = state.documentId?.toString()
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
                dispatchIntent(DocumentDetailIntent.OpenContactSheet)
            },
            onCreateContact = { resolvedContact ->
                if (isAccountantReadOnly) return@DocumentDetailScreen
                dispatchIntent(DocumentDetailIntent.SetPendingCreation)
                val detected = resolvedContact as? ResolvedContact.Detected
                navController.navigateTo(
                    ContactsDestination.CreateContact(
                        prefillCompanyName = when (resolvedContact) {
                            is ResolvedContact.Linked -> resolvedContact.name
                            is ResolvedContact.Suggested -> resolvedContact.name
                            is ResolvedContact.Detected -> resolvedContact.name
                            is ResolvedContact.Unknown -> null
                        },
                        prefillVat = detected?.vatNumber,
                        prefillAddress = detected?.address,
                        origin = ContactCreateOrigin.DocumentDetail.name,
                    )
                )
            },
        )
    }

    // Mark documents list as needing refresh when confirm succeeds
    LaunchedEffect(state.documentStatus) {
        if (state.isDocumentConfirmed) {
            markDocumentsRefreshRequired()
        }
    }

    val queueState = state.queueStateOrNull()
    val desktopQueueState = queueState?.takeIf { isLargeScreen && it.items.isNotEmpty() }
    val selectedDocumentId = state.selectedQueueDocumentIdOrDefault(initialDocumentId)
    val selectedDoc = queueState?.items?.firstOrNull { it.id == selectedDocumentId }

    Box(modifier = Modifier.fillMaxSize()) {
        if (desktopQueueState != null) {
            DocumentDetailDesktopSplit(
                documents = desktopQueueState.items,
                selectedDocumentId = selectedDocumentId,
                selectedDoc = selectedDoc,
                hasMore = desktopQueueState.hasMore,
                isLoadingMore = desktopQueueState.isLoadingMore,
                onSelectDocument = { selectedDocumentIdCandidate ->
                    if (selectedDocumentIdCandidate != selectedDocumentId) {
                        dispatchIntent(
                            DocumentDetailIntent.SelectQueueDocument(selectedDocumentIdCandidate)
                        )
                    }
                },
                onLoadMore = {
                    dispatchIntent(DocumentDetailIntent.LoadMoreQueue)
                },
                onExit = requestBackNavigation,
                onDownloadPdf = { dispatchIntent(DocumentDetailIntent.DownloadPdf) },
                downloadState = state.downloadState,
                hasContent = state.hasContent,
                backLabel = backLabel,
                content = reviewContent,
            )
        } else {
            reviewContent()
        }
    }

    // Contact Edit Sheet
    if (state.hasContent) {
        ContactEditSheet(
            isVisible = state.showContactSheet && !isAccountantReadOnly,
            onDismiss = { dispatchIntent(DocumentDetailIntent.CloseContactSheet) },
            suggestions = state.contactSuggestions,
            contactsState = contactsState,
            selectedContactId = (state.effectiveContact as? ResolvedContact.Linked)?.contactId
                ?: (state.effectiveContact as? ResolvedContact.Suggested)?.contactId,
            searchQuery = state.contactSheetSearchQuery,
            onSearchQueryChange = { query ->
                dispatchIntent(DocumentDetailIntent.UpdateContactSheetSearch(query))
            },
            onSelectContact = { contactId ->
                dispatchIntent(DocumentDetailIntent.SelectContact(contactId))
                dispatchIntent(DocumentDetailIntent.CloseContactSheet)
            },
            onCreateNewContact = {
                // Close sheet and navigate to contact creation
                if (isAccountantReadOnly) return@ContactEditSheet
                dispatchIntent(DocumentDetailIntent.CloseContactSheet)
                dispatchIntent(DocumentDetailIntent.SetPendingCreation)
                val contact = state.effectiveContact
                val detected = contact as? ResolvedContact.Detected
                navController.navigateTo(
                    ContactsDestination.CreateContact(
                        prefillCompanyName = when (contact) {
                            is ResolvedContact.Linked -> contact.name
                            is ResolvedContact.Suggested -> contact.name
                            is ResolvedContact.Detected -> contact.name
                            is ResolvedContact.Unknown -> null
                        },
                        prefillVat = detected?.vatNumber,
                        prefillAddress = detected?.address,
                        origin = ContactCreateOrigin.DocumentDetail.name,
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
    state.feedbackDialogState?.let { dialogState ->
        if (isAccountantReadOnly) return@let
        FeedbackDialog(
            state = dialogState,
            onCategorySelected = { category ->
                dispatchIntent(DocumentDetailIntent.SelectFeedbackCategory(category))
            },
            onFeedbackChanged = { text ->
                dispatchIntent(DocumentDetailIntent.UpdateFeedbackText(text))
            },
            onSubmit = {
                dispatchIntent(DocumentDetailIntent.SubmitFeedback)
            },
            onRejectInstead = {
                dispatchIntent(DocumentDetailIntent.DismissFeedbackDialog)
                dispatchIntent(DocumentDetailIntent.ShowRejectDialog)
            },
            onDismiss = {
                dispatchIntent(DocumentDetailIntent.DismissFeedbackDialog)
            },
        )
    }

    // Reject document dialog (state-driven)
    state.rejectDialogState?.let { dialogState ->
        if (isAccountantReadOnly) return@let
        RejectDocumentDialog(
            state = dialogState,
            onReasonSelected = { reason ->
                dispatchIntent(DocumentDetailIntent.SelectRejectReason(reason))
            },
            onNoteChanged = { note ->
                dispatchIntent(DocumentDetailIntent.UpdateRejectNote(note))
            },
            onConfirm = {
                dispatchIntent(DocumentDetailIntent.ConfirmReject)
            },
            onDismiss = {
                dispatchIntent(DocumentDetailIntent.DismissRejectDialog)
            },
        )
    }

    state.paymentSheetState?.let { paymentState ->
        if (isAccountantReadOnly) return@let
        val currencySign = when (val uiData = state.uiData) {
            is DocumentUiData.Invoice -> uiData.currencySign
            is DocumentUiData.CreditNote -> uiData.currencySign
            is DocumentUiData.Receipt -> uiData.currencySign
            else -> "\u20AC"
        }
        RecordPaymentDialog(
            sheetState = paymentState,
            currencySign = currencySign,
            onPaidAtChange = { paidAt ->
                container.store.intent(DocumentDetailIntent.Payment(DocumentPaymentIntent.UpdatePaymentPaidAt(paidAt)))
            },
            onAmountChange = { amount ->
                dispatchIntent(DocumentDetailIntent.Payment(DocumentPaymentIntent.UpdatePaymentAmountText(amount)))
            },
            onNoteChange = { note ->
                dispatchIntent(DocumentDetailIntent.Payment(DocumentPaymentIntent.UpdatePaymentNote(note)))
            },
            onOpenTransactionPicker = {
                dispatchIntent(DocumentDetailIntent.Payment(DocumentPaymentIntent.OpenPaymentTransactionPicker))
            },
            onCloseTransactionPicker = {
                dispatchIntent(DocumentDetailIntent.Payment(DocumentPaymentIntent.ClosePaymentTransactionPicker))
            },
            onSelectTransaction = { transactionId ->
                dispatchIntent(DocumentDetailIntent.Payment(DocumentPaymentIntent.SelectPaymentTransaction(transactionId)))
            },
            onClearSelectedTransaction = {
                dispatchIntent(DocumentDetailIntent.Payment(DocumentPaymentIntent.ClearPaymentTransactionSelection))
            },
            onSubmit = { dispatchIntent(DocumentDetailIntent.Payment(DocumentPaymentIntent.SubmitPayment)) },
            onDismiss = { dispatchIntent(DocumentDetailIntent.Payment(DocumentPaymentIntent.ClosePaymentSheet)) },
        )
    }
}

private fun DocumentDetailIntent.isBlockedForAccountantReadOnly(): Boolean = when (this) {
    is DocumentDetailIntent.Payment,
    is DocumentDetailIntent.SelectContact,
    is DocumentDetailIntent.AcceptSuggestedContact,
    is DocumentDetailIntent.ClearSelectedContact,
    is DocumentDetailIntent.ContactCreated,
    is DocumentDetailIntent.SetPendingCreation,
    is DocumentDetailIntent.OpenContactSheet,
    is DocumentDetailIntent.CloseContactSheet,
    is DocumentDetailIntent.UpdateContactSheetSearch,
    is DocumentDetailIntent.Confirm,
    is DocumentDetailIntent.ShowRejectDialog,
    is DocumentDetailIntent.DismissRejectDialog,
    is DocumentDetailIntent.SelectRejectReason,
    is DocumentDetailIntent.UpdateRejectNote,
    is DocumentDetailIntent.ConfirmReject,
    is DocumentDetailIntent.ShowFeedbackDialog,
    is DocumentDetailIntent.DismissFeedbackDialog,
    is DocumentDetailIntent.SelectFeedbackCategory,
    is DocumentDetailIntent.UpdateFeedbackText,
    is DocumentDetailIntent.SubmitFeedback,
    is DocumentDetailIntent.RequestAmendment,
    is DocumentDetailIntent.RetryAnalysis,
    is DocumentDetailIntent.ResolvePossibleMatchSame,
    is DocumentDetailIntent.ResolvePossibleMatchDifferent,
    is DocumentDetailIntent.SelectDocumentType,
    is DocumentDetailIntent.SelectDirection,
    is DocumentDetailIntent.ToggleBankStatementTransaction -> true
    else -> false
}

private fun DocumentDetailState.queueStateOrNull(): DocumentDetailQueueState? = queueState

private fun DocumentDetailState.selectedQueueDocumentIdOrDefault(defaultDocumentId: DocumentId): DocumentId =
    selectedQueueDocumentId ?: documentId ?: defaultDocumentId

