package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.launch
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.features.cashflow.usecases.DocumentReviewUseCase
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewActions(
    private val documentReviewUseCase: DocumentReviewUseCase,
    private val mapper: DocumentReviewExtractedDataMapper,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleSaveDraft() {
        withState<DocumentReviewState.Content, _> {
            if (!hasUnsavedChanges) return@withState

            logger.d { "Saving draft for document: $documentId" }
            updateState { copy(isSaving = true) }

            launch {
                val updatedData = mapper.buildExtractedDataFromEditable(editableData, originalData)
                documentReviewUseCase.updateDocumentDraft(
                    documentId,
                    UpdateDraftRequest(extractedData = updatedData)
                ).fold(
                    onSuccess = {
                        refreshAfterDraftUpdate(documentId)
                        withState<DocumentReviewState.Content, _> {
                            updateState { copy(isSaving = false, hasUnsavedChanges = false) }
                        }
                        action(DocumentReviewAction.ShowSuccess(DocumentReviewSuccess.DraftSaved))
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to save draft: $documentId" }
                        withState<DocumentReviewState.Content, _> {
                            updateState { copy(isSaving = false) }
                        }
                        action(DocumentReviewAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleDiscardChanges() {
        withState<DocumentReviewState.Content, _> {
            if (!hasUnsavedChanges) return@withState
            action(DocumentReviewAction.ShowDiscardConfirmation)
        }
    }

    suspend fun DocumentReviewCtx.handleConfirmDiscardChanges() {
        withState<DocumentReviewState.Content, _> {
            val restoredData = EditableExtractedData.fromExtractedData(originalData)
            updateState {
                copy(
                    editableData = restoredData,
                    hasUnsavedChanges = false
                )
            }
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
                val updatedData = mapper.buildExtractedDataFromEditable(editableData, originalData)
                documentReviewUseCase.confirmDocument(
                    documentId,
                    ConfirmDocumentRequest(
                        documentType = editableData.documentType,
                        extractedData = updatedData
                    )
                ).fold(
                    onSuccess = { record ->
                        val draft = record.draft
                        val isConfirmed = draft?.draftStatus == DraftStatus.Confirmed
                        val isRejected = draft?.draftStatus == DraftStatus.Rejected
                        withState<DocumentReviewState.Content, _> {
                            val linkedContactId = draft?.linkedContactId
                            updateState {
                                copy(
                                    document = record,
                                    editableData = EditableExtractedData.fromExtractedData(draft?.extractedData),
                                    originalData = draft?.extractedData,
                                    hasUnsavedChanges = false,
                                    isConfirming = false,
                                    isDocumentConfirmed = isConfirmed,
                                    isDocumentRejected = isRejected,
                                    counterpartyIntent = draft?.counterpartyIntent ?: CounterpartyIntent.None,
                                    selectedContactId = linkedContactId ?: selectedContactId,
                                    contactSelectionState = if (linkedContactId != null) {
                                        ContactSelectionState.Selected
                                    } else {
                                        contactSelectionState
                                    }
                                )
                            }
                        }
                        action(DocumentReviewAction.ShowSuccess(DocumentReviewSuccess.DocumentConfirmed))
                        action(
                            DocumentReviewAction.NavigateToEntity(
                                entityId = record.confirmedEntity?.let { entity ->
                                    when (entity) {
                                        is tech.dokus.domain.model.FinancialDocumentDto.InvoiceDto -> entity.id.toString()
                                        is tech.dokus.domain.model.FinancialDocumentDto.BillDto -> entity.id.toString()
                                        is tech.dokus.domain.model.FinancialDocumentDto.ExpenseDto -> entity.id.toString()
                                    }
                                } ?: documentId.toString(),
                                entityType = editableData.documentType
                            )
                        )
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to confirm document: $documentId" }
                        withState<DocumentReviewState.Content, _> {
                            updateState { copy(isConfirming = false) }
                        }
                        action(DocumentReviewAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }

    // === Reject Dialog Handlers ===

    suspend fun DocumentReviewCtx.handleShowRejectDialog() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(rejectDialogState = RejectDialogState()) }
        }
    }

    suspend fun DocumentReviewCtx.handleDismissRejectDialog() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(rejectDialogState = null) }
        }
    }

    suspend fun DocumentReviewCtx.handleSelectRejectReason(reason: DocumentRejectReason) {
        withState<DocumentReviewState.Content, _> {
            rejectDialogState?.let { dialogState ->
                updateState {
                    copy(
                        rejectDialogState = dialogState.copy(
                            selectedReason = reason,
                            // Clear note if not "Other" reason
                            otherNote = if (reason == DocumentRejectReason.Other) dialogState.otherNote else ""
                        )
                    )
                }
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateRejectNote(note: String) {
        withState<DocumentReviewState.Content, _> {
            rejectDialogState?.let { dialogState ->
                updateState {
                    copy(rejectDialogState = dialogState.copy(otherNote = note))
                }
            }
        }
    }

    suspend fun DocumentReviewCtx.handleConfirmReject() {
        withState<DocumentReviewState.Content, _> {
            val dialogState = rejectDialogState ?: return@withState
            val reason = dialogState.selectedReason

            // Set loading state in dialog
            updateState {
                copy(
                    isRejecting = true,
                    rejectDialogState = dialogState.copy(isConfirming = true)
                )
            }

            launch {
                documentReviewUseCase.rejectDocument(documentId, RejectDocumentRequest(reason))
                    .fold(
                        onSuccess = { record ->
                            val draft = record.draft
                            withState<DocumentReviewState.Content, _> {
                                updateState {
                                    copy(
                                        document = record,
                                        isRejecting = false,
                                        isDocumentRejected = draft?.draftStatus == DraftStatus.Rejected,
                                        isDocumentConfirmed = draft?.draftStatus == DraftStatus.Confirmed,
                                        rejectDialogState = null // Close dialog on success
                                    )
                                }
                            }
                            action(DocumentReviewAction.NavigateBack)
                        },
                        onFailure = { error ->
                            logger.e(error) { "Failed to reject document: $documentId" }
                            withState<DocumentReviewState.Content, _> {
                                updateState {
                                    copy(
                                        isRejecting = false,
                                        rejectDialogState = rejectDialogState?.copy(isConfirming = false)
                                    )
                                }
                            }
                            action(DocumentReviewAction.ShowError(error.asDokusException))
                        }
                    )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleOpenChat() {
        withState<DocumentReviewState.Content, _> {
            action(DocumentReviewAction.NavigateToChat(documentId))
        }
    }

    private suspend fun DocumentReviewCtx.refreshAfterDraftUpdate(documentId: tech.dokus.domain.ids.DocumentId) {
        documentReviewUseCase.getDocumentRecord(documentId).fold(
            onSuccess = { record ->
                val draft = record.draft
                withState<DocumentReviewState.Content, _> {
                    updateState {
                        copy(
                            document = record,
                            editableData = EditableExtractedData.fromExtractedData(draft?.extractedData),
                            originalData = draft?.extractedData,
                            counterpartyIntent = draft?.counterpartyIntent ?: CounterpartyIntent.None,
                            isDocumentConfirmed = draft?.draftStatus == DraftStatus.Confirmed,
                            isDocumentRejected = draft?.draftStatus == DraftStatus.Rejected
                        )
                    }
                }
            },
            onFailure = { error ->
                logger.w(error) { "Failed to refresh draft after update" }
            }
        )
    }
}
