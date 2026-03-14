package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.model.contact.CounterpartyInfo
import tech.dokus.domain.model.contact.isLinked
import tech.dokus.domain.model.contact.isUnresolved
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase
import tech.dokus.foundation.app.state.DokusState
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
        withState {
            val activeDocumentId = documentId ?: return@withState
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
                documentId = activeDocumentId,
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
                    withState {
                        val currentData = documentData ?: return@withState
                        updateState {
                            copy(
                                document = DokusState.success(
                                    currentData.copy(
                                        draftData = response.extractedData,
                                        originalData = response.extractedData,
                                    )
                                ),
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
                    withState {
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
        withState {
            if (!canConfirm) {
                action(
                    DocumentReviewAction.ShowError(
                        DokusException.Validation.DocumentMissingFields
                    )
                )
                return@withState
            }

            val activeDocumentId = documentId ?: return@withState
            val updatedData = draftData
            if (updatedData == null) {
                action(DocumentReviewAction.ShowError(DokusException.Validation.DocumentMissingFields))
                return@withState
            }

            inlineDraftSyncToken += 1
            inlineDraftSyncJob?.cancel()
            inlineDraftSyncJob = null

            logger.d { "Confirming document: $activeDocumentId" }
            updateState { copy(isConfirming = true) }

            launch {
                if (hasUnsavedChanges) {
                    val updateResult = updateDocumentDraft(
                        activeDocumentId,
                        UpdateDraftRequest(extractedData = updatedData)
                    )
                    val updateFailure = updateResult.exceptionOrNull()
                    if (updateFailure != null) {
                        logger.e(updateFailure) { "Failed to save draft before confirm: $activeDocumentId" }
                        withState {
                            updateState { copy(isConfirming = false) }
                        }
                        action(DocumentReviewAction.ShowError(updateFailure.asDokusException))
                        return@launch
                    }
                    val savedData = updateResult.getOrThrow().extractedData
                    withState {
                        val currentData = documentData ?: return@withState
                        updateState {
                            copy(
                                document = DokusState.success(
                                    currentData.copy(
                                        draftData = savedData,
                                        originalData = savedData,
                                    )
                                ),
                                hasUnsavedChanges = false,
                                isContactRequired = savedData.isContactRequired,
                            )
                        }
                    }
                }

                confirmDocument(activeDocumentId).fold(
                    onSuccess = { record ->
                        val draft = record.draft
                        val isConfirmed = draft?.documentStatus == DocumentStatus.Confirmed
                        val isRejected = draft?.documentStatus == DocumentStatus.Rejected
                        val isUnsupported = draft?.documentStatus == DocumentStatus.Unsupported
                        val cashflowEntryId = record.cashflowEntryId
                        withState {
                            val currentData = documentData ?: return@withState
                            val counterparty = draft?.counterparty
                            updateState {
                                copy(
                                    document = DokusState.success(
                                        currentData.copy(
                                            documentRecord = record,
                                            draftData = draft?.extractedData,
                                            originalData = draft?.extractedData,
                                        )
                                    ),
                                    hasUnsavedChanges = false,
                                    isConfirming = false,
                                    isDocumentConfirmed = isConfirmed,
                                    isDocumentRejected = isRejected,
                                    isDocumentUnsupported = isUnsupported,
                                    confirmedCashflowEntryId = cashflowEntryId,
                                    isContactRequired = draft?.extractedData?.let {
                                        it.isContactRequired
                                    } ?: isContactRequired,
                                    isPendingCreation = counterparty.isUnresolved() && counterparty.pendingCreation,
                                    selectedContactId = if (counterparty.isLinked()) counterparty.contactId else selectedContactId,
                                    contactSelectionState = if (counterparty.isLinked()) {
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
                        logger.e(error) { "Failed to confirm document: $activeDocumentId" }
                        withState {
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
        updateState { copy(rejectDialogState = RejectDialogState()) }
    }

    suspend fun DocumentReviewCtx.handleDismissRejectDialog() {
        updateState { copy(rejectDialogState = null) }
    }

    suspend fun DocumentReviewCtx.handleSelectRejectReason(reason: DocumentRejectReason) {
        withState {
            rejectDialogState?.let { dialogState ->
                updateState {
                    copy(
                        rejectDialogState = dialogState.copy(
                            selectedReason = reason,
                            otherNote = if (reason == DocumentRejectReason.Other) dialogState.otherNote else ""
                        )
                    )
                }
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateRejectNote(note: String) {
        withState {
            rejectDialogState?.let { dialogState ->
                updateState {
                    copy(rejectDialogState = dialogState.copy(otherNote = note))
                }
            }
        }
    }

    suspend fun DocumentReviewCtx.handleConfirmReject() {
        withState {
            val activeDocumentId = documentId ?: return@withState
            val dialogState = rejectDialogState ?: return@withState
            val reason = dialogState.selectedReason

            updateState {
                copy(
                    isRejecting = true,
                    rejectDialogState = dialogState.copy(isConfirming = true)
                )
            }

            launch {
                rejectDocument(activeDocumentId, RejectDocumentRequest(reason))
                    .fold(
                        onSuccess = { record ->
                            val draft = record.draft
                            withState {
                                val currentData = documentData ?: return@withState
                                updateState {
                                    copy(
                                        document = DokusState.success(
                                            currentData.copy(documentRecord = record)
                                        ),
                                        isRejecting = false,
                                        isDocumentRejected = draft?.documentStatus == DocumentStatus.Rejected,
                                        isDocumentConfirmed = draft?.documentStatus == DocumentStatus.Confirmed,
                                        isDocumentUnsupported = draft?.documentStatus == DocumentStatus.Unsupported,
                                        rejectDialogState = null
                                    )
                                }
                            }
                            action(DocumentReviewAction.NavigateBack)
                        },
                        onFailure = { error ->
                            logger.e(error) { "Failed to reject document: $activeDocumentId" }
                            withState {
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
        withState {
            val activeDocumentId = documentId ?: return@withState
            action(DocumentReviewAction.NavigateToChat(activeDocumentId))
        }
    }

    suspend fun DocumentReviewCtx.handleViewCashflowEntry() {
        withState {
            val entryId = confirmedCashflowEntryId ?: return@withState
            action(DocumentReviewAction.NavigateToCashflowEntry(entryId))
        }
    }

    suspend fun DocumentReviewCtx.handleViewEntity() {
        withState {
            val confirmedEntityId = documentRecord?.confirmedEntity?.let { entity ->
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
                withState {
                    val currentData = documentData ?: return@withState
                    updateState {
                        copy(
                            document = DokusState.success(
                                currentData.copy(
                                    documentRecord = record,
                                    draftData = draft?.extractedData,
                                    originalData = draft?.extractedData,
                                )
                            ),
                            isContactRequired = draft?.extractedData?.let {
                                it.isContactRequired
                            } ?: isContactRequired,
                            isPendingCreation = draft?.counterparty.let { it.isUnresolved() && it.pendingCreation },
                            confirmedCashflowEntryId = record.cashflowEntryId,
                            isDocumentConfirmed = draft?.documentStatus == DocumentStatus.Confirmed,
                            isDocumentRejected = draft?.documentStatus == DocumentStatus.Rejected,
                            isDocumentUnsupported = draft?.documentStatus == DocumentStatus.Unsupported,
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
