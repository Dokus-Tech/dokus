package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.launch
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.Dpi
import tech.dokus.domain.model.DocumentMatchResolutionDecision
import tech.dokus.domain.model.ReprocessRequest
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.ResolveDocumentMatchReviewUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewFeedbackActions(
    private val reprocessDocument: ReprocessDocumentUseCase,
    private val resolveDocumentMatchReview: ResolveDocumentMatchReviewUseCase,
    private val refreshAfterDraftUpdate: suspend DocumentReviewCtx.(DocumentId) -> Unit,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleShowFeedbackDialog() {
        updateState { copy(feedbackDialogState = FeedbackDialogState()) }
    }

    suspend fun DocumentReviewCtx.handleRequestAmendment() {
        handleShowFeedbackDialog()
    }

    suspend fun DocumentReviewCtx.handleDismissFeedbackDialog() {
        updateState { copy(feedbackDialogState = null) }
    }

    suspend fun DocumentReviewCtx.handleSelectFeedbackCategory(category: FeedbackCategory) {
        withState {
            feedbackDialogState?.let { dialogState ->
                updateState {
                    copy(feedbackDialogState = dialogState.copy(selectedCategory = category))
                }
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateFeedbackText(text: String) {
        withState {
            feedbackDialogState?.let { dialogState ->
                updateState {
                    copy(feedbackDialogState = dialogState.copy(feedbackText = text))
                }
            }
        }
    }

    suspend fun DocumentReviewCtx.handleSubmitFeedback() {
        withState {
            val activeDocumentId = documentId
            if (activeDocumentId == null) {
                logger.w { "SubmitFeedback: documentId is null, aborting" }
                return@withState
            }
            val dialogState = feedbackDialogState
            if (dialogState == null) {
                logger.w { "SubmitFeedback: feedbackDialogState is null, aborting" }
                return@withState
            }
            val category = dialogState.selectedCategory
            val feedbackText = dialogState.feedbackText.trim()
            if (category == null && feedbackText.isBlank()) {
                logger.w { "SubmitFeedback: no category and blank text, aborting" }
                return@withState
            }

            val fullFeedback = when (category) {
                null -> feedbackText
                else -> if (feedbackText.isNotBlank()) "[${category.name}] $feedbackText" else "[${category.name}]"
            }

            logger.d { "SubmitFeedback: proceeding with feedback for $activeDocumentId" }

            updateState {
                copy(feedbackDialogState = dialogState.copy(isSubmitting = true))
            }

            launch {
                reprocessDocument(
                    activeDocumentId,
                    ReprocessRequest(
                        force = true,
                        dpi = Dpi.create(220),
                        userFeedback = fullFeedback
                    )
                ).fold(
                    onSuccess = { response ->
                        logger.d { "Reprocess with feedback queued: runId=${response.runId}" }
                        updateState { copy(feedbackDialogState = null) }
                        refreshAfterDraftUpdate(activeDocumentId)
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to reprocess with feedback: $activeDocumentId" }
                        withState {
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
        withState {
            val activeDocumentId = documentId ?: return@withState
            logger.d { "Retrying analysis for document: $activeDocumentId" }

            launch {
                reprocessDocument(activeDocumentId)
                    .fold(
                        onSuccess = { response ->
                            logger.d { "Reprocess queued: runId=${response.runId}, status=${response.status}" }
                            refreshAfterDraftUpdate(activeDocumentId)
                        },
                        onFailure = { error ->
                            logger.e(error) { "Failed to reprocess document: $activeDocumentId" }
                            action(DocumentReviewAction.ShowError(error.asDokusException))
                        }
                    )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleDismissFailureBanner() {
        updateState { copy(failureBannerDismissed = true) }
    }

    suspend fun DocumentReviewCtx.handleResolvePossibleMatchSame() {
        resolvePossibleMatch(DocumentMatchResolutionDecision.SAME)
    }

    suspend fun DocumentReviewCtx.handleResolvePossibleMatchDifferent() {
        resolvePossibleMatch(DocumentMatchResolutionDecision.DIFFERENT)
    }

    private suspend fun DocumentReviewCtx.resolvePossibleMatch(decision: DocumentMatchResolutionDecision) {
        withState {
            val reviewId = documentRecord?.pendingMatchReview?.reviewId ?: return@withState
            if (isResolvingMatchReview) return@withState

            updateState { copy(isResolvingMatchReview = true) }

            launch {
                resolveDocumentMatchReview(reviewId, decision).fold(
                    onSuccess = { record ->
                        val draft = record.draft
                        withState {
                            val currentData = documentData ?: return@withState
                            updateState {
                                copy(
                                    document = DokusState.success(
                                        currentData.copy(
                                            documentRecord = record,
                                            draftData = draft?.content,
                                            originalData = draft?.content,
                                        )
                                    ),
                                    hasUnsavedChanges = false,
                                    isResolvingMatchReview = false,
                                    incomingPreviewState = null,
                                    documentStatus = draft?.documentStatus,
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to resolve possible match for reviewId=$reviewId" }
                        withState {
                            updateState { copy(isResolvingMatchReview = false) }
                        }
                        action(DocumentReviewAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }
}
