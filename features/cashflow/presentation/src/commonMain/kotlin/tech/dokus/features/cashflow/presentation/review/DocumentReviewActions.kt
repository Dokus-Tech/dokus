package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.launch
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewActions(
    private val updateDocumentDraft: UpdateDocumentDraftUseCase,
    private val confirmDocument: ConfirmDocumentUseCase,
    private val rejectDocument: RejectDocumentUseCase,
    private val reprocessDocument: ReprocessDocumentUseCase,
    private val getDocumentRecord: GetDocumentRecordUseCase,
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
                updateDocumentDraft(
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
                confirmDocument(
                    documentId,
                    ConfirmDocumentRequest(
                        documentType = editableData.documentType,
                        extractedData = updatedData
                    )
                ).fold(
                    onSuccess = { record ->
                        val draft = record.draft
                        val isConfirmed = draft?.documentStatus == DocumentStatus.Confirmed
                        val isRejected = draft?.documentStatus == DocumentStatus.Rejected
                        val cashflowEntryId = record.cashflowEntryId
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
                                    confirmedCashflowEntryId = cashflowEntryId,
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
                rejectDocument(documentId, RejectDocumentRequest(reason))
                    .fold(
                        onSuccess = { record ->
                            val draft = record.draft
                            withState<DocumentReviewState.Content, _> {
                                updateState {
                                    copy(
                                        document = record,
                                        isRejecting = false,
                                        isDocumentRejected = draft?.documentStatus == DocumentStatus.Rejected,
                                        isDocumentConfirmed = draft?.documentStatus == DocumentStatus.Confirmed,
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

    suspend fun DocumentReviewCtx.handleViewCashflowEntry() {
        withState<DocumentReviewState.Content, _> {
            val entryId = confirmedCashflowEntryId ?: return@withState
            action(DocumentReviewAction.NavigateToCashflowEntry(entryId))
        }
    }

    suspend fun DocumentReviewCtx.handleViewEntity() {
        withState<DocumentReviewState.Content, _> {
            val confirmedEntityId = document.confirmedEntity?.let { entity ->
                when (entity) {
                    is FinancialDocumentDto.InvoiceDto -> entity.id.toString()
                    is FinancialDocumentDto.BillDto -> entity.id.toString()
                    is FinancialDocumentDto.ExpenseDto -> entity.id.toString()
                    is FinancialDocumentDto.CreditNoteDto -> entity.id.toString()
                }
            }
            if (confirmedEntityId != null) {
                action(
                    DocumentReviewAction.NavigateToEntity(
                        entityId = confirmedEntityId,
                        entityType = editableData.documentType
                    )
                )
            }
        }
    }

    // === Failed Analysis Handlers ===

    suspend fun DocumentReviewCtx.handleRetryAnalysis() {
        withState<DocumentReviewState.Content, _> {
            logger.d { "Retrying analysis for document: $documentId" }

            launch {
                reprocessDocument(documentId)
                    .fold(
                        onSuccess = { response ->
                            logger.d { "Reprocess queued: runId=${response.runId}, status=${response.status}" }
                            // Refresh to get the new ingestion status
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

    private suspend fun DocumentReviewCtx.refreshAfterDraftUpdate(documentId: tech.dokus.domain.ids.DocumentId) {
        getDocumentRecord(documentId).fold(
            onSuccess = { record ->
                val draft = record.draft
                withState<DocumentReviewState.Content, _> {
                    updateState {
                        copy(
                            document = record,
                            editableData = EditableExtractedData.fromExtractedData(draft?.extractedData),
                            originalData = draft?.extractedData,
                            counterpartyIntent = draft?.counterpartyIntent ?: CounterpartyIntent.None,
                            isDocumentConfirmed = draft?.documentStatus == DocumentStatus.Confirmed,
                            isDocumentRejected = draft?.documentStatus == DocumentStatus.Rejected
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
