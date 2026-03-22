package tech.dokus.features.cashflow.presentation.detail

import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.isContactRequired
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.presentation.detail.mvi.payment.DocumentPaymentIntent
import tech.dokus.features.cashflow.presentation.detail.mvi.preview.DocumentPreviewIntent
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal class DocumentDetailLoader(
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val logger: Logger,
) {
    suspend fun DocumentDetailCtx.handleLoadDocument(documentId: DocumentId) {
        logger.d { "Loading document: $documentId" }
        updateState {
            copy(
                document = document.asLoading,
                isAwaitingExtraction = false,
                selectedQueueDocumentId = if (queueState != null) documentId else selectedQueueDocumentId,
                // Reset document-specific UI state to prevent stale data from previous document
                paymentSheetState = null,
                rejectDialogState = null,
                feedbackDialogState = null,
                selectedFieldPath = null,
                hasUnsavedChanges = false,
                failureBannerDismissed = false,
                showContactSheet = false,
            )
        }
        // Reset preview child state for the new document
        intent(DocumentDetailIntent.Preview(DocumentPreviewIntent.SetDocumentContext(
            documentId = documentId,
            documentRecord = null,
            hasContent = false,
            hasPendingMatchReview = false,
            previewState = DocumentPreviewState.Loading,
            incomingPreviewState = null,
            resetSourceViewer = true,
        )))

        fetchDocumentProcessing(documentId)
    }

    suspend fun DocumentDetailCtx.handleRefresh() {
        withState {
            val activeDocumentId = documentId ?: return@withState
            if (hasContent || isAwaitingExtraction) {
                logger.d { "Refreshing document: $activeDocumentId" }
                fetchDocumentProcessing(activeDocumentId)
            }
        }
    }

    suspend fun DocumentDetailCtx.handleApplyRemoteSnapshot(record: DocumentDetailDto) {
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

    private suspend fun DocumentDetailCtx.fetchDocumentProcessing(documentId: DocumentId) {
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
                                retryHandler = { intent(DocumentDetailIntent.LoadDocument(documentId)) },
                            ),
                            isAwaitingExtraction = false,
                        )
                    }
                }
            )
    }

    private suspend fun DocumentDetailCtx.transitionToDocumentState(
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
                        selectedQueueDocumentId = selectedQueueDocumentId ?: this@transitionToDocumentState.let {
                            var id: DocumentId? = null
                            withState { id = this.selectedQueueDocumentId }
                            id
                        },
                    )
                }
            }

            // Push context and preview state to child
            intent(DocumentDetailIntent.Preview(DocumentPreviewIntent.SetDocumentContext(
                documentId = documentId,
                documentRecord = document,
                hasContent = isFailed,
                hasPendingMatchReview = document.pendingMatchReview != null,
                previewState = previewState,
                incomingPreviewState = incomingPreviewState,
            )))
            if (shouldReloadPreview) {
                intent(DocumentDetailIntent.Preview(DocumentPreviewIntent.LoadPages))
            }
            return
        }

        val contactSuggestions = draft.contactSuggestions

        // Read previous cashflow state for preservation
        var previousCashflowEntryState: DokusState<tech.dokus.domain.model.CashflowEntryDto> = DokusState.idle()
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
                isContactRequired = content.isContactRequired,
                detectedContactAccepted = false,
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

        // Push context and preview state to child
        intent(DocumentDetailIntent.Preview(DocumentPreviewIntent.SetDocumentContext(
            documentId = documentId,
            documentRecord = document,
            hasContent = true,
            hasPendingMatchReview = document.pendingMatchReview != null,
            previewState = previewState,
            incomingPreviewState = incomingPreviewState,
        )))
        if (shouldReloadPreview || shouldReloadIncomingPreview) {
            intent(DocumentDetailIntent.Preview(DocumentPreviewIntent.LoadPages))
        }
        if (document.cashflowEntryId != null && document.cashflowEntryId != previousConfirmedCashflowEntryId) {
            intent(DocumentDetailIntent.Payment(DocumentPaymentIntent.LoadCashflowEntry))
        }
    }
}
