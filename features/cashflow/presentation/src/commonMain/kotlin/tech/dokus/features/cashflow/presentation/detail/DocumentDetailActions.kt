package tech.dokus.features.cashflow.presentation.detail

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.RejectDocumentRequest
import tech.dokus.domain.model.UpdateDraftRequest
import tech.dokus.domain.model.isContactRequired
import tech.dokus.domain.model.from
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.UnconfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftContactUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase
import tech.dokus.features.cashflow.presentation.detail.mvi.payment.DocumentPaymentIntent
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal class DocumentDetailActions(
    private val updateDocumentDraft: UpdateDocumentDraftUseCase,
    private val updateDocumentDraftContact: UpdateDocumentDraftContactUseCase,
    private val confirmDocument: ConfirmDocumentUseCase,
    private val unconfirmDocument: UnconfirmDocumentUseCase,
    private val rejectDocument: RejectDocumentUseCase,
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val logger: Logger,
) {
    private data class DraftSyncPayload(
        val documentId: DocumentId,
        val draftData: DocDto,
        val token: Long,
    )

    private var inlineDraftSyncJob: Job? = null
    private var inlineDraftSyncToken: Long = 0L

    suspend fun DocumentDetailCtx.syncDraftImmediately() {
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
                UpdateDraftRequest(extractedData = DocumentDraftData.from(syncPayload.draftData))
            )

            result.fold(
                onSuccess = { response ->
                    if (syncPayload.token != inlineDraftSyncToken) return@fold
                    inlineDraftSyncJob = null
                    val responseContent = DocDto.from(response.extractedData)
                    withState {
                        val currentData = documentData ?: return@withState
                        updateState {
                            copy(
                                document = DokusState.success(
                                    currentData.copy(
                                        draftData = responseContent,
                                        originalData = responseContent,
                                    )
                                ),
                                hasUnsavedChanges = false,
                                isSaving = false,
                                isContactRequired = responseContent.isContactRequired,
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
                    action(DocumentDetailAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    suspend fun DocumentDetailCtx.handleConfirm() {
        withState {
            if (!canConfirm) {
                action(
                    DocumentDetailAction.ShowError(
                        DokusException.Validation.DocumentMissingFields
                    )
                )
                return@withState
            }

            val activeDocumentId = documentId ?: return@withState
            val updatedData = draftData
            if (updatedData == null) {
                action(DocumentDetailAction.ShowError(DokusException.Validation.DocumentMissingFields))
                return@withState
            }

            // Capture effective contact for auto-bind (before launch to avoid stale reads)
            val effective = effectiveContact

            inlineDraftSyncToken += 1
            inlineDraftSyncJob?.cancel()
            inlineDraftSyncJob = null

            logger.d { "Confirming document: $activeDocumentId" }
            updateState { copy(isConfirming = true) }

            launch {
                // Auto-bind suggested contact before confirming
                when (effective) {
                    is ResolvedContact.Linked -> { /* already linked, proceed */ }
                    is ResolvedContact.Suggested -> {
                        logger.d { "Auto-binding suggested contact ${effective.contactId} for $activeDocumentId" }
                        updateState { copy(isBindingContact = true) }
                        val bindResult = updateDocumentDraftContact(activeDocumentId, effective.contactId)
                        if (bindResult.isFailure) {
                            val error = bindResult.exceptionOrNull()!!
                            logger.e(error) { "Failed to auto-bind contact before confirm: $activeDocumentId" }
                            updateState { copy(isBindingContact = false, isConfirming = false) }
                            action(DocumentDetailAction.ShowError(error.asDokusException))
                            return@launch
                        }
                        updateState {
                            copy(
                                selectedContactOverride = ResolvedContact.Linked(
                                    contactId = effective.contactId,
                                    name = effective.name,
                                    vatNumber = effective.vatNumber,
                                    email = null,
                                    avatarPath = null,
                                ),
                                isBindingContact = false,
                            )
                        }
                    }
                    is ResolvedContact.Detected -> { /* backend auto-creates, just proceed */ }
                    is ResolvedContact.Unknown -> { /* should be blocked by canConfirm */ }
                }

                if (hasUnsavedChanges) {
                    val updateResult = updateDocumentDraft(
                        activeDocumentId,
                        UpdateDraftRequest(extractedData = DocumentDraftData.from(updatedData))
                    )
                    val updateFailure = updateResult.exceptionOrNull()
                    if (updateFailure != null) {
                        logger.e(updateFailure) { "Failed to save draft before confirm: $activeDocumentId" }
                        withState {
                            updateState { copy(isConfirming = false) }
                        }
                        action(DocumentDetailAction.ShowError(updateFailure.asDokusException))
                        return@launch
                    }
                    val savedContent = DocDto.from(updateResult.getOrThrow().extractedData)
                    withState {
                        val currentData = documentData ?: return@withState
                        updateState {
                            copy(
                                document = DokusState.success(
                                    currentData.copy(
                                        draftData = savedContent,
                                        originalData = savedContent,
                                    )
                                ),
                                hasUnsavedChanges = false,
                                isContactRequired = savedContent.isContactRequired,
                            )
                        }
                    }
                }

                confirmDocument(activeDocumentId).fold(
                    onSuccess = { record ->
                        val draft = record.draft
                        val cashflowEntryId = record.cashflowEntryId
                        withState {
                            val currentData = documentData ?: return@withState
                            updateState {
                                copy(
                                    document = DokusState.success(
                                        currentData.copy(
                                            documentRecord = record,
                                        )
                                    ),
                                    hasUnsavedChanges = false,
                                    isConfirming = false,
                                    documentStatus = draft?.documentStatus,
                                    confirmedCashflowEntryId = cashflowEntryId,
                                )
                            }
                        }
                        if (cashflowEntryId != null) {
                            intent(DocumentDetailIntent.Payment(DocumentPaymentIntent.LoadCashflowEntry))
                        }
                        action(DocumentDetailAction.ShowSuccess(DocumentDetailSuccess.DocumentConfirmed))
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to confirm document: $activeDocumentId" }
                        withState {
                            updateState { copy(isConfirming = false) }
                        }
                        action(DocumentDetailAction.ShowError(error.asDokusException))
                    }
                )
            }
        }
    }

    // === Unconfirm Handler ===

    suspend fun DocumentDetailCtx.handleUnconfirm() {
        var activeDocumentId: DocumentId? = null
        withState {
            activeDocumentId = documentId
            if (activeDocumentId == null || !isDocumentConfirmed) return@withState
            updateState { copy(isConfirming = true) }
        }
        val docId = activeDocumentId ?: return

        unconfirmDocument(docId).fold(
            onSuccess = { record ->
                val draft = record.draft
                withState {
                    val currentData = documentData ?: return@withState
                    updateState {
                        copy(
                            document = DokusState.success(
                                currentData.copy(documentRecord = record)
                            ),
                            hasUnsavedChanges = false,
                            isConfirming = false,
                            documentStatus = draft?.documentStatus,
                            confirmedCashflowEntryId = null,
                            cashflowEntryState = DokusState.idle(),
                            autoPaymentStatus = DokusState.idle(),
                        )
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to unconfirm document: $docId" }
                withState {
                    updateState { copy(isConfirming = false) }
                }
                action(DocumentDetailAction.ShowError(error.asDokusException))
            }
        )
    }

    // === Reject Dialog Handlers ===

    suspend fun DocumentDetailCtx.handleShowRejectDialog() {
        updateState { copy(rejectDialogState = RejectDialogState()) }
    }

    suspend fun DocumentDetailCtx.handleDismissRejectDialog() {
        updateState { copy(rejectDialogState = null) }
    }

    suspend fun DocumentDetailCtx.handleSelectRejectReason(reason: DocumentRejectReason) {
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

    suspend fun DocumentDetailCtx.handleUpdateRejectNote(note: String) {
        withState {
            rejectDialogState?.let { dialogState ->
                updateState {
                    copy(rejectDialogState = dialogState.copy(otherNote = note))
                }
            }
        }
    }

    suspend fun DocumentDetailCtx.handleConfirmReject() {
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
                                        documentStatus = draft?.documentStatus,
                                        rejectDialogState = null
                                    )
                                }
                            }
                            action(DocumentDetailAction.NavigateBack)
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
                            action(DocumentDetailAction.ShowError(error.asDokusException))
                        }
                    )
            }
        }
    }

    suspend fun DocumentDetailCtx.handleViewCashflowEntry() {
        withState {
            val entryId = confirmedCashflowEntryId ?: return@withState
            action(DocumentDetailAction.NavigateToCashflowEntry(entryId))
        }
    }

    suspend fun DocumentDetailCtx.handleViewEntity() {
        withState {
            val content = documentRecord?.draft?.content
            val confirmedEntityId = when (content) {
                is DocDto.Invoice.Confirmed -> content.id.toString()
                is DocDto.CreditNote.Confirmed -> content.id.toString()
                is DocDto.Receipt.Confirmed -> content.id.toString()
                else -> null
            }
            if (confirmedEntityId != null) {
                action(
                    DocumentDetailAction.NavigateToEntity(
                        entityId = confirmedEntityId,
                        entityType = draftData.documentType
                    )
                )
            }
        }
    }

    suspend fun DocumentDetailCtx.refreshAfterDraftUpdate(documentId: DocumentId) {
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
                                )
                            ),
                            confirmedCashflowEntryId = record.cashflowEntryId,
                            documentStatus = draft?.documentStatus,
                        )
                    }
                }
                if (record.cashflowEntryId != null) {
                    intent(DocumentDetailIntent.Payment(DocumentPaymentIntent.LoadCashflowEntry))
                }
            },
            onFailure = { error ->
                logger.w(error) { "Failed to refresh draft after update" }
            }
        )
    }
}
