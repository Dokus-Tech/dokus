package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.annotation.ExperimentalFlowMVIAPI
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.delegate.delegate
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import pro.respawn.flowmvi.plugins.whileSubscribed
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.features.cashflow.presentation.review.mvi.payment.DocumentPaymentAction
import tech.dokus.features.cashflow.presentation.review.mvi.payment.DocumentPaymentContainer
import tech.dokus.features.cashflow.presentation.review.mvi.payment.DocumentPaymentIntent
import tech.dokus.features.cashflow.presentation.review.mvi.preview.DocumentPreviewAction
import tech.dokus.features.cashflow.presentation.review.mvi.preview.DocumentPreviewContainer
import tech.dokus.features.cashflow.presentation.review.mvi.preview.DocumentPreviewIntent
import tech.dokus.features.cashflow.presentation.review.route.toDocQueueItem
import tech.dokus.features.cashflow.presentation.review.route.toListFilter
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
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

internal typealias DocumentReviewCtx = PipelineContext<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction>

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
internal class DocumentReviewContainer(
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
    private val paymentContainer: DocumentPaymentContainer,
    private val previewContainer: DocumentPreviewContainer,
    private val initialDocumentId: DocumentId,
    private val routeContext: DocumentReviewRouteContext?,
) : Container<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> {

    private val logger = Logger.forClass<DocumentReviewContainer>()

    private val reducer = DocumentReviewReducer(
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
    override val store: Store<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> =
        store(
            DocumentReviewState(
                queueState = routeContext?.let { DocumentReviewQueueState(context = it) },
                selectedQueueDocumentId = routeContext?.let { initialDocumentId },
            )
        ) {
            val paymentState by delegate(paymentContainer.store) { paymentAction ->
                when (paymentAction) {
                    is DocumentPaymentAction.PaymentRecorded -> intent(DocumentReviewIntent.Refresh)
                    is DocumentPaymentAction.AutoPaymentUndone -> intent(DocumentReviewIntent.Refresh)
                    is DocumentPaymentAction.ShowError ->
                        action(DocumentReviewAction.ShowError(paymentAction.error))
                    is DocumentPaymentAction.NavigateToCashflowEntry ->
                        action(DocumentReviewAction.NavigateToCashflowEntry(paymentAction.entryId))
                }
            }

            val previewState by delegate(previewContainer.store) { previewAction ->
                when (previewAction) {
                    is DocumentPreviewAction.ShowError ->
                        action(DocumentReviewAction.ShowError(previewAction.error))
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
                    is DocumentReviewIntent.SelectQueueDocument -> {
                        updateState { copy(selectedQueueDocumentId = intent.documentId) }
                        with(reducer) {
                            handleLoadDocument(intent.documentId)
                        }
                    }
                    is DocumentReviewIntent.LoadMoreQueue -> {
                        loadQueuePage(reset = false)
                    }
                    DocumentReviewIntent.RefreshQueue -> {
                        loadQueuePage(reset = true)
                    }
                    DocumentReviewIntent.HandleRemoteDeletion -> {
                        action(DocumentReviewAction.NavigateBack)
                    }
                    else -> {
                        dispatchToReducer(intent)
                    }
                }
            }
        }

    private suspend fun DocumentReviewCtx.dispatchToReducer(intent: DocumentReviewIntent) {
        when (intent) {
            // === Preview (delegated to child store) ===
            is DocumentReviewIntent.Preview -> {
                previewContainer.store.intent(intent.intent)
            }

            // === Payment (delegated to child store) ===
            is DocumentReviewIntent.Payment -> {
                paymentContainer.store.intent(intent.intent)
            }

            // === All other intents go to reducer ===
            else -> with(reducer) {
                when (intent) {
                    // === Data Loading ===
                    is DocumentReviewIntent.LoadDocument -> handleLoadDocument(intent.documentId)
                    is DocumentReviewIntent.Refresh -> handleRefresh()
                    is DocumentReviewIntent.ApplyRemoteSnapshot -> handleApplyRemoteSnapshot(intent.record)

                    // === Contact Sheet ===
                    is DocumentReviewIntent.OpenContactSheet -> handleOpenContactSheet()
                    is DocumentReviewIntent.CloseContactSheet -> handleCloseContactSheet()
                    is DocumentReviewIntent.UpdateContactSheetSearch ->
                        handleUpdateContactSheetSearch(intent.query)

                    // === Contact Selection (with backend persist) ===
                    is DocumentReviewIntent.SelectContact -> handleSelectContact(intent.contactId)
                    is DocumentReviewIntent.AcceptSuggestedContact -> handleAcceptSuggestedContact()
                    is DocumentReviewIntent.ClearSelectedContact -> handleClearSelectedContact()
                    is DocumentReviewIntent.ContactCreated -> handleContactCreated(intent.contactId)
                    is DocumentReviewIntent.SetPendingCreation -> handleSetPendingCreation()

                    // === Provenance ===
                    is DocumentReviewIntent.SelectFieldForProvenance ->
                        handleSelectFieldForProvenance(intent.fieldPath)

                    // === Actions ===
                    is DocumentReviewIntent.Confirm -> handleConfirm()
                    is DocumentReviewIntent.ViewCashflowEntry -> handleViewCashflowEntry()
                    is DocumentReviewIntent.ViewEntity -> handleViewEntity()

                    // === Reject Dialog ===
                    is DocumentReviewIntent.ShowRejectDialog -> handleShowRejectDialog()
                    is DocumentReviewIntent.DismissRejectDialog -> handleDismissRejectDialog()
                    is DocumentReviewIntent.SelectRejectReason -> handleSelectRejectReason(intent.reason)
                    is DocumentReviewIntent.UpdateRejectNote -> handleUpdateRejectNote(intent.note)
                    is DocumentReviewIntent.ConfirmReject -> handleConfirmReject()

                    // === Feedback Dialog ===
                    is DocumentReviewIntent.ShowFeedbackDialog -> handleShowFeedbackDialog()
                    is DocumentReviewIntent.DismissFeedbackDialog -> handleDismissFeedbackDialog()
                    is DocumentReviewIntent.SelectFeedbackCategory ->
                        handleSelectFeedbackCategory(intent.category)
                    is DocumentReviewIntent.UpdateFeedbackText -> handleUpdateFeedbackText(intent.text)
                    is DocumentReviewIntent.SubmitFeedback -> handleSubmitFeedback()
                    DocumentReviewIntent.RequestAmendment -> handleRequestAmendment()

                    // === Failed Analysis ===
                    is DocumentReviewIntent.RetryAnalysis -> handleRetryAnalysis()
                    is DocumentReviewIntent.DismissFailureBanner -> handleDismissFailureBanner()
                    is DocumentReviewIntent.ResolvePossibleMatchSame -> handleResolvePossibleMatchSame()
                    is DocumentReviewIntent.ResolvePossibleMatchDifferent ->
                        handleResolvePossibleMatchDifferent()
                    is DocumentReviewIntent.ToggleBankStatementTransaction ->
                        handleToggleBankStatementTransaction(intent.index)

                    // === Manual Document Type Selection ===
                    is DocumentReviewIntent.SelectDocumentType -> handleSelectDocumentType(intent.type)
                    is DocumentReviewIntent.SelectDirection -> handleSelectDirection(intent.direction)

                    // === Inline Field Editing ===
                    is DocumentReviewIntent.UpdateField -> handleUpdateField(intent.field, intent.value)

                    // === Unconfirm ===
                    DocumentReviewIntent.RequestUnconfirm -> handleUnconfirm()

                    // handled before reducer or forwarded to child
                    is DocumentReviewIntent.Preview,
                    is DocumentReviewIntent.Payment,
                    is DocumentReviewIntent.SelectQueueDocument,
                    DocumentReviewIntent.LoadMoreQueue,
                    DocumentReviewIntent.RefreshQueue,
                    DocumentReviewIntent.HandleRemoteDeletion -> Unit
                }
            }
        }
    }

    private suspend fun DocumentReviewCtx.loadQueuePage(reset: Boolean) {
        val context = routeContext ?: return
        withState {
            val currentQueueState = queueState ?: DocumentReviewQueueState(context = context)
            if (!reset && (currentQueueState.isLoading || currentQueueState.isLoadingMore || !currentQueueState.hasMore)) {
                return@withState
            }
        }

        val nextPage: Int
        val existingItems: List<DocQueueItem>

        withState {
            val currentQueueState = queueState ?: DocumentReviewQueueState(context = context)
            if (reset) {
                updateState {
                    val base = queueState ?: DocumentReviewQueueState(context = context)
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
                    val base = queueState ?: DocumentReviewQueueState(context = context)
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
            val qs = queueState ?: DocumentReviewQueueState(context = context)
            page = if (reset) 0 else qs.currentPage + 1
            items = if (reset) emptyList() else qs.items
        }
        nextPage = page
        existingItems = items

        loadDocumentRecords(
            page = nextPage,
            pageSize = DocumentsState.PAGE_SIZE,
            filter = context.filter.toListFilter(),
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
                        queueState = (queueState ?: DocumentReviewQueueState(context = context)).copy(
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
                        queueState = (queueState ?: DocumentReviewQueueState(context = context)).copy(
                            isLoading = false,
                            isLoadingMore = false,
                        ),
                    )
                }
            },
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
        val selectedExisting = existingItems.firstOrNull { it.id == selectedDocumentId } ?: return incomingItems
        return listOf(selectedExisting) + incomingItems
    }
}
