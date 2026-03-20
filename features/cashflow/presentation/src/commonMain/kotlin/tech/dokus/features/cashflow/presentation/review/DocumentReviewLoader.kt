package tech.dokus.features.cashflow.presentation.review

import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.isContactRequired
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewLoader(
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleLoadDocument(documentId: DocumentId) {
        logger.d { "Loading document: $documentId" }
        updateState {
            copy(
                document = document.asLoading,
                isAwaitingExtraction = false,
                selectedQueueDocumentId = if (queueState != null) documentId else selectedQueueDocumentId,
                incomingPreviewState = null,
                // Reset document-specific UI state to prevent stale data from previous document
                sourceViewerState = null,
                paymentSheetState = null,
                rejectDialogState = null,
                feedbackDialogState = null,
                selectedFieldPath = null,
                hasUnsavedChanges = false,
                failureBannerDismissed = false,
                showContactSheet = false,
            )
        }

        fetchDocumentProcessing(documentId)
    }

    suspend fun DocumentReviewCtx.handleRefresh() {
        withState {
            val activeDocumentId = documentId ?: return@withState
            if (hasContent || isAwaitingExtraction) {
                logger.d { "Refreshing document: $activeDocumentId" }
                fetchDocumentProcessing(activeDocumentId)
            }
        }
    }

    suspend fun DocumentReviewCtx.handleApplyRemoteSnapshot(record: DocumentDetailDto) {
        withState {
            val activeDocumentId = documentId ?: run {
                logger.d { "Dropping remote snapshot: no active document in current state" }
                return@withState
            }
            if (activeDocumentId != record.document.id) return@withState

            logger.d { "Applying remote snapshot for document: $activeDocumentId" }
            transitionToDocumentState(
                documentId = activeDocumentId,
                document = record,
            )
        }
    }

    private suspend fun DocumentReviewCtx.fetchDocumentProcessing(documentId: DocumentId) {
        getDocumentRecord(documentId)
            .fold(
                onSuccess = { document ->
                    transitionToDocumentState(
                        documentId = documentId,
                        document = document,
                    )
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load document: $documentId" }
                    updateState {
                        copy(
                            document = DokusState.error(
                                exception = error.asDokusException,
                                retryHandler = { intent(DocumentReviewIntent.LoadDocument(documentId)) },
                            ),
                            isAwaitingExtraction = false,
                        )
                    }
                }
            )
    }

    private suspend fun DocumentReviewCtx.transitionToDocumentState(
        documentId: DocumentId,
        document: DocumentDetailDto,
    ) {
        val previewUrl = document.document.downloadUrl
        var previousPreviewUrl: String? = null
        var previousPreviewState: DocumentPreviewState = DocumentPreviewState.Loading
        var previousIncomingSourceId: tech.dokus.domain.ids.DocumentSourceId? = null
        var previousIncomingPreviewState: DocumentPreviewState? = null
        withState {
            previousPreviewUrl = this.previewUrl
            previousPreviewState = this.previewState
            previousIncomingSourceId = documentRecord?.pendingMatchReview?.incomingSourceId
            previousIncomingPreviewState = incomingPreviewState
        }
        val shouldReloadPreview = previousPreviewUrl != previewUrl ||
            previousPreviewState is DocumentPreviewState.Loading
        val previewState = if (previousPreviewUrl == previewUrl) {
            previousPreviewState
        } else {
            DocumentPreviewState.Loading
        }
        val incomingSourceId = document.pendingMatchReview?.incomingSourceId
        val shouldReloadIncomingPreview = when {
            incomingSourceId == null -> false
            previousIncomingSourceId != incomingSourceId -> true
            previousIncomingPreviewState == null -> true
            previousIncomingPreviewState is DocumentPreviewState.Loading -> true
            else -> false
        }
        val incomingPreviewState = when {
            incomingSourceId == null -> null
            previousIncomingSourceId == incomingSourceId && previousIncomingPreviewState != null -> previousIncomingPreviewState
            else -> DocumentPreviewState.Loading
        }
        var selectedQueueDocumentId: DocumentId? = null
        withState {
            selectedQueueDocumentId = if (queueState != null) documentId else null
        }
        val draft = document.draft
        val content = draft?.content

        if (content == null) {
            val isFailed = document.latestIngestion?.status == IngestionStatus.Failed
            if (isFailed) {
                // Failed extraction -> show as content with null draft data
                updateState {
                    copy(
                        document = DokusState.success(
                            ReviewDocumentData(
                                documentId = documentId,
                                documentRecord = document,
                                draftData = null,
                                originalData = null,
                                previewUrl = previewUrl,
                                contactSuggestions = emptyList(),
                            )
                        ),
                        isAwaitingExtraction = false,
                        previewState = previewState,
                        incomingPreviewState = incomingPreviewState,
                        // Preserve existing UI state where available
                        isContactRequired = false,
                        documentStatus = draft?.documentStatus,
                        confirmedCashflowEntryId = document.cashflowEntryId,
                        selectedQueueDocumentId = selectedQueueDocumentId ?: this@transitionToDocumentState.let {
                            var id: DocumentId? = null
                            withState { id = this.selectedQueueDocumentId }
                            id
                        },
                    )
                }
            } else {
                // Still awaiting extraction
                updateState {
                    copy(
                        document = DokusState.success(
                            ReviewDocumentData(
                                documentId = documentId,
                                documentRecord = document,
                                draftData = null,
                                originalData = null,
                                previewUrl = previewUrl,
                                contactSuggestions = emptyList(),
                            )
                        ),
                        isAwaitingExtraction = true,
                        previewState = previewState,
                        incomingPreviewState = incomingPreviewState,
                        selectedQueueDocumentId = selectedQueueDocumentId ?: this@transitionToDocumentState.let {
                            var id: DocumentId? = null
                            withState { id = this.selectedQueueDocumentId }
                            id
                        },
                    )
                }
            }

            if (shouldReloadPreview) {
                intent(DocumentReviewIntent.LoadPreviewPages)
            }
            return
        }

        val contactSuggestions = draft.contactSuggestions

        // Read previous cashflow state for preservation
        var previousCashflowEntryState: DokusState<tech.dokus.domain.model.CashflowEntry> = DokusState.idle()
        var previousAutoPaymentStatus: DokusState<tech.dokus.domain.model.AutoPaymentStatus> = DokusState.idle()
        var previousConfirmedCashflowEntryId: tech.dokus.domain.ids.CashflowEntryId? = null
        withState {
            if (hasContent) {
                previousCashflowEntryState = cashflowEntryState
                previousAutoPaymentStatus = autoPaymentStatus
                previousConfirmedCashflowEntryId = confirmedCashflowEntryId
            }
        }

        updateState {
            copy(
                document = DokusState.success(
                    ReviewDocumentData(
                        documentId = documentId,
                        documentRecord = document,
                        draftData = content,
                        originalData = content,
                        previewUrl = previewUrl,
                        contactSuggestions = contactSuggestions,
                    )
                ),
                isAwaitingExtraction = false,
                previewState = previewState,
                incomingPreviewState = incomingPreviewState,
                isContactRequired = content.isContactRequired,
                documentStatus = draft.documentStatus,
                confirmedCashflowEntryId = document.cashflowEntryId,
                cashflowEntryState = previousCashflowEntryState,
                autoPaymentStatus = previousAutoPaymentStatus,
                selectedQueueDocumentId = selectedQueueDocumentId ?: this@transitionToDocumentState.let {
                    var id: DocumentId? = null
                    withState { id = this.selectedQueueDocumentId }
                    id
                },
            )
        }

        if (shouldReloadPreview || shouldReloadIncomingPreview) {
            intent(DocumentReviewIntent.LoadPreviewPages)
        }
        if (document.cashflowEntryId != null && document.cashflowEntryId != previousConfirmedCashflowEntryId) {
            intent(DocumentReviewIntent.LoadCashflowEntry)
        }
    }
}
