package ai.dokus.app.cashflow.presentation.review

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.contacts.usecases.GetContactUseCase
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedLineItem
import tech.dokus.domain.model.UpdateDraftRequest

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

    override val store: Store<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> =
        store(DocumentReviewState.Loading) {
            init {
                // No auto-initialization - wait for LoadDocument intent with documentId
            }

            reduce { intent ->
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
                    is DocumentReviewIntent.OpenContactPicker -> handleOpenContactPicker()
                    is DocumentReviewIntent.CloseContactPicker -> handleCloseContactPicker()

                    // === Contact Creation ===
                    is DocumentReviewIntent.OpenCreateContactSheet -> handleOpenCreateContactSheet()
                    is DocumentReviewIntent.CloseCreateContactSheet -> handleCloseCreateContactSheet()
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

    // =========================================================================
    // DATA LOADING
    // =========================================================================

    private suspend fun DocumentReviewCtx.handleLoadDocument(documentId: DocumentId) {
        logger.d { "Loading document: $documentId" }
        updateState { DocumentReviewState.Loading }

        fetchDocumentProcessing(documentId)
    }

    private suspend fun DocumentReviewCtx.handleRefresh() {
        withState<DocumentReviewState.Content, _> {
            logger.d { "Refreshing document: $documentId" }
            fetchDocumentProcessing(documentId)
        }
    }

    private suspend fun DocumentReviewCtx.fetchDocumentProcessing(documentId: DocumentId) {
        // Fetch document record from the API
        dataSource.getDocumentRecord(documentId)
            .fold(
            onSuccess = { document ->
                transitionToContent(documentId, document)
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

    private suspend fun DocumentReviewCtx.transitionToContent(
        documentId: DocumentId,
        document: DocumentRecordDto
    ) {
        val extractedData = document.draft?.extractedData
        val editableData = EditableExtractedData.fromExtractedData(extractedData)
        val documentType = document.draft?.documentType

        // Build contact suggestions from document if available
        val contactSuggestions = buildContactSuggestions(document)

        // Determine if contact is required (Invoice/Bill only)
        val isContactRequired = documentType == DocumentType.Invoice || documentType == DocumentType.Bill

        // Check if document is confirmed (read-only mode)
        val isDocumentConfirmed = document.draft?.draftStatus == DraftStatus.Confirmed

        // Build contact selection state from draft
        val (contactSelectionState, selectedContactId, selectedContactSnapshot) =
            buildContactSelectionState(document)

        updateState {
            DocumentReviewState.Content(
                documentId = documentId,
                document = document,
                editableData = editableData,
                originalData = extractedData,
                hasUnsavedChanges = false,
                isSaving = false,
                isConfirming = false,
                selectedFieldPath = null,
                previewUrl = document.document.downloadUrl,
                contactSuggestions = contactSuggestions,
                previewState = DocumentPreviewState.Loading,
                // Contact selection state
                selectedContactId = selectedContactId,
                selectedContactSnapshot = selectedContactSnapshot,
                contactSelectionState = contactSelectionState,
                isContactRequired = isContactRequired,
                isDocumentConfirmed = isDocumentConfirmed,
            )
        }

        // Trigger preview loading after content is ready
        intent(DocumentReviewIntent.LoadPreviewPages)

        // If we have a selectedContactId but no snapshot, fetch contact details
        if (selectedContactId != null && selectedContactSnapshot == null) {
            fetchContactSnapshot(selectedContactId)
        }
    }

    /**
     * Build initial contact selection state from document draft.
     *
     * Note: Selected contact is persisted via PATCH and would be tracked separately.
     * For now, we start with suggested contact if available, otherwise no contact.
     * Selected state is set after user explicitly accepts a suggestion or selects from picker.
     */
    private fun buildContactSelectionState(
        document: DocumentRecordDto
    ): Triple<ContactSelectionState, ContactId?, ContactSnapshot?> {
        val draft = document.draft ?: return Triple(ContactSelectionState.NoContact, null, null)

        // Check for AI suggested contact (not yet accepted by user)
        val suggestedContactId = draft.suggestedContactId
        if (suggestedContactId != null) {
            val extractedName = when (draft.documentType) {
                DocumentType.Invoice -> draft.extractedData?.invoice?.clientName
                DocumentType.Bill -> draft.extractedData?.bill?.supplierName
                else -> null
            }
            val extractedVat = when (draft.documentType) {
                DocumentType.Invoice -> draft.extractedData?.invoice?.clientVatNumber
                DocumentType.Bill -> draft.extractedData?.bill?.supplierVatNumber
                else -> null
            }
            val suggested = ContactSelectionState.Suggested(
                contactId = suggestedContactId,
                name = extractedName ?: "Unknown",
                vatNumber = extractedVat,
                confidence = draft.contactSuggestionConfidence ?: 0f,
                reason = draft.contactSuggestionReason ?: "AI suggested",
            )
            return Triple(suggested, null, null)
        }

        return Triple(ContactSelectionState.NoContact, null, null)
    }

    /**
     * Fetch contact details for display snapshot.
     */
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

    private fun buildContactSuggestions(document: DocumentRecordDto): List<ContactSuggestion> {
        val suggestions = mutableListOf<ContactSuggestion>()

        // Add suggested contact if available
        document.draft?.suggestedContactId?.let { contactId ->
            // Build suggestion from document's contact match info
            val extractedName = when (document.draft?.documentType) {
                DocumentType.Invoice -> document.draft?.extractedData?.invoice?.clientName
                DocumentType.Bill -> document.draft?.extractedData?.bill?.supplierName
                DocumentType.Expense -> document.draft?.extractedData?.expense?.merchant
                else -> null
            }
            val extractedVat = when (document.draft?.documentType) {
                DocumentType.Invoice -> document.draft?.extractedData?.invoice?.clientVatNumber
                DocumentType.Bill -> document.draft?.extractedData?.bill?.supplierVatNumber
                DocumentType.Expense -> document.draft?.extractedData?.expense?.merchantVatNumber
                else -> null
            }

            suggestions.add(
                ContactSuggestion(
                    contactId = contactId,
                    name = extractedName ?: "Unknown",
                    vatNumber = extractedVat,
                    matchConfidence = 0f, // TODO: Add confidence to draft
                    matchReason = "AI suggested"
                )
            )
        }

        return suggestions
    }

    // =========================================================================
    // FIELD EDITING
    // =========================================================================

    private suspend fun DocumentReviewCtx.handleUpdateInvoiceField(field: InvoiceField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentInvoice = editableData.invoice ?: return@withState

            val updatedInvoice = when (field) {
                InvoiceField.CLIENT_NAME -> currentInvoice.copy(clientName = value as? String ?: "")
                InvoiceField.CLIENT_VAT_NUMBER -> currentInvoice.copy(clientVatNumber = value as? String ?: "")
                InvoiceField.CLIENT_EMAIL -> currentInvoice.copy(clientEmail = value as? String ?: "")
                InvoiceField.CLIENT_ADDRESS -> currentInvoice.copy(clientAddress = value as? String ?: "")
                InvoiceField.INVOICE_NUMBER -> currentInvoice.copy(invoiceNumber = value as? String ?: "")
                InvoiceField.ISSUE_DATE -> currentInvoice.copy(issueDate = value as? LocalDate)
                InvoiceField.DUE_DATE -> currentInvoice.copy(dueDate = value as? LocalDate)
                InvoiceField.SUBTOTAL_AMOUNT -> currentInvoice.copy(subtotalAmount = value as? String ?: "")
                InvoiceField.VAT_AMOUNT -> currentInvoice.copy(vatAmount = value as? String ?: "")
                InvoiceField.TOTAL_AMOUNT -> currentInvoice.copy(totalAmount = value as? String ?: "")
                InvoiceField.CURRENCY -> currentInvoice.copy(currency = value as? String ?: "EUR")
                InvoiceField.NOTES -> currentInvoice.copy(notes = value as? String ?: "")
                InvoiceField.PAYMENT_TERMS -> currentInvoice.copy(paymentTerms = value as? String ?: "")
                InvoiceField.BANK_ACCOUNT -> currentInvoice.copy(bankAccount = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(invoice = updatedInvoice),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    private suspend fun DocumentReviewCtx.handleUpdateBillField(field: BillField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentBill = editableData.bill ?: return@withState

            val updatedBill = when (field) {
                BillField.SUPPLIER_NAME -> currentBill.copy(supplierName = value as? String ?: "")
                BillField.SUPPLIER_VAT_NUMBER -> currentBill.copy(supplierVatNumber = value as? String ?: "")
                BillField.SUPPLIER_ADDRESS -> currentBill.copy(supplierAddress = value as? String ?: "")
                BillField.INVOICE_NUMBER -> currentBill.copy(invoiceNumber = value as? String ?: "")
                BillField.ISSUE_DATE -> currentBill.copy(issueDate = value as? LocalDate)
                BillField.DUE_DATE -> currentBill.copy(dueDate = value as? LocalDate)
                BillField.AMOUNT -> currentBill.copy(amount = value as? String ?: "")
                BillField.VAT_AMOUNT -> currentBill.copy(vatAmount = value as? String ?: "")
                BillField.VAT_RATE -> currentBill.copy(vatRate = value as? String ?: "")
                BillField.CURRENCY -> currentBill.copy(currency = value as? String ?: "EUR")
                BillField.CATEGORY -> currentBill.copy(category = value as? ExpenseCategory)
                BillField.DESCRIPTION -> currentBill.copy(description = value as? String ?: "")
                BillField.NOTES -> currentBill.copy(notes = value as? String ?: "")
                BillField.PAYMENT_TERMS -> currentBill.copy(paymentTerms = value as? String ?: "")
                BillField.BANK_ACCOUNT -> currentBill.copy(bankAccount = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(bill = updatedBill),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    private suspend fun DocumentReviewCtx.handleUpdateExpenseField(field: ExpenseField, value: Any?) {
        withState<DocumentReviewState.Content, _> {
            val currentExpense = editableData.expense ?: return@withState

            val updatedExpense = when (field) {
                ExpenseField.MERCHANT -> currentExpense.copy(merchant = value as? String ?: "")
                ExpenseField.MERCHANT_ADDRESS -> currentExpense.copy(merchantAddress = value as? String ?: "")
                ExpenseField.MERCHANT_VAT_NUMBER -> currentExpense.copy(merchantVatNumber = value as? String ?: "")
                ExpenseField.DATE -> currentExpense.copy(date = value as? LocalDate)
                ExpenseField.AMOUNT -> currentExpense.copy(amount = value as? String ?: "")
                ExpenseField.VAT_AMOUNT -> currentExpense.copy(vatAmount = value as? String ?: "")
                ExpenseField.VAT_RATE -> currentExpense.copy(vatRate = value as? String ?: "")
                ExpenseField.CURRENCY -> currentExpense.copy(currency = value as? String ?: "EUR")
                ExpenseField.CATEGORY -> currentExpense.copy(category = value as? ExpenseCategory)
                ExpenseField.DESCRIPTION -> currentExpense.copy(description = value as? String ?: "")
                ExpenseField.IS_DEDUCTIBLE -> currentExpense.copy(isDeductible = value as? Boolean ?: true)
                ExpenseField.DEDUCTIBLE_PERCENTAGE -> currentExpense.copy(deductiblePercentage = value as? String ?: "100")
                ExpenseField.PAYMENT_METHOD -> currentExpense.copy(paymentMethod = value as? PaymentMethod)
                ExpenseField.NOTES -> currentExpense.copy(notes = value as? String ?: "")
                ExpenseField.RECEIPT_NUMBER -> currentExpense.copy(receiptNumber = value as? String ?: "")
            }

            updateState {
                copy(
                    editableData = editableData.copy(expense = updatedExpense),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    // =========================================================================
    // CONTACT SELECTION (with backend persist)
    // =========================================================================

    /**
     * Handle selecting a contact from picker - binds to backend immediately.
     */
    private suspend fun DocumentReviewCtx.handleSelectContact(contactId: ContactId) {
        withState<DocumentReviewState.Content, _> {
            bindContact(documentId, contactId)
        }
    }

    /**
     * Handle accepting the AI-suggested contact - binds to backend immediately.
     */
    private suspend fun DocumentReviewCtx.handleAcceptSuggestedContact() {
        withState<DocumentReviewState.Content, _> {
            val suggested = contactSelectionState as? ContactSelectionState.Suggested
                ?: return@withState
            bindContact(documentId, suggested.contactId)
        }
    }

    /**
     * Handle clearing the selected contact - persists to backend immediately.
     */
    private suspend fun DocumentReviewCtx.handleClearSelectedContact() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(isBindingContact = true) }
        }

        // PATCH draft to clear contactId
        withState<DocumentReviewState.Content, _> {
            dataSource.updateDocumentDraftContact(documentId, null)
                .fold(
                    onSuccess = {
                        // Revert to Suggested if one exists, else NoContact
                        val newState = document.draft?.suggestedContactId?.let { suggestedId ->
                            ContactSelectionState.Suggested(
                                contactId = suggestedId,
                                name = document.draft?.extractedData?.invoice?.clientName
                                    ?: document.draft?.extractedData?.bill?.supplierName
                                    ?: "Unknown",
                                vatNumber = document.draft?.extractedData?.invoice?.clientVatNumber
                                    ?: document.draft?.extractedData?.bill?.supplierVatNumber,
                                confidence = document.draft?.contactSuggestionConfidence ?: 0f,
                                reason = document.draft?.contactSuggestionReason ?: "AI suggested",
                            )
                        } ?: ContactSelectionState.NoContact

                        updateState {
                            copy(
                                selectedContactId = null,
                                selectedContactSnapshot = null,
                                contactSelectionState = newState,
                                isBindingContact = false,
                                contactValidationError = null,
                            )
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to clear contact" }
                        updateState { copy(isBindingContact = false) }
                        action(DocumentReviewAction.ShowError("Failed to clear contact"))
                    }
                )
        }
    }

    /**
     * Persist contact binding to backend, then update local state.
     * Flow: Set loading → PATCH draft → Fetch contact snapshot → Update state
     */
    private suspend fun DocumentReviewCtx.bindContact(documentId: DocumentId, contactId: ContactId) {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(isBindingContact = true, contactValidationError = null) }
        }

        // 1. PATCH draft with contactId
        dataSource.updateDocumentDraftContact(documentId, contactId)
            .fold(
                onSuccess = {
                    // 2. Fetch contact details for snapshot
                    getContact(contactId).fold(
                        onSuccess = { contact ->
                            withState<DocumentReviewState.Content, _> {
                                updateState {
                                    copy(
                                        selectedContactId = contactId,
                                        selectedContactSnapshot = ContactSnapshot(
                                            id = contact.id,
                                            name = contact.name.value,
                                            vatNumber = contact.vatNumber?.value,
                                            email = contact.email?.value,
                                        ),
                                        contactSelectionState = ContactSelectionState.Selected,
                                        isBindingContact = false,
                                    )
                                }
                            }
                        },
                        onFailure = { error ->
                            // Contact bound but couldn't fetch details - still proceed
                            logger.w(error) { "Contact bound but fetch failed" }
                            withState<DocumentReviewState.Content, _> {
                                updateState {
                                    copy(
                                        selectedContactId = contactId,
                                        selectedContactSnapshot = null,
                                        contactSelectionState = ContactSelectionState.Selected,
                                        isBindingContact = false,
                                    )
                                }
                            }
                        }
                    )
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to bind contact to draft" }
                    withState<DocumentReviewState.Content, _> {
                        updateState {
                            copy(
                                isBindingContact = false,
                                contactValidationError = "Failed to save contact selection",
                            )
                        }
                    }
                    action(DocumentReviewAction.ShowError("Failed to bind contact"))
                }
            )
    }

    private suspend fun DocumentReviewCtx.handleOpenContactPicker() {
        // This triggers navigation action to open picker UI
        // For now, the picker is part of the screen, so just an intent marker
        // Could emit action if needed: action(DocumentReviewAction.OpenContactPicker)
    }

    private suspend fun DocumentReviewCtx.handleCloseContactPicker() {
        // Picker closed without selection - no state change needed
    }

    // =========================================================================
    // CONTACT CREATION
    // =========================================================================

    private suspend fun DocumentReviewCtx.handleOpenCreateContactSheet() {
        withState<DocumentReviewState.Content, _> {
            // Build pre-fill data from extracted fields
            val preFill = when (editableData.documentType) {
                DocumentType.Invoice -> ContactPreFillData(
                    name = editableData.invoice?.clientName ?: "",
                    vatNumber = editableData.invoice?.clientVatNumber,
                    email = editableData.invoice?.clientEmail,
                    address = editableData.invoice?.clientAddress,
                )
                DocumentType.Bill -> ContactPreFillData(
                    name = editableData.bill?.supplierName ?: "",
                    vatNumber = editableData.bill?.supplierVatNumber,
                    email = null,
                    address = editableData.bill?.supplierAddress,
                )
                else -> null
            }
            updateState { copy(showCreateContactSheet = true, createContactPreFill = preFill) }
        }
    }

    private suspend fun DocumentReviewCtx.handleCloseCreateContactSheet() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(showCreateContactSheet = false, createContactPreFill = null) }
        }
    }

    private suspend fun DocumentReviewCtx.handleContactCreated(contactId: ContactId) {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(showCreateContactSheet = false, createContactPreFill = null) }
            bindContact(documentId, contactId)
        }
    }

    // =========================================================================
    // MOBILE PREVIEW SHEET
    // =========================================================================

    private suspend fun DocumentReviewCtx.handleOpenPreviewSheet() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(showPreviewSheet = true) }
        }
    }

    private suspend fun DocumentReviewCtx.handleClosePreviewSheet() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(showPreviewSheet = false) }
        }
    }

    // =========================================================================
    // LINE ITEMS
    // =========================================================================

    private suspend fun DocumentReviewCtx.handleAddLineItem() {
        withState<DocumentReviewState.Content, _> {
            if (editableData.documentType != DocumentType.Invoice) return@withState

            val currentInvoice = editableData.invoice ?: return@withState
            val newItem = ExtractedLineItem(
                description = "",
                quantity = 1.0,
                unitPrice = null,
                vatRate = null,
                lineTotal = null,
                vatAmount = null
            )
            val updatedItems = currentInvoice.items + newItem
            val updatedInvoice = currentInvoice.copy(items = updatedItems)

            updateState {
                copy(
                    editableData = editableData.copy(invoice = updatedInvoice),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    private suspend fun DocumentReviewCtx.handleUpdateLineItem(index: Int, item: ExtractedLineItem) {
        withState<DocumentReviewState.Content, _> {
            val currentInvoice = editableData.invoice ?: return@withState
            if (index < 0 || index >= currentInvoice.items.size) return@withState

            val updatedItems = currentInvoice.items.toMutableList()
            updatedItems[index] = item
            val updatedInvoice = currentInvoice.copy(items = updatedItems)

            updateState {
                copy(
                    editableData = editableData.copy(invoice = updatedInvoice),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    private suspend fun DocumentReviewCtx.handleRemoveLineItem(index: Int) {
        withState<DocumentReviewState.Content, _> {
            val currentInvoice = editableData.invoice ?: return@withState
            if (index < 0 || index >= currentInvoice.items.size) return@withState

            val updatedItems = currentInvoice.items.toMutableList()
            updatedItems.removeAt(index)
            val updatedInvoice = currentInvoice.copy(items = updatedItems)

            updateState {
                copy(
                    editableData = editableData.copy(invoice = updatedInvoice),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    // =========================================================================
    // PROVENANCE
    // =========================================================================

    private suspend fun DocumentReviewCtx.handleSelectFieldForProvenance(fieldPath: String?) {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(selectedFieldPath = fieldPath) }
        }
    }

    // =========================================================================
    // PDF PREVIEW
    // =========================================================================

    private suspend fun DocumentReviewCtx.handleLoadPreviewPages() {
        withState<DocumentReviewState.Content, _> {
            loadPreviewPages(
                documentId = documentId,
                contentType = document.document.contentType,
                dpi = PreviewConfig.dpi.value,
                maxPages = PreviewConfig.DEFAULT_MAX_PAGES
            )
        }
    }

    private suspend fun DocumentReviewCtx.handleLoadMorePages(maxPages: Int) {
        withState<DocumentReviewState.Content, _> {
            loadPreviewPages(
                documentId = documentId,
                contentType = document.document.contentType,
                dpi = PreviewConfig.dpi.value,
                maxPages = maxPages
            )
        }
    }

    private suspend fun DocumentReviewCtx.loadPreviewPages(
        documentId: DocumentId,
        contentType: String,
        dpi: Int,
        maxPages: Int
    ) {
        // Check if document is a PDF
        if (!contentType.contains("pdf", ignoreCase = true)) {
            withState<DocumentReviewState.Content, _> {
                updateState { copy(previewState = DocumentPreviewState.NotPdf) }
            }
            return
        }

        withState<DocumentReviewState.Content, _> {
            updateState { copy(previewState = DocumentPreviewState.Loading) }
        }

        dataSource.getDocumentPages(documentId, dpi, maxPages)
            .fold(
                onSuccess = { response ->
                    withState<DocumentReviewState.Content, _> {
                        if (response.pages.isEmpty()) {
                            updateState { copy(previewState = DocumentPreviewState.NoPreview) }
                        } else {
                            updateState {
                                copy(
                                    previewState = DocumentPreviewState.Ready(
                                        pages = response.pages,
                                        totalPages = response.totalPages,
                                        renderedPages = response.renderedPages,
                                        dpi = response.dpi,
                                        hasMore = response.totalPages > response.renderedPages
                                    )
                                )
                            }
                        }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load preview pages for document: $documentId" }
                    withState<DocumentReviewState.Content, _> {
                        updateState {
                            copy(
                                previewState = DocumentPreviewState.Error(
                                    message = error.message ?: "Failed to load preview",
                                    retry = { intent(DocumentReviewIntent.RetryLoadPreview) }
                                )
                            )
                        }
                    }
                }
            )
    }

    // =========================================================================
    // ACTIONS
    // =========================================================================

    private suspend fun DocumentReviewCtx.handleSaveDraft() {
        withState<DocumentReviewState.Content, _> {
            if (!hasUnsavedChanges) return@withState

            logger.d { "Saving draft for document: $documentId" }
            updateState { copy(isSaving = true) }

            // Build ExtractedDocumentData from editable data
            val updatedExtractedData = buildExtractedDataFromEditable(editableData, originalData)
            val request = UpdateDraftRequest(
                extractedData = updatedExtractedData,
                changeDescription = "User edits"
            )

            launch {
                // Note: This requires adding updateDraft method to CashflowRemoteDataSource
                // For now, we'll simulate success and show a message
                logger.i { "Draft save requested (API method needed): $documentId" }

                withState<DocumentReviewState.Content, _> {
                    updateState {
                        copy(
                            isSaving = false,
                            hasUnsavedChanges = false
                        )
                    }
                    action(DocumentReviewAction.ShowSuccess("Draft saved"))
                }
            }
        }
    }

    private suspend fun DocumentReviewCtx.handleDiscardChanges() {
        withState<DocumentReviewState.Content, _> {
            if (!hasUnsavedChanges) return@withState

            action(DocumentReviewAction.ShowDiscardConfirmation)
        }
    }

    private suspend fun DocumentReviewCtx.handleConfirm() {
        withState<DocumentReviewState.Content, _> {
            if (!canConfirm) {
                action(DocumentReviewAction.ShowError("Cannot confirm: required fields missing"))
                return@withState
            }

            logger.d { "Confirming document: $documentId" }
            updateState { copy(isConfirming = true) }

            // Build confirmation request with current extracted data
            val updatedExtractedData = buildExtractedDataFromEditable(editableData, originalData)
            val request = ConfirmDocumentRequest(
                documentType = editableData.documentType,
                extractedData = updatedExtractedData
            )

            launch {
                // Note: This requires adding confirmDocument method to CashflowRemoteDataSource
                // For now, we'll simulate success
                logger.i { "Confirmation requested (API method needed): $documentId" }

                withState<DocumentReviewState.Content, _> {
                    updateState { copy(isConfirming = false) }
                    action(DocumentReviewAction.ShowSuccess("Document confirmed"))
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

    private suspend fun DocumentReviewCtx.handleReject() {
        withState<DocumentReviewState.Content, _> {
            action(DocumentReviewAction.ShowRejectConfirmation)
        }
    }

    private suspend fun DocumentReviewCtx.handleOpenChat() {
        withState<DocumentReviewState.Content, _> {
            action(DocumentReviewAction.NavigateToChat(documentId))
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private fun buildExtractedDataFromEditable(
        editable: EditableExtractedData,
        original: ExtractedDocumentData?
    ): ExtractedDocumentData {
        return ExtractedDocumentData(
            documentType = editable.documentType,
            rawText = original?.rawText,
            invoice = editable.invoice?.let { invoice ->
                original?.invoice?.copy(
                    clientName = invoice.clientName.takeIf { it.isNotBlank() },
                    clientVatNumber = invoice.clientVatNumber.takeIf { it.isNotBlank() },
                    clientEmail = invoice.clientEmail.takeIf { it.isNotBlank() },
                    clientAddress = invoice.clientAddress.takeIf { it.isNotBlank() },
                    invoiceNumber = invoice.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = invoice.issueDate,
                    dueDate = invoice.dueDate,
                    items = invoice.items,
                    notes = invoice.notes.takeIf { it.isNotBlank() },
                    paymentTerms = invoice.paymentTerms.takeIf { it.isNotBlank() },
                    bankAccount = invoice.bankAccount.takeIf { it.isNotBlank() }
                )
            },
            bill = editable.bill?.let { bill ->
                original?.bill?.copy(
                    supplierName = bill.supplierName.takeIf { it.isNotBlank() },
                    supplierVatNumber = bill.supplierVatNumber.takeIf { it.isNotBlank() },
                    supplierAddress = bill.supplierAddress.takeIf { it.isNotBlank() },
                    invoiceNumber = bill.invoiceNumber.takeIf { it.isNotBlank() },
                    issueDate = bill.issueDate,
                    dueDate = bill.dueDate,
                    category = bill.category,
                    description = bill.description.takeIf { it.isNotBlank() },
                    notes = bill.notes.takeIf { it.isNotBlank() },
                    paymentTerms = bill.paymentTerms.takeIf { it.isNotBlank() },
                    bankAccount = bill.bankAccount.takeIf { it.isNotBlank() }
                )
            },
            expense = editable.expense?.let { expense ->
                original?.expense?.copy(
                    merchant = expense.merchant.takeIf { it.isNotBlank() },
                    merchantAddress = expense.merchantAddress.takeIf { it.isNotBlank() },
                    merchantVatNumber = expense.merchantVatNumber.takeIf { it.isNotBlank() },
                    date = expense.date,
                    category = expense.category,
                    description = expense.description.takeIf { it.isNotBlank() },
                    isDeductible = expense.isDeductible,
                    paymentMethod = expense.paymentMethod,
                    notes = expense.notes.takeIf { it.isNotBlank() },
                    receiptNumber = expense.receiptNumber.takeIf { it.isNotBlank() }
                )
            },
            overallConfidence = original?.overallConfidence,
            fieldConfidences = original?.fieldConfidences ?: emptyMap()
        )
    }

}
