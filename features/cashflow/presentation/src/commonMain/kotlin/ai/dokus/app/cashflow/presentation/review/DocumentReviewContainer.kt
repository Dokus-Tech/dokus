package ai.dokus.app.cashflow.presentation.review

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.contacts.usecases.GetContactUseCase
import ai.dokus.foundation.platform.Logger
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce

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
internal class DocumentReviewContainer(
    private val dataSource: CashflowRemoteDataSource,
    private val getContact: GetContactUseCase,
) : Container<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> {

    private val logger = Logger.forClass<DocumentReviewContainer>()
    private val reducer = DocumentReviewReducer(
        dataSource = dataSource,
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
                    is DocumentReviewIntent.UpdateInvoiceField -> handleUpdateInvoiceField(intent.field, intent.value)
                    is DocumentReviewIntent.UpdateBillField -> handleUpdateBillField(intent.field, intent.value)
                    is DocumentReviewIntent.UpdateExpenseField -> handleUpdateExpenseField(intent.field, intent.value)

                    // === Contact Selection (with backend persist) ===
                    is DocumentReviewIntent.SelectContact -> handleSelectContact(intent.contactId)
                    is DocumentReviewIntent.AcceptSuggestedContact -> handleAcceptSuggestedContact()
                    is DocumentReviewIntent.ClearSelectedContact -> handleClearSelectedContact()
                    is DocumentReviewIntent.ContactCreated -> handleContactCreated(intent.contactId)

                    // === Line Items ===
                    is DocumentReviewIntent.AddLineItem -> handleAddLineItem()
                    is DocumentReviewIntent.UpdateLineItem -> handleUpdateLineItem(intent.index, intent.item)
                    is DocumentReviewIntent.RemoveLineItem -> handleRemoveLineItem(intent.index)

                    // === Provenance ===
                    is DocumentReviewIntent.SelectFieldForProvenance -> handleSelectFieldForProvenance(intent.fieldPath)

                    // === Actions ===
                    is DocumentReviewIntent.SaveDraft -> handleSaveDraft()
                    is DocumentReviewIntent.DiscardChanges -> handleDiscardChanges()
                    is DocumentReviewIntent.Confirm -> handleConfirm()
                    is DocumentReviewIntent.Reject -> handleReject()
                    is DocumentReviewIntent.OpenChat -> handleOpenChat()
                    }
                }
            }
        }
}
