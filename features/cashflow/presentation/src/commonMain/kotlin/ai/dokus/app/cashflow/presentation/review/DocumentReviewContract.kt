package ai.dokus.app.cashflow.presentation.review

import ai.dokus.foundation.domain.asbtractions.RetryHandler
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.domain.enums.ProcessingStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentProcessingId
import tech.dokus.domain.model.DocumentProcessingDto
import tech.dokus.domain.model.ExtractedBillFields
import tech.dokus.domain.model.ExtractedDocumentData
import tech.dokus.domain.model.ExtractedExpenseFields
import tech.dokus.domain.model.ExtractedInvoiceFields
import tech.dokus.domain.model.ExtractedLineItem
import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for the Document Review screen.
 *
 * The Document Review screen allows users to:
 * - View AI-extracted document data
 * - Edit and correct extracted fields
 * - See confidence indicators and provenance info
 * - Confirm (create entity) or reject the document
 * - Navigate to chat for document Q&A
 *
 * Flow:
 * 1. Loading -> Fetch document processing record
 * 2. Content -> Display extracted data for review/editing
 * 3. Saving -> Saving changes or confirming
 * 4. Error -> Failed to load with retry option
 *
 * Audit Trail:
 * - Original AI draft is preserved when user makes first edit
 * - Each correction is tracked with timestamp
 * - Draft version increments on each save
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface DocumentReviewState : MVIState, DokusState<Nothing> {

    /**
     * Loading state - fetching document processing record.
     */
    data object Loading : DocumentReviewState

    /**
     * Content state - document loaded and ready for review.
     *
     * @property processingId The document processing record ID
     * @property document Full document processing record
     * @property editableData Current editable state of extracted data
     * @property originalData Original AI-extracted data (for comparison)
     * @property hasUnsavedChanges Whether user has made unsaved edits
     * @property isSaving Whether a save operation is in progress
     * @property isConfirming Whether confirmation is in progress
     * @property selectedFieldPath Currently selected field for provenance highlight
     * @property previewUrl URL to preview the original document
     * @property contactSuggestions Suggested contacts based on extraction
     */
    data class Content(
        val processingId: DocumentProcessingId,
        val document: DocumentProcessingDto,
        val editableData: EditableExtractedData,
        val originalData: ExtractedDocumentData?,
        val hasUnsavedChanges: Boolean = false,
        val isSaving: Boolean = false,
        val isConfirming: Boolean = false,
        val selectedFieldPath: String? = null,
        val previewUrl: String? = null,
        val contactSuggestions: List<ContactSuggestion> = emptyList(),
    ) : DocumentReviewState {

        /**
         * Whether the document can be confirmed.
         * Must be in PROCESSED status and have valid required fields.
         */
        val canConfirm: Boolean
            get() = document.status == ProcessingStatus.Processed &&
                    !isConfirming &&
                    !isSaving &&
                    editableData.isValid

        /**
         * Whether the document shows AI confidence indicators.
         */
        val showConfidence: Boolean
            get() {
                val conf = document.confidence
                return conf != null && conf > 0.0
            }

        /**
         * Overall confidence percentage for display.
         */
        val confidencePercent: Int
            get() = ((document.confidence ?: 0.0) * 100).toInt()
    }

    /**
     * Error state - failed to load document.
     *
     * @property exception The error that occurred
     * @property retryHandler Handler to retry the failed operation
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : DocumentReviewState, DokusState.Error<Nothing>
}

// ============================================================================
// EDITABLE DATA MODELS
// ============================================================================

/**
 * Unified editable data model for all document types.
 * Contains nullable fields that are populated based on document type.
 */
@Immutable
data class EditableExtractedData(
    val documentType: DocumentType,

    // Invoice fields
    val invoice: EditableInvoiceFields? = null,

    // Bill fields
    val bill: EditableBillFields? = null,

    // Expense fields
    val expense: EditableExpenseFields? = null,
) {
    /**
     * Whether the current data has all required fields for confirmation.
     */
    val isValid: Boolean
        get() = when (documentType) {
            DocumentType.Invoice -> invoice?.isValid == true
            DocumentType.Bill -> bill?.isValid == true
            DocumentType.Expense -> expense?.isValid == true
            else -> false
        }

    companion object {
        /**
         * Create editable data from extracted document data.
         */
        fun fromExtractedData(data: ExtractedDocumentData?): EditableExtractedData {
            val type = data?.documentType ?: DocumentType.Unknown
            return EditableExtractedData(
                documentType = type,
                invoice = data?.invoice?.let { EditableInvoiceFields.fromExtracted(it) },
                bill = data?.bill?.let { EditableBillFields.fromExtracted(it) },
                expense = data?.expense?.let { EditableExpenseFields.fromExtracted(it) },
            )
        }
    }
}

/**
 * Editable invoice fields with validation.
 */
@Immutable
data class EditableInvoiceFields(
    val clientName: String = "",
    val clientVatNumber: String = "",
    val clientEmail: String = "",
    val clientAddress: String = "",
    val selectedContactId: ContactId? = null,
    val invoiceNumber: String = "",
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val items: List<ExtractedLineItem> = emptyList(),
    val subtotalAmount: String = "",
    val vatAmount: String = "",
    val totalAmount: String = "",
    val currency: String = "EUR",
    val notes: String = "",
    val paymentTerms: String = "",
    val bankAccount: String = "",
) {
    val isValid: Boolean
        get() = invoiceNumber.isNotBlank() &&
                issueDate != null &&
                totalAmount.isNotBlank()

    companion object {
        fun fromExtracted(data: ExtractedInvoiceFields): EditableInvoiceFields {
            return EditableInvoiceFields(
                clientName = data.clientName ?: "",
                clientVatNumber = data.clientVatNumber ?: "",
                clientEmail = data.clientEmail ?: "",
                clientAddress = data.clientAddress ?: "",
                invoiceNumber = data.invoiceNumber ?: "",
                issueDate = data.issueDate,
                dueDate = data.dueDate,
                items = data.items ?: emptyList(),
                subtotalAmount = data.subtotalAmount?.value ?: "",
                vatAmount = data.vatAmount?.value ?: "",
                totalAmount = data.totalAmount?.value ?: "",
                currency = data.currency?.name ?: "EUR",
                notes = data.notes ?: "",
                paymentTerms = data.paymentTerms ?: "",
                bankAccount = data.bankAccount ?: "",
            )
        }
    }
}

/**
 * Editable bill fields with validation.
 */
@Immutable
data class EditableBillFields(
    val supplierName: String = "",
    val supplierVatNumber: String = "",
    val supplierAddress: String = "",
    val selectedContactId: ContactId? = null,
    val invoiceNumber: String = "",
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val amount: String = "",
    val vatAmount: String = "",
    val vatRate: String = "",
    val currency: String = "EUR",
    val category: ExpenseCategory? = null,
    val description: String = "",
    val notes: String = "",
    val paymentTerms: String = "",
    val bankAccount: String = "",
) {
    val isValid: Boolean
        get() = supplierName.isNotBlank() &&
                issueDate != null &&
                amount.isNotBlank()

    companion object {
        fun fromExtracted(data: ExtractedBillFields): EditableBillFields {
            return EditableBillFields(
                supplierName = data.supplierName ?: "",
                supplierVatNumber = data.supplierVatNumber ?: "",
                supplierAddress = data.supplierAddress ?: "",
                invoiceNumber = data.invoiceNumber ?: "",
                issueDate = data.issueDate,
                dueDate = data.dueDate,
                amount = data.amount?.value ?: "",
                vatAmount = data.vatAmount?.value ?: "",
                vatRate = data.vatRate?.value ?: "",
                currency = data.currency?.name ?: "EUR",
                category = data.category,
                description = data.description ?: "",
                notes = data.notes ?: "",
                paymentTerms = data.paymentTerms ?: "",
                bankAccount = data.bankAccount ?: "",
            )
        }
    }
}

/**
 * Editable expense fields with validation.
 */
@Immutable
data class EditableExpenseFields(
    val merchant: String = "",
    val merchantAddress: String = "",
    val merchantVatNumber: String = "",
    val date: LocalDate? = null,
    val amount: String = "",
    val vatAmount: String = "",
    val vatRate: String = "",
    val currency: String = "EUR",
    val category: ExpenseCategory? = null,
    val description: String = "",
    val isDeductible: Boolean = true,
    val deductiblePercentage: String = "100",
    val paymentMethod: PaymentMethod? = null,
    val notes: String = "",
    val receiptNumber: String = "",
) {
    val isValid: Boolean
        get() = merchant.isNotBlank() &&
                date != null &&
                amount.isNotBlank()

    companion object {
        fun fromExtracted(data: ExtractedExpenseFields): EditableExpenseFields {
            return EditableExpenseFields(
                merchant = data.merchant ?: "",
                merchantAddress = data.merchantAddress ?: "",
                merchantVatNumber = data.merchantVatNumber ?: "",
                date = data.date,
                amount = data.amount?.value ?: "",
                vatAmount = data.vatAmount?.value ?: "",
                vatRate = data.vatRate?.value ?: "",
                currency = data.currency?.name ?: "EUR",
                category = data.category,
                description = data.description ?: "",
                isDeductible = data.isDeductible ?: true,
                deductiblePercentage = data.deductiblePercentage?.value ?: "100",
                paymentMethod = data.paymentMethod,
                notes = data.notes ?: "",
                receiptNumber = data.receiptNumber ?: "",
            )
        }
    }
}

/**
 * Contact suggestion for auto-matching.
 */
@Immutable
data class ContactSuggestion(
    val contactId: ContactId,
    val name: String,
    val vatNumber: String?,
    val matchConfidence: Float,
    val matchReason: String,
)

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface DocumentReviewIntent : MVIIntent {

    // === Data Loading ===

    /** Load the document processing record */
    data class LoadDocument(val processingId: DocumentProcessingId) : DocumentReviewIntent

    /** Refresh the document data */
    data object Refresh : DocumentReviewIntent

    // === Field Editing ===

    /** Update an invoice field */
    data class UpdateInvoiceField(
        val field: InvoiceField,
        val value: Any?,
    ) : DocumentReviewIntent

    /** Update a bill field */
    data class UpdateBillField(
        val field: BillField,
        val value: Any?,
    ) : DocumentReviewIntent

    /** Update an expense field */
    data class UpdateExpenseField(
        val field: ExpenseField,
        val value: Any?,
    ) : DocumentReviewIntent

    /** Select a contact from suggestions */
    data class SelectContact(val contactId: ContactId) : DocumentReviewIntent

    /** Clear selected contact */
    data object ClearContact : DocumentReviewIntent

    // === Line Items (Invoice) ===

    /** Add a new line item */
    data object AddLineItem : DocumentReviewIntent

    /** Update a line item */
    data class UpdateLineItem(val index: Int, val item: ExtractedLineItem) : DocumentReviewIntent

    /** Remove a line item */
    data class RemoveLineItem(val index: Int) : DocumentReviewIntent

    // === Provenance ===

    /** Select a field to highlight its source in the document preview */
    data class SelectFieldForProvenance(val fieldPath: String?) : DocumentReviewIntent

    // === Actions ===

    /** Save current changes as draft */
    data object SaveDraft : DocumentReviewIntent

    /** Discard unsaved changes */
    data object DiscardChanges : DocumentReviewIntent

    /** Confirm the document and create entity */
    data object Confirm : DocumentReviewIntent

    /** Reject the document */
    data object Reject : DocumentReviewIntent

    /** Navigate to chat with this document */
    data object OpenChat : DocumentReviewIntent
}

/**
 * Invoice field identifiers for updates.
 */
enum class InvoiceField {
    CLIENT_NAME,
    CLIENT_VAT_NUMBER,
    CLIENT_EMAIL,
    CLIENT_ADDRESS,
    INVOICE_NUMBER,
    ISSUE_DATE,
    DUE_DATE,
    SUBTOTAL_AMOUNT,
    VAT_AMOUNT,
    TOTAL_AMOUNT,
    CURRENCY,
    NOTES,
    PAYMENT_TERMS,
    BANK_ACCOUNT,
}

/**
 * Bill field identifiers for updates.
 */
enum class BillField {
    SUPPLIER_NAME,
    SUPPLIER_VAT_NUMBER,
    SUPPLIER_ADDRESS,
    INVOICE_NUMBER,
    ISSUE_DATE,
    DUE_DATE,
    AMOUNT,
    VAT_AMOUNT,
    VAT_RATE,
    CURRENCY,
    CATEGORY,
    DESCRIPTION,
    NOTES,
    PAYMENT_TERMS,
    BANK_ACCOUNT,
}

/**
 * Expense field identifiers for updates.
 */
enum class ExpenseField {
    MERCHANT,
    MERCHANT_ADDRESS,
    MERCHANT_VAT_NUMBER,
    DATE,
    AMOUNT,
    VAT_AMOUNT,
    VAT_RATE,
    CURRENCY,
    CATEGORY,
    DESCRIPTION,
    IS_DEDUCTIBLE,
    DEDUCTIBLE_PERCENTAGE,
    PAYMENT_METHOD,
    NOTES,
    RECEIPT_NUMBER,
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface DocumentReviewAction : MVIAction {

    // === Navigation ===

    /** Navigate back to previous screen */
    data object NavigateBack : DocumentReviewAction

    /** Navigate to document chat screen */
    data class NavigateToChat(val processingId: DocumentProcessingId) : DocumentReviewAction

    /** Navigate to created entity after confirmation */
    data class NavigateToEntity(val entityId: String, val entityType: DocumentType) : DocumentReviewAction

    // === Feedback ===

    /** Show error message */
    data class ShowError(val message: String) : DocumentReviewAction

    /** Show success message */
    data class ShowSuccess(val message: String) : DocumentReviewAction

    /** Show confirmation dialog before discard */
    data object ShowDiscardConfirmation : DocumentReviewAction

    /** Show confirmation dialog before reject */
    data object ShowRejectConfirmation : DocumentReviewAction
}
