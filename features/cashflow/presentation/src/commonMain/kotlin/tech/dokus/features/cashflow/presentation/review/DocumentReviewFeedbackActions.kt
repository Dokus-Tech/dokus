package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.launch
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.ResolveDocumentMatchReviewUseCase
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewFeedbackActions(
    private val reprocessDocument: ReprocessDocumentUseCase,
    private val resolveDocumentMatchReview: ResolveDocumentMatchReviewUseCase,
    private val refreshAfterDraftUpdate: suspend DocumentReviewCtx.(DocumentId) -> Unit,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleShowFeedbackDialog() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(feedbackDialogState = FeedbackDialogState()) }
        }
    }

    suspend fun DocumentReviewCtx.handleRequestAmendment() {
        handleShowFeedbackDialog()
    }

    suspend fun DocumentReviewCtx.handleDismissFeedbackDialog() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(feedbackDialogState = null) }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateFeedbackText(text: String) {
        withState<DocumentReviewState.Content, _> {
            feedbackDialogState?.let { dialogState ->
                updateState {
                    copy(feedbackDialogState = dialogState.copy(feedbackText = text))
                }
            }
        }
    }

    suspend fun DocumentReviewCtx.handleSubmitFeedback() {
        withState<DocumentReviewState.Content, _> {
            val dialogState = feedbackDialogState ?: return@withState
            val feedback = dialogState.feedbackText.trim()
            if (feedback.isBlank()) return@withState

            updateState {
                copy(feedbackDialogState = dialogState.copy(isSubmitting = true))
            }

            launch {
                reprocessDocument(
                    documentId,
                    ReprocessRequest(
                        force = true,
                        dpi = 220,
                        userFeedback = feedback
                    )
                ).fold(
                    onSuccess = { response ->
                        logger.d { "Reprocess with feedback queued: runId=${response.runId}" }
                        withState<DocumentReviewState.Content, _> {
                            updateState { copy(feedbackDialogState = null) }
                        }
                        refreshAfterDraftUpdate(documentId)
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to reprocess with feedback: $documentId" }
                        withState<DocumentReviewState.Content, _> {
                            updateState {
                                copy(feedbackDialogState = feedbackDialogState?.copy(isSubmitting = false))
                            }
                        }
                        action(DocumentReviewAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleRetryAnalysis() {
        withState<DocumentReviewState.Content, _> {
            logger.d { "Retrying analysis for document: $documentId" }

            launch {
                reprocessDocument(documentId)
                    .fold(
                        onSuccess = { response ->
                            logger.d { "Reprocess queued: runId=${response.runId}, status=${response.status}" }
                            refreshAfterDraftUpdate(documentId)
                        },
                        onFailure = { error ->
                            logger.e(error) { "Failed to reprocess document: $documentId" }
                            action(DocumentReviewAction.ShowError(error.asDokusException))
                        }
                    )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleDismissFailureBanner() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(failureBannerDismissed = true) }
        }
    }

    suspend fun DocumentReviewCtx.handleResolvePossibleMatchSame() {
        resolvePossibleMatch(DocumentMatchResolutionDecision.SAME)
    }

    suspend fun DocumentReviewCtx.handleResolvePossibleMatchDifferent() {
        resolvePossibleMatch(DocumentMatchResolutionDecision.DIFFERENT)
    }

    private suspend fun DocumentReviewCtx.resolvePossibleMatch(decision: DocumentMatchResolutionDecision) {
        withState<DocumentReviewState.Content, _> {
            val reviewId = document.pendingMatchReview?.reviewId ?: return@withState
            if (isResolvingMatchReview) return@withState

            updateState { copy(isResolvingMatchReview = true) }

            launch {
                resolveDocumentMatchReview(reviewId, decision).fold(
                    onSuccess = { record ->
                        val draft = record.draft
                        withState<DocumentReviewState.Content, _> {
                            updateState {
                                copy(
                                    document = record,
                                    draftData = draft?.extractedData,
                                    originalData = draft?.extractedData,
                                    hasUnsavedChanges = false,
                                    isResolvingMatchReview = false,
                                    counterpartyIntent = draft?.counterpartyIntent ?: CounterpartyIntent.None,
                                    isDocumentConfirmed = draft?.documentStatus == DocumentStatus.Confirmed,
                                    isDocumentRejected = draft?.documentStatus == DocumentStatus.Rejected,
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to resolve possible match for reviewId=$reviewId" }
                        withState<DocumentReviewState.Content, _> {
                            updateState { copy(isResolvingMatchReview = false) }
                        }
                        action(DocumentReviewAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }
}
