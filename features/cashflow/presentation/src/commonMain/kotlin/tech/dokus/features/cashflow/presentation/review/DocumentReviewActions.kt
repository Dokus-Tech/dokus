package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewActions(
    private val updateDocumentDraft: UpdateDocumentDraftUseCase,
    private val confirmDocument: ConfirmDocumentUseCase,
    private val rejectDocument: RejectDocumentUseCase,
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val logger: Logger,
) {
    private data class DraftSyncPayload(
        val documentId: DocumentId,
        val draftData: DocumentDraftData,
        val token: Long,
    )

    private var inlineDraftSyncJob: Job? = null
    private var inlineDraftSyncToken: Long = 0L

    suspend fun DocumentReviewCtx.syncDraftImmediately() {
        var payload: DraftSyncPayload? = null
        withState<DocumentReviewState.Content, _> {
            val updatedData = draftData ?: return@withState
            inlineDraftSyncToken += 1
            val token = inlineDraftSyncToken

            updateState {
                copy(
                    hasUnsavedChanges = true,
                    isSaving = true,
                )
            }

            payload = DraftSyncPayload(
                documentId = documentId,
                draftData = updatedData,
                token = token,
            )
        }
        val syncPayload = payload ?: return

        inlineDraftSyncJob?.cancel()
        inlineDraftSyncJob = launch {
            val result = updateDocumentDraft(
                syncPayload.documentId,
                UpdateDraftRequest(extractedData = syncPayload.draftData)
            )

            result.fold(
                onSuccess = { response ->
                    if (syncPayload.token != inlineDraftSyncToken) return@fold
                    inlineDraftSyncJob = null
                    withState<DocumentReviewState.Content, _> {
                        updateState {
                            copy(
                                draftData = response.extractedData,
                                originalData = response.extractedData,
                                hasUnsavedChanges = false,
                                isSaving = false,
                                isContactRequired = response.extractedData.isContactRequired,
                            )
                        }
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException || syncPayload.token != inlineDraftSyncToken) {
                        return@fold
                    }
                    inlineDraftSyncJob = null
                    logger.e(error) { "Failed to persist draft correction: ${syncPayload.documentId}" }
                    withState<DocumentReviewState.Content, _> {
                        updateState {
                            copy(
                                hasUnsavedChanges = true,
                                isSaving = false,
                            )
                        }
                    }
                    action(DocumentReviewAction.ShowError(error.asDokusException))
                }
            )
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

            val updatedData = draftData
            if (updatedData == null) {
                action(DocumentReviewAction.ShowError(DokusException.Validation.DocumentMissingFields))
                return@withState
            }

            inlineDraftSyncToken += 1
            inlineDraftSyncJob?.cancel()
            inlineDraftSyncJob = null

            logger.d { "Confirming document: $documentId" }
            updateState { copy(isConfirming = true) }

            launch {
                if (hasUnsavedChanges) {
                    val updateResult = updateDocumentDraft(
                        documentId,
                        UpdateDraftRequest(extractedData = updatedData)
                    )
                    val updateFailure = updateResult.exceptionOrNull()
                    if (updateFailure != null) {
                        logger.e(updateFailure) { "Failed to save draft before confirm: $documentId" }
                        withState<DocumentReviewState.Content, _> {
                            updateState { copy(isConfirming = false) }
                        }
                        action(DocumentReviewAction.ShowError(updateFailure.asDokusException))
                        return@launch
                    }
                    val savedData = updateResult.getOrThrow().extractedData
                    withState<DocumentReviewState.Content, _> {
                        updateState {
                            copy(
                                draftData = savedData,
                                originalData = savedData,
                                hasUnsavedChanges = false,
                                isContactRequired = savedData.isContactRequired,
                            )
                        }
                    }
                }

                confirmDocument(documentId).fold(
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
                                    draftData = draft?.extractedData,
                                    originalData = draft?.extractedData,
                                    hasUnsavedChanges = false,
                                    isConfirming = false,
                                    isDocumentConfirmed = isConfirmed,
                                    isDocumentRejected = isRejected,
                                    confirmedCashflowEntryId = cashflowEntryId,
                                    isContactRequired = draft?.extractedData?.let {
                                        it.isContactRequired
                                    } ?: isContactRequired,
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
                        if (cashflowEntryId != null) {
                            intent(DocumentReviewIntent.LoadCashflowEntry)
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
                    is FinancialDocumentDto.ExpenseDto -> entity.id.toString()
                    is FinancialDocumentDto.CreditNoteDto -> entity.id.toString()
                    is FinancialDocumentDto.ProFormaDto -> entity.id.toString()
                    is FinancialDocumentDto.QuoteDto -> entity.id.toString()
                    is FinancialDocumentDto.PurchaseOrderDto -> entity.id.toString()
                }
            }
            if (confirmedEntityId != null) {
                action(
                    DocumentReviewAction.NavigateToEntity(
                        entityId = confirmedEntityId,
                        entityType = draftData.documentType
                    )
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.refreshAfterDraftUpdate(documentId: DocumentId) {
        getDocumentRecord(documentId).fold(
            onSuccess = { record ->
                val draft = record.draft
                withState<DocumentReviewState.Content, _> {
                    updateState {
                        copy(
                            document = record,
                            draftData = draft?.extractedData,
                            originalData = draft?.extractedData,
                            isContactRequired = draft?.extractedData?.let {
                                it.isContactRequired
                            } ?: isContactRequired,
                            counterpartyIntent = draft?.counterpartyIntent ?: CounterpartyIntent.None,
                            confirmedCashflowEntryId = record.cashflowEntryId,
                            isDocumentConfirmed = draft?.documentStatus == DocumentStatus.Confirmed,
                            isDocumentRejected = draft?.documentStatus == DocumentStatus.Rejected
                        )
                    }
                }
                if (record.cashflowEntryId != null) {
                    intent(DocumentReviewIntent.LoadCashflowEntry)
                }
            },
            onFailure = { error ->
                logger.w(error) { "Failed to refresh draft after update" }
            }
        )
    }
}
