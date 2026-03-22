package tech.dokus.features.cashflow.presentation.detail

import pro.respawn.flowmvi.annotation.ExperimentalFlowMVIAPI
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.delegate.delegate
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.features.cashflow.presentation.detail.mvi.payment.DocumentPaymentAction
import tech.dokus.features.cashflow.presentation.detail.mvi.payment.DocumentPaymentContainer
import tech.dokus.features.cashflow.presentation.detail.mvi.preview.DocumentPreviewAction
import tech.dokus.features.cashflow.presentation.detail.mvi.preview.DocumentPreviewContainer
import tech.dokus.features.cashflow.presentation.detail.route.toDocQueueItem
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.DownloadDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.ResolveDocumentMatchReviewUseCase
import tech.dokus.features.cashflow.usecases.UnconfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftContactUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.app.shell.DocQueueItem
import tech.dokus.foundation.platform.Logger
import tech.dokus.navigation.destinations.CashFlowDestination

internal typealias DocumentDetailCtx = PipelineContext<DocumentDetailState, DocumentDetailIntent, DocumentDetailAction>

/**
 * Container for the Document Review screen using FlowMVI.
 *
 * Manages document review workflow:
 * - Loading document processing details with AI extraction
 * - Editing extracted fields with change tracking
 * - Saving drafts for later review
 * - Confirming documents (creates entities)
 * - Rejecting documents
 * - Navigation to document chat
 *
 * Audit Trail:
 * - Original AI draft is preserved on first edit
 * - Each correction is tracked with timestamp
 * - Draft version increments on each save
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
@Suppress("LongParameterList") // Explicit use case wiring keeps intent boundaries clear.
internal class DocumentDetailContainer(
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val updateDocumentDraft: UpdateDocumentDraftUseCase,
    private val updateDocumentDraftContact: UpdateDocumentDraftContactUseCase,
    private val confirmDocument: ConfirmDocumentUseCase,
    private val unconfirmDocument: UnconfirmDocumentUseCase,
    private val rejectDocument: RejectDocumentUseCase,
    private val reprocessDocument: ReprocessDocumentUseCase,
    private val resolveDocumentMatchReview: ResolveDocumentMatchReviewUseCase,
    private val getContact: GetContactUseCase,
    private val loadDocumentRecords: LoadDocumentRecordsUseCase,
    private val downloadDocument: DownloadDocumentUseCase,
    private val paymentContainer: DocumentPaymentContainer,
    private val previewContainer: DocumentPreviewContainer,
    private val initialDocumentId: DocumentId,
    private val queueContext: CashFlowDestination.DocumentDetailQueueContext,
) : Container<DocumentDetailState, DocumentDetailIntent, DocumentDetailAction> {

    private val logger = Logger.forClass<DocumentDetailContainer>()

    private val reducer = DocumentDetailReducer(
        getDocumentRecord = getDocumentRecord,
        updateDocumentDraft = updateDocumentDraft,
        updateDocumentDraftContact = updateDocumentDraftContact,
        confirmDocument = confirmDocument,
        unconfirmDocument = unconfirmDocument,
        rejectDocument = rejectDocument,
        reprocessDocument = reprocessDocument,
        resolveDocumentMatchReview = resolveDocumentMatchReview,
        getContact = getContact,
        logger = logger,
    )

    @OptIn(ExperimentalFlowMVIAPI::class)
    override val store: Store<DocumentDetailState, DocumentDetailIntent, DocumentDetailAction> =
        store(
            DocumentDetailState(
                queueState = DocumentDetailQueueState(context = queueContext),
                selectedQueueDocumentId = initialDocumentId,
            )
        ) {
            val paymentState by delegate(paymentContainer.store) { paymentAction ->
                when (paymentAction) {
                    is DocumentPaymentAction.PaymentRecorded -> intent(DocumentDetailIntent.Refresh)
                    is DocumentPaymentAction.AutoPaymentUndone -> intent(DocumentDetailIntent.Refresh)
                    is DocumentPaymentAction.ShowError ->
                        updateState { copy(actionError = paymentAction.error) }

                    is DocumentPaymentAction.NavigateToCashflowEntry ->
                        action(DocumentDetailAction.NavigateToCashflowEntry(paymentAction.entryId))
                }
            }

            val previewState by delegate(previewContainer.store) { previewAction ->
                when (previewAction) {
                    is DocumentPreviewAction.ShowError ->
                        updateState { copy(actionError = previewAction.error) }
                }
            }

            whileSubscribed {
                paymentState.collect { childState ->
                    updateState {
                        copy(
                            paymentSheetState = childState.paymentSheetState,
                            cashflowEntryState = childState.cashflowEntryState,
                            autoPaymentStatus = childState.autoPaymentStatus,
                            isUndoingAutoPayment = childState.isUndoingAutoPayment,
                            confirmedCashflowEntryId = childState.confirmedCashflowEntryId,
                        )
                    }
                }
            }

            whileSubscribed {
                previewState.collect { childState ->
                    updateState {
                        copy(
                            previewState = childState.previewState,
                            incomingPreviewState = childState.incomingPreviewState,
                            sourceViewerState = childState.sourceViewerState,
                        )
                    }
                }
            }

            init {
                with(reducer) {
                    handleLoadDocument(initialDocumentId)
                }
                loadQueuePage(reset = true)
            }

            reduce { intent ->
                when (intent) {
                    is DocumentDetailIntent.SelectQueueDocument -> {
                        updateState { copy(selectedQueueDocumentId = intent.documentId) }
                        with(reducer) {
                            handleLoadDocument(intent.documentId)
                        }
                    }

                    is DocumentDetailIntent.LoadMoreQueue -> {
                        loadQueuePage(reset = false)
                    }

                    DocumentDetailIntent.RefreshQueue -> {
                        loadQueuePage(reset = true)
                    }

                    DocumentDetailIntent.HandleRemoteDeletion -> {
                        action(DocumentDetailAction.NavigateBack)
                    }

                    DocumentDetailIntent.DismissActionError -> {
                        updateState { copy(actionError = null) }
                    }

                    DocumentDetailIntent.DownloadPdf -> {
                        withState {
                            val docId = documentId ?: return@withState
                            val filename = documentRecord?.document?.filename ?: "document.pdf"
                            updateState { copy(isDownloading = true) }
                            downloadDocument(docId, filename).fold(
                                onSuccess = {
                                    logger.i { "Document $docId downloaded as $filename" }
                                },
                                onFailure = { error ->
                                    logger.e(error) { "Failed to download document $docId" }
                                },
                            )
                            updateState { copy(isDownloading = false) }
                        }
                    }

                    else -> {
                        dispatchToReducer(intent)
                    }
                }
            }
        }

    private suspend fun DocumentDetailCtx.dispatchToReducer(intent: DocumentDetailIntent) {
        when (intent) {
            // === Preview (delegated to child store) ===
            is DocumentDetailIntent.Preview -> {
                previewContainer.store.intent(intent.intent)
            }

            // === Payment (delegated to child store) ===
            is DocumentDetailIntent.Payment -> {
                paymentContainer.store.intent(intent.intent)
            }

            // === All other intents go to reducer ===
            else -> with(reducer) {
                when (intent) {
                    // === Data Loading ===
                    is DocumentDetailIntent.LoadDocument -> handleLoadDocument(intent.documentId)
                    is DocumentDetailIntent.Refresh -> handleRefresh()
                    is DocumentDetailIntent.ApplyRemoteSnapshot -> handleApplyRemoteSnapshot(intent.record)

                    // === Contact Sheet ===
                    is DocumentDetailIntent.OpenContactSheet -> handleOpenContactSheet()
                    is DocumentDetailIntent.CloseContactSheet -> handleCloseContactSheet()
                    is DocumentDetailIntent.UpdateContactSheetSearch ->
                        handleUpdateContactSheetSearch(intent.query)

                    // === Contact Selection (with backend persist) ===
                    is DocumentDetailIntent.SelectContact -> handleSelectContact(intent.contactId)
                    is DocumentDetailIntent.AcceptSuggestedContact -> handleAcceptSuggestedContact()
                    is DocumentDetailIntent.ClearSelectedContact -> handleClearSelectedContact()
                    is DocumentDetailIntent.ContactCreated -> handleContactCreated(intent.contactId)
                    is DocumentDetailIntent.SetPendingCreation -> handleSetPendingCreation()

                    // === Provenance ===
                    is DocumentDetailIntent.SelectFieldForProvenance ->
                        handleSelectFieldForProvenance(intent.fieldPath)

                    // === Actions ===
                    is DocumentDetailIntent.Confirm -> handleConfirm()
                    is DocumentDetailIntent.ViewCashflowEntry -> handleViewCashflowEntry()
                    is DocumentDetailIntent.ViewEntity -> handleViewEntity()

                    // === Reject Dialog ===
                    is DocumentDetailIntent.ShowRejectDialog -> handleShowRejectDialog()
                    is DocumentDetailIntent.DismissRejectDialog -> handleDismissRejectDialog()
                    is DocumentDetailIntent.SelectRejectReason -> handleSelectRejectReason(intent.reason)
                    is DocumentDetailIntent.UpdateRejectNote -> handleUpdateRejectNote(intent.note)
                    is DocumentDetailIntent.ConfirmReject -> handleConfirmReject()

                    // === Feedback Dialog ===
                    is DocumentDetailIntent.ShowFeedbackDialog -> handleShowFeedbackDialog()
                    is DocumentDetailIntent.DismissFeedbackDialog -> handleDismissFeedbackDialog()
                    is DocumentDetailIntent.SelectFeedbackCategory ->
                        handleSelectFeedbackCategory(intent.category)

                    is DocumentDetailIntent.UpdateFeedbackText -> handleUpdateFeedbackText(intent.text)
                    is DocumentDetailIntent.SubmitFeedback -> handleSubmitFeedback()
                    DocumentDetailIntent.RequestAmendment -> handleRequestAmendment()

                    // === Failed Analysis ===
                    is DocumentDetailIntent.RetryAnalysis -> handleRetryAnalysis()
                    is DocumentDetailIntent.DismissFailureBanner -> handleDismissFailureBanner()
                    is DocumentDetailIntent.ResolvePossibleMatchSame -> handleResolvePossibleMatchSame()
                    is DocumentDetailIntent.ResolvePossibleMatchDifferent ->
                        handleResolvePossibleMatchDifferent()

                    is DocumentDetailIntent.ToggleBankStatementTransaction ->
                        handleToggleBankStatementTransaction(intent.index)

                    // === Manual Document Type Selection ===
                    is DocumentDetailIntent.SelectDocumentType -> handleSelectDocumentType(intent.type)
                    is DocumentDetailIntent.SelectDirection -> handleSelectDirection(intent.direction)

                    // === Inline Field Editing ===
                    is DocumentDetailIntent.UpdateField -> handleUpdateField(
                        intent.field,
                        intent.value
                    )

                    // === Unconfirm ===
                    DocumentDetailIntent.RequestUnconfirm -> handleUnconfirm()

                    // handled before reducer or forwarded to child
                    is DocumentDetailIntent.Preview,
                    is DocumentDetailIntent.Payment,
                    is DocumentDetailIntent.SelectQueueDocument,
                    DocumentDetailIntent.LoadMoreQueue,
                    DocumentDetailIntent.RefreshQueue,
                    DocumentDetailIntent.HandleRemoteDeletion,
                    DocumentDetailIntent.DismissActionError,
                    DocumentDetailIntent.DownloadPdf -> Unit
                }
            }
        }
    }

    private suspend fun DocumentDetailCtx.loadQueuePage(reset: Boolean) {
        withState {
            val currentQueueState = queueState ?: DocumentDetailQueueState(context = queueContext)
            if (!reset && (currentQueueState.isLoading || currentQueueState.isLoadingMore || !currentQueueState.hasMore)) {
                return@withState
            }
        }

        val nextPage: Int
        val existingItems: List<DocQueueItem>

        withState {
            val currentQueueState = queueState ?: DocumentDetailQueueState(context = queueContext)
            if (reset) {
                updateState {
                    val base = queueState ?: DocumentDetailQueueState(context = queueContext)
                    copy(
                        queueState = base.copy(
                            items = emptyList(),
                            isLoading = true,
                            isLoadingMore = false,
                        )
                    )
                }
            } else {
                updateState {
                    val base = queueState ?: DocumentDetailQueueState(context = queueContext)
                    copy(
                        queueState = base.copy(
                            isLoading = false,
                            isLoadingMore = true,
                        )
                    )
                }
            }
        }

        // Read the computed values after state update
        var page = 0
        var items = emptyList<DocQueueItem>()
        withState {
            val qs = queueState ?: DocumentDetailQueueState(context = queueContext)
            page = if (reset) 0 else qs.currentPage + 1
            items = if (reset) emptyList() else qs.items
        }
        nextPage = page
        existingItems = items

        loadDocumentRecordsBySource(
            source = queueContext,
            page = nextPage,
            pageSize = DocumentsState.PAGE_SIZE,
        ).fold(
            onSuccess = { response ->
                val loadedItems = response.items.map { it.toDocQueueItem() }
                var selectedDocumentId: DocumentId = initialDocumentId
                withState {
                    selectedDocumentId = this.selectedQueueDocumentId ?: initialDocumentId
                }
                val mergedItems = if (reset) {
                    preserveSelectedQueueItem(
                        existingItems = existingItems,
                        incomingItems = loadedItems,
                        selectedDocumentId = selectedDocumentId,
                    )
                } else {
                    mergeQueueItems(existingItems = existingItems, incomingItems = loadedItems)
                }
                updateState {
                    copy(
                        queueState = (queueState
                            ?: DocumentDetailQueueState(context = queueContext)).copy(
                            items = mergedItems,
                            currentPage = nextPage,
                            hasMore = response.hasMore,
                            isLoading = false,
                            isLoadingMore = false,
                        ),
                    )
                }
                // Ensure selected document ID
                withState {
                    if (selectedQueueDocumentId == null) {
                        updateState { copy(selectedQueueDocumentId = initialDocumentId) }
                    }
                }
            },
            onFailure = { error ->
                logger.w(error) { "Failed to load document review queue page=$nextPage" }
                updateState {
                    copy(
                        queueState = (queueState
                            ?: DocumentDetailQueueState(context = queueContext)).copy(
                            isLoading = false,
                            isLoadingMore = false,
                        ),
                    )
                }
            },
        )
    }

    private suspend fun loadDocumentRecordsBySource(
        source: CashFlowDestination.DocumentDetailQueueContext,
        page: Int,
        pageSize: Int,
    ) = when (source) {
        is CashFlowDestination.DocumentDetailQueueContext.DocumentList -> loadDocumentRecords(
            page = page,
            pageSize = pageSize,
            filter = source.filter
        )

        is CashFlowDestination.DocumentDetailQueueContext.Contact -> loadDocumentRecords(
            page = page,
            pageSize = pageSize,
            contactId = source.contactId
        )

        is CashFlowDestination.DocumentDetailQueueContext.Search -> loadDocumentRecords(
            page = page,
            pageSize = pageSize
        )

        is CashFlowDestination.DocumentDetailQueueContext.Recent -> loadDocumentRecords(
            page = page,
            pageSize = pageSize
        )
    }

    private fun mergeQueueItems(
        existingItems: List<DocQueueItem>,
        incomingItems: List<DocQueueItem>,
    ): List<DocQueueItem> {
        if (existingItems.isEmpty()) return incomingItems
        if (incomingItems.isEmpty()) return existingItems
        val existingIds = existingItems.asSequence().map { it.id }.toSet()
        return existingItems + incomingItems.filter { it.id !in existingIds }
    }

    private fun preserveSelectedQueueItem(
        existingItems: List<DocQueueItem>,
        incomingItems: List<DocQueueItem>,
        selectedDocumentId: DocumentId,
    ): List<DocQueueItem> {
        if (incomingItems.any { it.id == selectedDocumentId }) return incomingItems
        val selectedExisting =
            existingItems.firstOrNull { it.id == selectedDocumentId } ?: return incomingItems
        return listOf(selectedExisting) + incomingItems
    }
}
