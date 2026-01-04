package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftContactUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase
import tech.dokus.features.contacts.usecases.GetContactUseCase
import tech.dokus.foundation.platform.Logger

internal typealias DocumentReviewCtx = PipelineContext<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction>

/**
 * Container for the Document Review screen using FlowMVI.
 *
 * Manages document review workflow:
 * - Loading document processing details with AI extraction
 * - Editing extracted fields with change tracking
 * - Saving drafts for later review
 * - Confirming documents (creates entities)
 * - Rejecting documents
 * - Navigation to document chat
 *
 * Audit Trail:
 * - Original AI draft is preserved on first edit
 * - Each correction is tracked with timestamp
 * - Draft version increments on each save
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
@Suppress("LongParameterList") // Explicit use case wiring keeps intent boundaries clear.
internal class DocumentReviewContainer(
    private val getDocumentRecord: GetDocumentRecordUseCase,
    private val updateDocumentDraft: UpdateDocumentDraftUseCase,
    private val updateDocumentDraftContact: UpdateDocumentDraftContactUseCase,
    private val confirmDocument: ConfirmDocumentUseCase,
    private val rejectDocument: RejectDocumentUseCase,
    private val reprocessDocument: ReprocessDocumentUseCase,
    private val getDocumentPages: GetDocumentPagesUseCase,
    private val getContact: GetContactUseCase,
) : Container<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> {

    private val logger = Logger.forClass<DocumentReviewContainer>()
    private val reducer = DocumentReviewReducer(
        getDocumentRecord = getDocumentRecord,
        updateDocumentDraft = updateDocumentDraft,
        updateDocumentDraftContact = updateDocumentDraftContact,
        confirmDocument = confirmDocument,
        rejectDocument = rejectDocument,
        reprocessDocument = reprocessDocument,
        getDocumentPages = getDocumentPages,
        getContact = getContact,
        logger = logger,
    )

    override val store: Store<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> =
        store(DocumentReviewState.Loading) {
            init {
                // No auto-initialization - wait for LoadDocument intent with documentId
            }

            reduce { intent ->
                with(reducer) {
                    when (intent) {
                        // === Data Loading ===
                        is DocumentReviewIntent.LoadDocument -> handleLoadDocument(intent.documentId)
                        is DocumentReviewIntent.Refresh -> handleRefresh()

                        // === Preview ===
                        is DocumentReviewIntent.LoadPreviewPages -> handleLoadPreviewPages()
                        is DocumentReviewIntent.LoadMorePages -> handleLoadMorePages(intent.maxPages)
                        is DocumentReviewIntent.RetryLoadPreview -> handleLoadPreviewPages()
                        is DocumentReviewIntent.OpenPreviewSheet -> handleOpenPreviewSheet()
                        is DocumentReviewIntent.ClosePreviewSheet -> handleClosePreviewSheet()

                        // === Field Editing ===
                        is DocumentReviewIntent.UpdateInvoiceField -> handleUpdateInvoiceField(
                            intent.field,
                            intent.value
                        )
                        is DocumentReviewIntent.UpdateBillField -> handleUpdateBillField(intent.field, intent.value)
                        is DocumentReviewIntent.UpdateExpenseField -> handleUpdateExpenseField(
                            intent.field,
                            intent.value
                        )

                        // === Contact Selection (with backend persist) ===
                        is DocumentReviewIntent.SelectContact -> handleSelectContact(intent.contactId)
                        is DocumentReviewIntent.AcceptSuggestedContact -> handleAcceptSuggestedContact()
                        is DocumentReviewIntent.ClearSelectedContact -> handleClearSelectedContact()
                        is DocumentReviewIntent.ContactCreated -> handleContactCreated(intent.contactId)
                        is DocumentReviewIntent.SetCounterpartyIntent -> handleSetCounterpartyIntent(intent.intent)

                        // === Line Items ===
                        is DocumentReviewIntent.AddLineItem -> handleAddLineItem()
                        is DocumentReviewIntent.UpdateLineItem -> handleUpdateLineItem(intent.index, intent.item)
                        is DocumentReviewIntent.RemoveLineItem -> handleRemoveLineItem(intent.index)

                        // === Provenance ===
                        is DocumentReviewIntent.SelectFieldForProvenance -> handleSelectFieldForProvenance(
                            intent.fieldPath
                        )

                        // === Actions ===
                        is DocumentReviewIntent.SaveDraft -> handleSaveDraft()
                        is DocumentReviewIntent.DiscardChanges -> handleDiscardChanges()
                        is DocumentReviewIntent.ConfirmDiscardChanges -> handleConfirmDiscardChanges()
                        is DocumentReviewIntent.Confirm -> handleConfirm()
                        is DocumentReviewIntent.OpenChat -> handleOpenChat()

                        // === Reject Dialog ===
                        is DocumentReviewIntent.ShowRejectDialog -> handleShowRejectDialog()
                        is DocumentReviewIntent.DismissRejectDialog -> handleDismissRejectDialog()
                        is DocumentReviewIntent.SelectRejectReason -> handleSelectRejectReason(intent.reason)
                        is DocumentReviewIntent.UpdateRejectNote -> handleUpdateRejectNote(intent.note)
                        is DocumentReviewIntent.ConfirmReject -> handleConfirmReject()

                        // === Failed Analysis ===
                        is DocumentReviewIntent.RetryAnalysis -> handleRetryAnalysis()
                        is DocumentReviewIntent.DismissFailureBanner -> handleDismissFailureBanner()
                    }
                }
            }
        }
}
