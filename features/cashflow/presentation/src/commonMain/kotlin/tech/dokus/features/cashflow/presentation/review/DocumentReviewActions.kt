package tech.dokus.features.cashflow.presentation.review

import tech.dokus.foundation.platform.Logger
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.exceptions.DokusException

internal class DocumentReviewActions(
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleSaveDraft() {
        withState<DocumentReviewState.Content, _> {
            if (!hasUnsavedChanges) return@withState

            logger.d { "Saving draft for document: $documentId" }
            updateState { copy(isSaving = true) }


            launch {
                logger.i { "Draft save requested (API method needed): $documentId" }

                withState<DocumentReviewState.Content, _> {
                    updateState {
                        copy(
                            isSaving = false,
                            hasUnsavedChanges = false
                        )
                    }
                    action(DocumentReviewAction.ShowSuccess(DocumentReviewSuccess.DraftSaved))
                }
            }
        }
    }

    suspend fun DocumentReviewCtx.handleDiscardChanges() {
        withState<DocumentReviewState.Content, _> {
            if (!hasUnsavedChanges) return@withState
            action(DocumentReviewAction.ShowDiscardConfirmation)
        }
    }

    suspend fun DocumentReviewCtx.handleConfirm() {
        withState<DocumentReviewState.Content, _> {
            if (!canConfirm) {
                action(
                    DocumentReviewAction.ShowError(
                        DokusException.Validation.DocumentMissingFields
                    )
                )
                return@withState
            }

            logger.d { "Confirming document: $documentId" }
            updateState { copy(isConfirming = true) }


            launch {
                logger.i { "Confirmation requested (API method needed): $documentId" }

                withState<DocumentReviewState.Content, _> {
                    updateState { copy(isConfirming = false) }
                    action(DocumentReviewAction.ShowSuccess(DocumentReviewSuccess.DocumentConfirmed))
                    action(
                        DocumentReviewAction.NavigateToEntity(
                            entityId = documentId.toString(),
                            entityType = editableData.documentType
                        )
                    )
                }
            }
        }
    }

    suspend fun DocumentReviewCtx.handleReject() {
        withState<DocumentReviewState.Content, _> {
            action(DocumentReviewAction.ShowRejectConfirmation)
        }
    }

    suspend fun DocumentReviewCtx.handleOpenChat() {
        withState<DocumentReviewState.Content, _> {
            action(DocumentReviewAction.NavigateToChat(documentId))
        }
    }
}
