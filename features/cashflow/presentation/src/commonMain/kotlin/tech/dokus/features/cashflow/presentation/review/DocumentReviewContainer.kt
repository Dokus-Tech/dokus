package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.features.cashflow.presentation.review.route.toDocQueueItem
import tech.dokus.features.cashflow.presentation.review.route.toListFilter
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetCashflowEntryUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourceContentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourcePagesUseCase
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import tech.dokus.features.cashflow.usecases.RecordCashflowPaymentUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.ResolveDocumentMatchReviewUseCase
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
    private val rejectDocument: RejectDocumentUseCase,
    private val reprocessDocument: ReprocessDocumentUseCase,
    private val resolveDocumentMatchReview: ResolveDocumentMatchReviewUseCase,
    private val getDocumentPages: GetDocumentPagesUseCase,
    private val getDocumentSourcePages: GetDocumentSourcePagesUseCase,
    private val getDocumentSourceContent: GetDocumentSourceContentUseCase,
    private val getCashflowEntry: GetCashflowEntryUseCase,
    private val recordCashflowPayment: RecordCashflowPaymentUseCase,
    private val getContact: GetContactUseCase,
    private val loadDocumentRecords: LoadDocumentRecordsUseCase,
    private val initialDocumentId: DocumentId,
    private val routeContext: DocumentReviewRouteContext?,
) : Container<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> {

    private val logger = Logger.forClass<DocumentReviewContainer>()
    private val reducer = DocumentReviewReducer(
        getDocumentRecord = getDocumentRecord,
        updateDocumentDraft = updateDocumentDraft,
        updateDocumentDraftContact = updateDocumentDraftContact,
        confirmDocument = confirmDocument,
        rejectDocument = rejectDocument,
        reprocessDocument = reprocessDocument,
        resolveDocumentMatchReview = resolveDocumentMatchReview,
        getDocumentPages = getDocumentPages,
        getDocumentSourcePages = getDocumentSourcePages,
        getDocumentSourceContent = getDocumentSourceContent,
        getCashflowEntry = getCashflowEntry,
        recordCashflowPayment = recordCashflowPayment,
        getContact = getContact,
        logger = logger,
    )

    override val store: Store<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> =
        store(
            DocumentReviewState.Loading(
                queueState = routeContext?.let { DocumentReviewQueueState(context = it) },
                selectedQueueDocumentId = routeContext?.let { initialDocumentId },
            )
        ) {
            init {
                with(reducer) {
                    handleLoadDocument(initialDocumentId)
                }
                loadQueuePage(reset = true)
            }

            reduce { intent ->
                when (intent) {
                    is DocumentReviewIntent.SelectQueueDocument -> {
                        updateSelectedQueueDocumentId(intent.documentId)
                        with(reducer) {
                            handleLoadDocument(intent.documentId)
                        }
                    }
                    is DocumentReviewIntent.LoadMoreQueue -> {
                        loadQueuePage(reset = false)
                    }
                    else -> {
                        dispatchToReducer(intent)
                    }
                }
            }
        }

    private suspend fun DocumentReviewCtx.dispatchToReducer(intent: DocumentReviewIntent) {
        with(reducer) {
            when (intent) {
                // === Data Loading ===
                is DocumentReviewIntent.LoadDocument -> handleLoadDocument(intent.documentId)
                is DocumentReviewIntent.Refresh -> handleRefresh()

                // === Preview ===
                is DocumentReviewIntent.LoadPreviewPages -> handleLoadPreviewPages()
                is DocumentReviewIntent.LoadMorePages -> handleLoadMorePages(intent.maxPages)
                is DocumentReviewIntent.RetryLoadPreview -> handleLoadPreviewPages()
                is DocumentReviewIntent.OpenSourceModal -> handleOpenSourceModal(intent.sourceId)
                is DocumentReviewIntent.CloseSourceModal -> handleCloseSourceModal()
                is DocumentReviewIntent.ToggleSourceTechnicalDetails -> handleToggleSourceTechnicalDetails()
                is DocumentReviewIntent.LoadCashflowEntry -> handleLoadCashflowEntry()
                is DocumentReviewIntent.OpenPaymentSheet -> handleOpenPaymentSheet()
                is DocumentReviewIntent.ClosePaymentSheet -> handleClosePaymentSheet()
                is DocumentReviewIntent.UpdatePaymentAmountText ->
                    handleUpdatePaymentAmountText(intent.text)
                is DocumentReviewIntent.UpdatePaymentPaidAt ->
                    handleUpdatePaymentPaidAt(intent.date)
                is DocumentReviewIntent.UpdatePaymentNote -> handleUpdatePaymentNote(intent.note)
                is DocumentReviewIntent.SubmitPayment -> handleSubmitPayment()

                // === Contact Sheet ===
                is DocumentReviewIntent.OpenContactSheet -> handleOpenContactSheet()
                is DocumentReviewIntent.CloseContactSheet -> handleCloseContactSheet()
                is DocumentReviewIntent.UpdateContactSheetSearch -> handleUpdateContactSheetSearch(intent.query)

                // === Contact Selection (with backend persist) ===
                is DocumentReviewIntent.SelectContact -> handleSelectContact(intent.contactId)
                is DocumentReviewIntent.AcceptSuggestedContact -> handleAcceptSuggestedContact()
                is DocumentReviewIntent.ClearSelectedContact -> handleClearSelectedContact()
                is DocumentReviewIntent.ContactCreated -> handleContactCreated(intent.contactId)
                is DocumentReviewIntent.SetCounterpartyIntent -> handleSetCounterpartyIntent(intent.intent)

                // === Line Items ===
                is DocumentReviewIntent.AddLineItem -> handleAddLineItem()
                is DocumentReviewIntent.UpdateLineItem -> handleUpdateLineItem(intent.index, intent.item)
                is DocumentReviewIntent.RemoveLineItem -> handleRemoveLineItem(intent.index)

                // === Provenance ===
                is DocumentReviewIntent.SelectFieldForProvenance -> handleSelectFieldForProvenance(
                    intent.fieldPath
                )

                // === Actions ===
                is DocumentReviewIntent.EnterEditMode -> handleEnterEditMode()
                is DocumentReviewIntent.CancelEditMode -> handleCancelEditMode()
                is DocumentReviewIntent.SaveDraft -> handleSaveDraft()
                is DocumentReviewIntent.DiscardChanges -> handleDiscardChanges()
                is DocumentReviewIntent.ConfirmDiscardChanges -> handleConfirmDiscardChanges()
                is DocumentReviewIntent.Confirm -> handleConfirm()
                is DocumentReviewIntent.OpenChat -> handleOpenChat()
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
                is DocumentReviewIntent.UpdateFeedbackText -> handleUpdateFeedbackText(intent.text)
                is DocumentReviewIntent.SubmitFeedback -> handleSubmitFeedback()
                DocumentReviewIntent.RequestAmendment -> handleRequestAmendment()

                // === Failed Analysis ===
                is DocumentReviewIntent.RetryAnalysis -> handleRetryAnalysis()
                is DocumentReviewIntent.DismissFailureBanner -> handleDismissFailureBanner()
                is DocumentReviewIntent.ResolvePossibleMatchSame -> handleResolvePossibleMatchSame()
                is DocumentReviewIntent.ResolvePossibleMatchDifferent -> handleResolvePossibleMatchDifferent()

                // === Manual Document Type Selection ===
                is DocumentReviewIntent.SelectDocumentType -> handleSelectDocumentType(intent.type)
                is DocumentReviewIntent.SelectDirection -> handleSelectDirection(intent.direction)

                // handled before reducer
                is DocumentReviewIntent.SelectQueueDocument,
                DocumentReviewIntent.LoadMoreQueue -> Unit
            }
        }
    }

    private suspend fun DocumentReviewCtx.loadQueuePage(reset: Boolean) {
        val context = routeContext ?: return
        val queueState = currentQueueState() ?: DocumentReviewQueueState(context = context)
        if (!reset && (queueState.isLoading || queueState.isLoadingMore || !queueState.hasMore)) {
            return
        }

        val nextPage = if (reset) 0 else queueState.currentPage + 1
        val existingItems = if (reset) emptyList() else queueState.items

        updateQueueState {
            val base = it ?: DocumentReviewQueueState(context = context)
            if (reset) {
                base.copy(
                    items = existingItems,
                    isLoading = true,
                    isLoadingMore = false,
                )
            } else {
                base.copy(
                    isLoading = false,
                    isLoadingMore = true,
                )
            }
        }

        loadDocumentRecords(
            page = nextPage,
            pageSize = DocumentsState.PAGE_SIZE,
            filter = context.filter.toListFilter(),
            search = context.search,
        ).fold(
            onSuccess = { response ->
                val loadedItems = response.items.map { it.toDocQueueItem() }
                val mergedItems = mergeQueueItems(existingItems = existingItems, incomingItems = loadedItems)
                updateQueueState {
                    (it ?: DocumentReviewQueueState(context = context)).copy(
                        items = mergedItems,
                        currentPage = nextPage,
                        hasMore = response.hasMore,
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
                ensureSelectedQueueDocumentId()
            },
            onFailure = { error ->
                logger.w(error) { "Failed to load document review queue page=$nextPage" }
                updateQueueState {
                    (it ?: DocumentReviewQueueState(context = context)).copy(
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
            },
        )
    }

    private suspend fun DocumentReviewCtx.currentQueueState(): DocumentReviewQueueState? {
        var queueState: DocumentReviewQueueState? = null
        withState<DocumentReviewState.Loading, _> {
            queueState = this.queueState
        }
        withState<DocumentReviewState.AwaitingExtraction, _> {
            queueState = this.queueState
        }
        withState<DocumentReviewState.Content, _> {
            queueState = this.queueState
        }
        return queueState
    }

    private suspend fun DocumentReviewCtx.currentSelectedDocumentId(): DocumentId? {
        var selectedDocumentId: DocumentId? = null
        withState<DocumentReviewState.Loading, _> {
            selectedDocumentId = selectedQueueDocumentId
        }
        withState<DocumentReviewState.AwaitingExtraction, _> {
            selectedDocumentId = selectedQueueDocumentId ?: documentId
        }
        withState<DocumentReviewState.Content, _> {
            selectedDocumentId = selectedQueueDocumentId ?: documentId
        }
        return selectedDocumentId
    }

    private suspend fun DocumentReviewCtx.ensureSelectedQueueDocumentId() {
        val selectedDocumentId = currentSelectedDocumentId() ?: initialDocumentId
        updateSelectedQueueDocumentId(selectedDocumentId)
    }

    private suspend fun DocumentReviewCtx.updateQueueState(
        transform: (DocumentReviewQueueState?) -> DocumentReviewQueueState?
    ) {
        updateState {
            when (this) {
                is DocumentReviewState.Loading -> copy(queueState = transform(queueState))
                is DocumentReviewState.AwaitingExtraction -> copy(queueState = transform(queueState))
                is DocumentReviewState.Content -> copy(queueState = transform(queueState))
                is DocumentReviewState.Error -> this
            }
        }
    }

    private suspend fun DocumentReviewCtx.updateSelectedQueueDocumentId(documentId: DocumentId?) {
        updateState {
            when (this) {
                is DocumentReviewState.Loading -> copy(selectedQueueDocumentId = documentId)
                is DocumentReviewState.AwaitingExtraction -> copy(selectedQueueDocumentId = documentId)
                is DocumentReviewState.Content -> copy(selectedQueueDocumentId = documentId)
                is DocumentReviewState.Error -> this
            }
        }
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
}
