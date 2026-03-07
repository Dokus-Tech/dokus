package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.AutoPaymentStatusDto
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.contact.SuggestedContact
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewLoader(
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val getContact: GetContactUseCase,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleLoadDocument(documentId: DocumentId) {
        logger.d { "Loading document: $documentId" }
        val routeSnapshot = currentRouteSnapshot()
        updateState {
            DocumentReviewState.Loading(
                queueState = routeSnapshot.queueState,
                selectedQueueDocumentId = if (routeSnapshot.queueState != null) {
                    documentId
                } else {
                    null
                }
            )
        }

        fetchDocumentProcessing(documentId)
    }

    suspend fun DocumentReviewCtx.handleRefresh() {
        withState<DocumentReviewState.Content, _> {
            logger.d { "Refreshing document: $documentId" }
            fetchDocumentProcessing(documentId)
        }
        withState<DocumentReviewState.AwaitingExtraction, _> {
            logger.d { "Refreshing document: $documentId" }
            fetchDocumentProcessing(documentId)
        }
    }

    suspend fun DocumentReviewCtx.handleApplyRemoteSnapshot(record: DocumentRecordDto) {
        val routeSnapshot = currentRouteSnapshot()
        val activeDocumentId = routeSnapshot.activeDocumentId ?: run {
            logger.d { "Dropping remote snapshot: no active document in current state" }
            return
        }
        if (activeDocumentId != record.document.id) return

        logger.d { "Applying remote snapshot for document: $activeDocumentId" }
        transitionToDocumentState(
            documentId = activeDocumentId,
            document = record,
            routeSnapshot = routeSnapshot,
            contentSnapshot = currentContentSnapshot(),
        )
    }

    private suspend fun DocumentReviewCtx.fetchDocumentProcessing(documentId: DocumentId) {
        getDocumentRecord(documentId)
            .fold(
                onSuccess = { document ->
                    transitionToDocumentState(
                        documentId = documentId,
                        document = document,
                        routeSnapshot = currentRouteSnapshot(),
                        contentSnapshot = currentContentSnapshot(),
                    )
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load document: $documentId" }
                    updateState {
                        DocumentReviewState.Error(
                            exception = error.asDokusException,
                            retryHandler = { intent(DocumentReviewIntent.LoadDocument(documentId)) }
                        )
                    }
                }
            )
    }

    private suspend fun DocumentReviewCtx.transitionToDocumentState(
        documentId: DocumentId,
        document: DocumentRecordDto,
        routeSnapshot: RouteSnapshot,
        contentSnapshot: ContentSnapshot?,
    ) {
        val previewUrl = document.document.downloadUrl
        val shouldReloadPreview = routeSnapshot.previewUrl != previewUrl ||
            routeSnapshot.previewState is DocumentPreviewState.Loading
        val previewState = if (routeSnapshot.previewUrl == previewUrl) {
            routeSnapshot.previewState
        } else {
            DocumentPreviewState.Loading
        }
        val selectedQueueDocumentId = if (routeSnapshot.queueState != null) documentId else null
        val draft = document.draft
        val extractedData = draft?.extractedData

        if (extractedData == null) {
            val isFailed = document.latestIngestion?.status == IngestionStatus.Failed
            if (isFailed) {
                updateState {
                    DocumentReviewState.Content(
                        documentId = documentId,
                        document = document,
                        draftData = null,
                        originalData = null,
                        hasUnsavedChanges = contentSnapshot?.hasUnsavedChanges ?: false,
                        isSaving = contentSnapshot?.isSaving ?: false,
                        isConfirming = contentSnapshot?.isConfirming ?: false,
                        selectedFieldPath = contentSnapshot?.selectedFieldPath,
                        previewUrl = previewUrl,
                        previewState = previewState,
                        selectedContactSnapshot = contentSnapshot?.selectedContactSnapshot,
                        contactSuggestions = emptyList(),
                        selectedContactId = null,
                        contactSelectionState = ContactSelectionState.NoContact,
                        isContactRequired = false,
                        counterpartyIntent = draft?.counterpartyIntent ?: tech.dokus.domain.enums.CounterpartyIntent.None,
                        contactValidationError = contentSnapshot?.contactValidationError,
                        isBindingContact = contentSnapshot?.isBindingContact ?: false,
                        isRejecting = contentSnapshot?.isRejecting ?: false,
                        isResolvingMatchReview = contentSnapshot?.isResolvingMatchReview ?: false,
                        isDocumentConfirmed = draft?.documentStatus == DocumentStatus.Confirmed,
                        isDocumentRejected = draft?.documentStatus == DocumentStatus.Rejected,
                        confirmedCashflowEntryId = document.cashflowEntryId,
                        cashflowEntryState = contentSnapshot?.cashflowEntryState ?: DokusState.idle(),
                        autoPaymentStatus = contentSnapshot?.autoPaymentStatus ?: DokusState.idle(),
                        isUndoingAutoPayment = contentSnapshot?.isUndoingAutoPayment ?: false,
                        isEditMode = contentSnapshot?.isEditMode ?: false,
                        sourceViewerState = contentSnapshot?.sourceViewerState
                            ?.takeIf { viewer -> document.sources.any { it.id == viewer.sourceId } },
                        paymentSheetState = contentSnapshot?.paymentSheetState,
                        rejectDialogState = contentSnapshot?.rejectDialogState,
                        feedbackDialogState = contentSnapshot?.feedbackDialogState,
                        failureBannerDismissed = contentSnapshot?.failureBannerDismissed ?: false,
                        showContactSheet = contentSnapshot?.showContactSheet ?: false,
                        contactSheetSearchQuery = contentSnapshot?.contactSheetSearchQuery.orEmpty(),
                        contactSheetContacts = contentSnapshot?.contactSheetContacts ?: DokusState.idle(),
                        queueState = routeSnapshot.queueState,
                        selectedQueueDocumentId = selectedQueueDocumentId,
                    )
                }
            } else {
                updateState {
                    DocumentReviewState.AwaitingExtraction(
                        documentId = documentId,
                        document = document,
                        previewUrl = previewUrl,
                        previewState = previewState,
                        queueState = routeSnapshot.queueState,
                        selectedQueueDocumentId = selectedQueueDocumentId,
                    )
                }
            }

            if (shouldReloadPreview) {
                intent(DocumentReviewIntent.LoadPreviewPages)
            }
            return
        }

        val contactSuggestions = draft.contactSuggestions.map { suggestion ->
            ContactSuggestion(
                contactId = suggestion.contactId,
                name = suggestion.name,
                vatNumber = suggestion.vatNumber?.value
            )
        }
        val documentStatus = draft.documentStatus
        val isDocumentConfirmed = documentStatus == DocumentStatus.Confirmed
        val isDocumentRejected = documentStatus == DocumentStatus.Rejected
        val counterpartyIntent = draft.counterpartyIntent
        val (contactSelectionState, linkedContactId, selectedContactSnapshot) =
            buildContactSelectionState(document, draft.contactSuggestions)
        val preservedContactSnapshot = when {
            linkedContactId != null && linkedContactId == contentSnapshot?.selectedContactId ->
                contentSnapshot.selectedContactSnapshot
            else -> selectedContactSnapshot
        }

        updateState {
            DocumentReviewState.Content(
                documentId = documentId,
                document = document,
                draftData = extractedData,
                originalData = draft.aiDraftData ?: extractedData,
                hasUnsavedChanges = contentSnapshot?.hasUnsavedChanges ?: false,
                isSaving = contentSnapshot?.isSaving ?: false,
                isConfirming = contentSnapshot?.isConfirming ?: false,
                selectedFieldPath = contentSnapshot?.selectedFieldPath,
                previewUrl = previewUrl,
                contactSuggestions = contactSuggestions,
                previewState = previewState,
                selectedContactId = linkedContactId,
                selectedContactSnapshot = preservedContactSnapshot,
                contactSelectionState = contactSelectionState,
                isContactRequired = extractedData.isContactRequired,
                counterpartyIntent = counterpartyIntent,
                contactValidationError = contentSnapshot?.contactValidationError,
                isBindingContact = contentSnapshot?.isBindingContact ?: false,
                isRejecting = contentSnapshot?.isRejecting ?: false,
                isResolvingMatchReview = contentSnapshot?.isResolvingMatchReview ?: false,
                isDocumentConfirmed = isDocumentConfirmed,
                isDocumentRejected = isDocumentRejected,
                confirmedCashflowEntryId = document.cashflowEntryId,
                cashflowEntryState = contentSnapshot?.cashflowEntryState ?: DokusState.idle(),
                autoPaymentStatus = contentSnapshot?.autoPaymentStatus ?: DokusState.idle(),
                isUndoingAutoPayment = contentSnapshot?.isUndoingAutoPayment ?: false,
                isEditMode = contentSnapshot?.isEditMode ?: false,
                sourceViewerState = contentSnapshot?.sourceViewerState
                    ?.takeIf { viewer -> document.sources.any { it.id == viewer.sourceId } },
                paymentSheetState = contentSnapshot?.paymentSheetState,
                rejectDialogState = contentSnapshot?.rejectDialogState,
                feedbackDialogState = contentSnapshot?.feedbackDialogState,
                failureBannerDismissed = contentSnapshot?.failureBannerDismissed ?: false,
                showContactSheet = contentSnapshot?.showContactSheet ?: false,
                contactSheetSearchQuery = contentSnapshot?.contactSheetSearchQuery.orEmpty(),
                contactSheetContacts = contentSnapshot?.contactSheetContacts ?: DokusState.idle(),
                queueState = routeSnapshot.queueState,
                selectedQueueDocumentId = selectedQueueDocumentId,
            )
        }

        if (shouldReloadPreview) {
            intent(DocumentReviewIntent.LoadPreviewPages)
        }
        if (document.cashflowEntryId != null && document.cashflowEntryId != contentSnapshot?.confirmedCashflowEntryId) {
            intent(DocumentReviewIntent.LoadCashflowEntry)
        }
        if (linkedContactId != null && preservedContactSnapshot == null) {
            fetchContactSnapshot(linkedContactId)
        }
    }

    private fun buildContactSelectionState(
        document: DocumentRecordDto,
        suggestions: List<SuggestedContact>
    ): Triple<ContactSelectionState, ContactId?, ContactSnapshot?> {
        val draft = document.draft ?: return Triple(ContactSelectionState.NoContact, null, null)
        val linkedContactId = draft.linkedContactId
        if (linkedContactId != null) {
            return Triple(ContactSelectionState.Selected, linkedContactId, null)
        }
        val topSuggestion = suggestions.firstOrNull()
        if (topSuggestion != null) {
            return Triple(
                ContactSelectionState.Suggested(
                    contactId = topSuggestion.contactId,
                    name = topSuggestion.name,
                    vatNumber = topSuggestion.vatNumber?.value,
                ),
                null,
                null
            )
        }
        return Triple(ContactSelectionState.NoContact, null, null)
    }

    private suspend fun DocumentReviewCtx.fetchContactSnapshot(contactId: ContactId) {
        getContact(contactId).fold(
            onSuccess = { contact ->
                withState<DocumentReviewState.Content, _> {
                    updateState {
                        copy(
                            selectedContactSnapshot = ContactSnapshot(
                                id = contact.id,
                                name = contact.name.value,
                                vatNumber = contact.vatNumber?.value,
                                email = contact.email?.value,
                                avatarUrl = contact.avatar?.small,
                            )
                        )
                    }
                }
            },
            onFailure = { error ->
                logger.w(error) { "Failed to fetch contact snapshot for $contactId" }
            }
        )
    }

    private suspend fun DocumentReviewCtx.currentRouteSnapshot(): RouteSnapshot {
        var snapshot = RouteSnapshot()
        withState<DocumentReviewState.Loading, _> {
            snapshot = RouteSnapshot(
                activeDocumentId = null,
                queueState = queueState,
                selectedQueueDocumentId = selectedQueueDocumentId,
            )
        }
        withState<DocumentReviewState.AwaitingExtraction, _> {
            snapshot = RouteSnapshot(
                activeDocumentId = documentId,
                queueState = queueState,
                selectedQueueDocumentId = selectedQueueDocumentId,
                previewUrl = previewUrl,
                previewState = previewState,
            )
        }
        withState<DocumentReviewState.Content, _> {
            snapshot = RouteSnapshot(
                activeDocumentId = documentId,
                queueState = queueState,
                selectedQueueDocumentId = selectedQueueDocumentId,
                previewUrl = previewUrl,
                previewState = previewState,
            )
        }
        return snapshot
    }

    private suspend fun DocumentReviewCtx.currentContentSnapshot(): ContentSnapshot? {
        var snapshot: ContentSnapshot? = null
        withState<DocumentReviewState.Content, _> {
            snapshot = ContentSnapshot(
                hasUnsavedChanges = hasUnsavedChanges,
                isSaving = isSaving,
                isConfirming = isConfirming,
                selectedFieldPath = selectedFieldPath,
                selectedContactId = selectedContactId,
                selectedContactSnapshot = selectedContactSnapshot,
                contactValidationError = contactValidationError,
                isBindingContact = isBindingContact,
                isRejecting = isRejecting,
                isResolvingMatchReview = isResolvingMatchReview,
                confirmedCashflowEntryId = confirmedCashflowEntryId,
                cashflowEntryState = cashflowEntryState,
                autoPaymentStatus = autoPaymentStatus,
                isUndoingAutoPayment = isUndoingAutoPayment,
                isEditMode = isEditMode,
                sourceViewerState = sourceViewerState,
                paymentSheetState = paymentSheetState,
                rejectDialogState = rejectDialogState,
                feedbackDialogState = feedbackDialogState,
                failureBannerDismissed = failureBannerDismissed,
                showContactSheet = showContactSheet,
                contactSheetSearchQuery = contactSheetSearchQuery,
                contactSheetContacts = contactSheetContacts,
            )
        }
        return snapshot
    }

    private data class RouteSnapshot(
        val activeDocumentId: DocumentId? = null,
        val queueState: DocumentReviewQueueState? = null,
        val selectedQueueDocumentId: DocumentId? = null,
        val previewUrl: String? = null,
        val previewState: DocumentPreviewState = DocumentPreviewState.Loading,
    )

    private data class ContentSnapshot(
        val hasUnsavedChanges: Boolean,
        val isSaving: Boolean,
        val isConfirming: Boolean,
        val selectedFieldPath: String?,
        val selectedContactId: ContactId?,
        val selectedContactSnapshot: ContactSnapshot?,
        val contactValidationError: tech.dokus.domain.exceptions.DokusException?,
        val isBindingContact: Boolean,
        val isRejecting: Boolean,
        val isResolvingMatchReview: Boolean,
        val confirmedCashflowEntryId: tech.dokus.domain.ids.CashflowEntryId?,
        val cashflowEntryState: DokusState<CashflowEntry>,
        val autoPaymentStatus: DokusState<AutoPaymentStatusDto>,
        val isUndoingAutoPayment: Boolean,
        val isEditMode: Boolean,
        val sourceViewerState: SourceEvidenceViewerState?,
        val paymentSheetState: PaymentSheetState?,
        val rejectDialogState: RejectDialogState?,
        val feedbackDialogState: FeedbackDialogState?,
        val failureBannerDismissed: Boolean,
        val showContactSheet: Boolean,
        val contactSheetSearchQuery: String,
        val contactSheetContacts: DokusState<List<tech.dokus.domain.model.contact.ContactDto>>,
    )
}
