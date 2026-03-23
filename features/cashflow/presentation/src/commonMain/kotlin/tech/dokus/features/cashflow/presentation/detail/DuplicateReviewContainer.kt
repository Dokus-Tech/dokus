package tech.dokus.features.cashflow.presentation.detail

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.Dpi
import tech.dokus.domain.model.sortDate
import tech.dokus.domain.model.totalAmount
import tech.dokus.features.cashflow.presentation.detail.models.toUiData
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.ResolveDocumentMatchReviewUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal typealias DuplicateReviewCtx =
    PipelineContext<DuplicateReviewState, DuplicateReviewIntent, DuplicateReviewAction>

/**
 * FlowMVI Container for the duplicate document review flow.
 *
 * Loads two documents (existing + incoming), computes field diffs,
 * loads PDF previews for both, and handles Same/Different resolution.
 */
internal class DuplicateReviewContainer(
    private val existingDocumentId: DocumentId,
    private val incomingDocumentId: DocumentId,
    private val reviewId: DocumentMatchReviewId,
    private val reasonType: ReviewReason,
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val getDocumentPages: GetDocumentPagesUseCase,
    private val resolveMatchReview: ResolveDocumentMatchReviewUseCase,
) : Container<DuplicateReviewState, DuplicateReviewIntent, DuplicateReviewAction> {

    private val logger = Logger.forClass<DuplicateReviewContainer>()

    override val store: Store<DuplicateReviewState, DuplicateReviewIntent, DuplicateReviewAction> =
        store(DuplicateReviewState(reviewId = reviewId, reasonType = reasonType)) {
            reduce { intent ->
                when (intent) {
                    DuplicateReviewIntent.ResolveSame -> handleResolve(DocumentMatchResolutionDecision.SAME)
                    DuplicateReviewIntent.ResolveDifferent -> handleResolve(DocumentMatchResolutionDecision.DIFFERENT)
                }
            }

            // Load both documents on store start
            init {
                handleLoad()
            }
        }

    private suspend fun DuplicateReviewCtx.handleLoad() {
        logger.d { "Loading duplicate review: existing=$existingDocumentId, incoming=$incomingDocumentId" }

        updateState {
            copy(
                existingDoc = DokusState.loading(),
                incomingDoc = DokusState.loading(),
            )
        }

        // Load existing document
        getDocumentRecord(existingDocumentId).fold(
            onSuccess = { record ->
                val uiData = record.draft?.content?.toUiData()
                updateState {
                    copy(
                        existingDoc = DokusState.success(record),
                        existingUiData = uiData,
                    )
                }
                // Load existing PDF preview
                loadPreview(existingDocumentId, isExisting = true)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load existing document" }
                updateState {
                    copy(existingDoc = DokusState.error(
                        exception = error.asDokusException,
                        retryHandler = { intent(DuplicateReviewIntent.ResolveSame) },
                    ))
                }
            },
        )

        // Load incoming document
        getDocumentRecord(incomingDocumentId).fold(
            onSuccess = { record ->
                val draft = record.draft?.content
                val uiData = draft?.toUiData()
                updateState {
                    copy(
                        incomingDoc = DokusState.success(record),
                        incomingUiData = uiData,
                    )
                }
                // Load incoming PDF preview
                loadPreview(incomingDocumentId, isExisting = false)

                // Compute diffs now that we have both — uses raw DocDto for field comparison
                withState {
                    val existingRecord = (existingDoc as? DokusState.Success<DocumentDetailDto>)?.data
                    val existingDraft = existingRecord?.draft?.content
                    if (existingDraft != null && draft != null) {
                        val diffs = computeDiffs(existingDraft, draft)
                        updateState { copy(diffs = diffs) }
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load incoming document" }
                updateState {
                    copy(incomingDoc = DokusState.error(
                        exception = error.asDokusException,
                        retryHandler = { intent(DuplicateReviewIntent.ResolveSame) },
                    ))
                }
            },
        )
    }

    private suspend fun DuplicateReviewCtx.loadPreview(
        documentId: DocumentId,
        isExisting: Boolean,
    ) {
        getDocumentPages(documentId, Dpi.default).fold(
            onSuccess = { response ->
                val previewState = if (response.pages.isEmpty()) {
                    DocumentPreviewState.NoPreview
                } else {
                    DocumentPreviewState.Ready(
                        pages = response.pages,
                        totalPages = response.totalPages,
                        renderedPages = response.renderedPages,
                        dpi = response.dpi,
                        hasMore = response.totalPages > response.renderedPages,
                    )
                }
                updateState {
                    if (isExisting) copy(existingPreview = previewState)
                    else copy(incomingPreview = previewState)
                }
            },
            onFailure = {
                updateState {
                    if (isExisting) copy(existingPreview = DocumentPreviewState.NoPreview)
                    else copy(incomingPreview = DocumentPreviewState.NoPreview)
                }
            },
        )
    }

    private suspend fun DuplicateReviewCtx.handleResolve(decision: DocumentMatchResolutionDecision) {
        logger.d { "Resolving match review $reviewId as $decision" }
        updateState { copy(isResolving = true, error = null) }

        resolveMatchReview(reviewId, decision).fold(
            onSuccess = {
                logger.i { "Match review $reviewId resolved as $decision" }
                updateState { copy(isResolving = false) }
                action(DuplicateReviewAction.Resolved)
            },
            onFailure = { error ->
                logger.e(error) { "Failed to resolve match review" }
                updateState {
                    copy(isResolving = false, error = error.asDokusException)
                }
            },
        )
    }
}

/**
 * Compute field-level diffs between two DocDto objects.
 */
private fun computeDiffs(existing: DocDto, incoming: DocDto): List<DuplicateDiff> = buildList {
    val existingInvoiceNo = (existing as? DocDto.Invoice)?.invoiceNumber
    val incomingInvoiceNo = (incoming as? DocDto.Invoice)?.invoiceNumber
    if (existingInvoiceNo != null && incomingInvoiceNo != null && existingInvoiceNo != incomingInvoiceNo) {
        add(DuplicateDiff("invoiceNo", existingInvoiceNo, incomingInvoiceNo))
    }

    val existingTotal = existing.totalAmount
    val incomingTotal = incoming.totalAmount
    if (existingTotal != null && incomingTotal != null && existingTotal != incomingTotal) {
        add(DuplicateDiff("total", "\u20AC${existingTotal.formatAmount()}", "\u20AC${incomingTotal.formatAmount()}"))
    }

    val existingDate = existing.sortDate
    val incomingDate = incoming.sortDate
    if (existingDate != null && incomingDate != null && existingDate != incomingDate) {
        add(DuplicateDiff("issueDate", existingDate.toString(), incomingDate.toString()))
    }
}
