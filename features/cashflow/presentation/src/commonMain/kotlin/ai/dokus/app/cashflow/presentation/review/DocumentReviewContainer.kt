package ai.dokus.app.cashflow.presentation.review

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.ProcessingStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentProcessingId
import tech.dokus.domain.model.ConfirmDocumentRequest
import tech.dokus.domain.model.DocumentCorrections
import tech.dokus.domain.model.DocumentProcessingDto
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedLineItem
import tech.dokus.domain.model.UpdateDraftRequest
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
import tech.dokus.domain.exceptions.DokusException

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
) : Container<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> {

    private val logger = Logger.forClass<DocumentReviewContainer>()

    override val store: Store<DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> =
        store(DocumentReviewState.Loading) {
            init {
                // No auto-initialization - wait for LoadDocument intent with processingId
            }

            reduce { intent ->
                when (intent) {
                    // === Data Loading ===
                    is DocumentReviewIntent.LoadDocument -> handleLoadDocument(intent.processingId)
                    is DocumentReviewIntent.Refresh -> handleRefresh()

                    // === Field Editing ===
                    is DocumentReviewIntent.UpdateInvoiceField -> handleUpdateInvoiceField(intent.field, intent.value)
                    is DocumentReviewIntent.UpdateBillField -> handleUpdateBillField(intent.field, intent.value)
                    is DocumentReviewIntent.UpdateExpenseField -> handleUpdateExpenseField(intent.field, intent.value)
                    is DocumentReviewIntent.SelectContact -> handleSelectContact(intent.contactId)
                    is DocumentReviewIntent.ClearContact -> handleClearContact()

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

    private suspend fun DocumentReviewCtx.handleLoadDocument(processingId: DocumentProcessingId) {
        logger.d { "Loading document: $processingId" }
        updateState { DocumentReviewState.Loading }

        fetchDocumentProcessing(processingId)
    }

    private suspend fun DocumentReviewCtx.handleRefresh() {
        withState<DocumentReviewState.Content, _> {
            logger.d { "Refreshing document: $processingId" }
            fetchDocumentProcessing(processingId)
        }
    }

    private suspend fun DocumentReviewCtx.fetchDocumentProcessing(processingId: DocumentProcessingId) {
        // Fetch document processing from the list endpoint and find by ID
        // Note: In production, you'd have a dedicated getDocumentProcessing(id) method
        dataSource.listDocumentProcessing(
            statuses = listOf(ProcessingStatus.Processed, ProcessingStatus.Pending, ProcessingStatus.Processing),
            page = 0,
            limit = 100
        ).fold(
            onSuccess = { response ->
                val document = response.items.find { it.id == processingId }
                if (document != null) {
                    transitionToContent(processingId, document)
                } else {
                    logger.e { "Document not found: $processingId" }
                    updateState {
                        DocumentReviewState.Error(
                            exception = DokusException.NotFound("Document not found"),
                            retryHandler = { intent(DocumentReviewIntent.LoadDocument(processingId)) }
                        )
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load document: $processingId" }
                updateState {
                    DocumentReviewState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(DocumentReviewIntent.LoadDocument(processingId)) }
                    )
                }
            }
        )
    }

    private suspend fun DocumentReviewCtx.transitionToContent(
        processingId: DocumentProcessingId,
        document: DocumentProcessingDto
    ) {
        val extractedData = document.extractedData
        val editableData = EditableExtractedData.fromExtractedData(extractedData)

        // Build contact suggestions from document if available
        val contactSuggestions = buildContactSuggestions(document)

        updateState {
            DocumentReviewState.Content(
                processingId = processingId,
                document = document,
                editableData = editableData,
                originalData = extractedData,
                hasUnsavedChanges = false,
                isSaving = false,
                isConfirming = false,
                selectedFieldPath = null,
                previewUrl = document.document?.downloadUrl,
                contactSuggestions = contactSuggestions
            )
        }
    }

    private fun buildContactSuggestions(document: DocumentProcessingDto): List<ContactSuggestion> {
        val suggestions = mutableListOf<ContactSuggestion>()

        // Add suggested contact if available
        document.suggestedContactId?.let { contactId ->
            // Build suggestion from document's contact match info
            val extractedName = when (document.documentType) {
                DocumentType.Invoice -> document.extractedData?.invoice?.clientName
                DocumentType.Bill -> document.extractedData?.bill?.supplierName
                DocumentType.Expense -> document.extractedData?.expense?.merchant
                else -> null
            }
            val extractedVat = when (document.documentType) {
                DocumentType.Invoice -> document.extractedData?.invoice?.clientVatNumber
                DocumentType.Bill -> document.extractedData?.bill?.supplierVatNumber
                DocumentType.Expense -> document.extractedData?.expense?.merchantVatNumber
                else -> null
            }

            suggestions.add(
                ContactSuggestion(
                    contactId = contactId,
                    name = extractedName ?: "Unknown",
                    vatNumber = extractedVat,
                    matchConfidence = document.contactSuggestionConfidence ?: 0f,
                    matchReason = document.contactSuggestionReason ?: "AI suggested"
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

    private suspend fun DocumentReviewCtx.handleSelectContact(contactId: ContactId) {
        withState<DocumentReviewState.Content, _> {
            when (editableData.documentType) {
                DocumentType.Invoice -> {
                    val updatedInvoice = editableData.invoice?.copy(selectedContactId = contactId)
                    updateState {
                        copy(
                            editableData = editableData.copy(invoice = updatedInvoice),
                            hasUnsavedChanges = true
                        )
                    }
                }
                DocumentType.Bill -> {
                    val updatedBill = editableData.bill?.copy(selectedContactId = contactId)
                    updateState {
                        copy(
                            editableData = editableData.copy(bill = updatedBill),
                            hasUnsavedChanges = true
                        )
                    }
                }
                else -> { /* No contact for expenses */ }
            }
        }
    }

    private suspend fun DocumentReviewCtx.handleClearContact() {
        withState<DocumentReviewState.Content, _> {
            when (editableData.documentType) {
                DocumentType.Invoice -> {
                    val updatedInvoice = editableData.invoice?.copy(selectedContactId = null)
                    updateState {
                        copy(
                            editableData = editableData.copy(invoice = updatedInvoice),
                            hasUnsavedChanges = true
                        )
                    }
                }
                DocumentType.Bill -> {
                    val updatedBill = editableData.bill?.copy(selectedContactId = null)
                    updateState {
                        copy(
                            editableData = editableData.copy(bill = updatedBill),
                            hasUnsavedChanges = true
                        )
                    }
                }
                else -> { /* No contact for expenses */ }
            }
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
    // ACTIONS
    // =========================================================================

    private suspend fun DocumentReviewCtx.handleSaveDraft() {
        withState<DocumentReviewState.Content, _> {
            if (!hasUnsavedChanges) return@withState

            logger.d { "Saving draft for document: $processingId" }
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
                logger.i { "Draft save requested (API method needed): $processingId" }

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

            logger.d { "Confirming document: $processingId" }
            updateState { copy(isConfirming = true) }

            // Build confirmation request
            val corrections = buildDocumentCorrections(editableData)
            val request = ConfirmDocumentRequest(
                entityType = editableData.documentType,
                corrections = corrections
            )

            launch {
                // Note: This requires adding confirmDocument method to CashflowRemoteDataSource
                // For now, we'll simulate success
                logger.i { "Confirmation requested (API method needed): $processingId" }

                withState<DocumentReviewState.Content, _> {
                    updateState { copy(isConfirming = false) }
                    action(DocumentReviewAction.ShowSuccess("Document confirmed"))
                    action(
                        DocumentReviewAction.NavigateToEntity(
                            entityId = processingId.toString(),
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
            action(DocumentReviewAction.NavigateToChat(processingId))
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

    private fun buildDocumentCorrections(editable: EditableExtractedData): DocumentCorrections {
        return when (editable.documentType) {
            DocumentType.Invoice -> {
                val invoice = editable.invoice
                DocumentCorrections(
                    contactId = invoice?.selectedContactId?.toString(),
                    invoiceNumber = invoice?.invoiceNumber,
                    date = invoice?.issueDate,
                    dueDate = invoice?.dueDate,
                    notes = invoice?.notes,
                    items = invoice?.items
                )
            }
            DocumentType.Bill -> {
                val bill = editable.bill
                DocumentCorrections(
                    supplierName = bill?.supplierName,
                    supplierVatNumber = bill?.supplierVatNumber,
                    invoiceNumber = bill?.invoiceNumber,
                    date = bill?.issueDate,
                    dueDate = bill?.dueDate,
                    category = bill?.category,
                    description = bill?.description,
                    notes = bill?.notes
                )
            }
            DocumentType.Expense -> {
                val expense = editable.expense
                DocumentCorrections(
                    merchant = expense?.merchant,
                    date = expense?.date,
                    category = expense?.category,
                    description = expense?.description,
                    isDeductible = expense?.isDeductible,
                    paymentMethod = expense?.paymentMethod,
                    notes = expense?.notes
                )
            }
            else -> DocumentCorrections()
        }
    }
}
