package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.Percentage
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.DocumentType
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.PaymentMethod
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

// ============================================================================
// EXTRACTED DATA MODELS
// These mirror the create request fields for easy form pre-filling after AI extraction
// ============================================================================

/**
 * Container for all extracted document data.
 * Contains the detected type, raw text, and type-specific extracted fields.
 */
@Serializable
data class ExtractedDocumentData(
    /** Detected document type */
    val documentType: DocumentType = DocumentType.Unknown,

    /** Full OCR/extracted text from document - stored for future AI use */
    val rawText: String? = null,

    /** Extracted invoice fields (if documentType == Invoice) */
    val invoice: ExtractedInvoiceFields? = null,

    /** Extracted bill fields (if documentType == Bill) */
    val bill: ExtractedBillFields? = null,

    /** Extracted expense/receipt fields (if documentType == Expense) */
    val expense: ExtractedExpenseFields? = null,

    /** Overall extraction confidence (0.0 - 1.0) */
    val overallConfidence: Double? = null,

    /** Per-field confidence scores (field name -> confidence) */
    val fieldConfidences: Map<String, Double> = emptyMap()
)

/**
 * Extracted invoice fields - mirrors CreateInvoiceRequest structure.
 * All fields are nullable since AI may not extract everything.
 */
@Serializable
data class ExtractedInvoiceFields(
    // Client information (needs to be matched to existing client)
    val clientName: String? = null,
    val clientVatNumber: String? = null,
    val clientEmail: String? = null,
    val clientAddress: String? = null,

    // Invoice identification
    val invoiceNumber: String? = null,

    // Dates
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,

    // Line items
    val items: List<ExtractedLineItem>? = null,

    // Totals (can be extracted or calculated from items)
    val subtotalAmount: Money? = null,
    val vatAmount: Money? = null,
    val totalAmount: Money? = null,

    // Currency
    val currency: Currency? = null,

    // Additional info
    val notes: String? = null,
    val paymentTerms: String? = null,
    val bankAccount: String? = null
)

/**
 * Extracted bill fields - mirrors CreateBillRequest structure.
 */
@Serializable
data class ExtractedBillFields(
    // Supplier information
    val supplierName: String? = null,
    val supplierVatNumber: String? = null,
    val supplierAddress: String? = null,

    // Invoice identification
    val invoiceNumber: String? = null,

    // Dates
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,

    // Amounts
    val amount: Money? = null,
    val vatAmount: Money? = null,
    val vatRate: VatRate? = null,

    // Currency
    val currency: Currency? = null,

    // Categorization
    val category: ExpenseCategory? = null,
    val description: String? = null,

    // Additional info
    val notes: String? = null,
    val paymentTerms: String? = null,
    val bankAccount: String? = null
)

/**
 * Extracted expense/receipt fields - mirrors CreateExpenseRequest structure.
 */
@Serializable
data class ExtractedExpenseFields(
    // Merchant/vendor information
    val merchant: String? = null,
    val merchantAddress: String? = null,
    val merchantVatNumber: String? = null,

    // Date and amount
    val date: LocalDate? = null,
    val amount: Money? = null,
    val vatAmount: Money? = null,
    val vatRate: VatRate? = null,

    // Currency
    val currency: Currency? = null,

    // Categorization
    val category: ExpenseCategory? = null,
    val description: String? = null,

    // Deductibility
    val isDeductible: Boolean? = null,
    val deductiblePercentage: Percentage? = null,

    // Payment info
    val paymentMethod: PaymentMethod? = null,

    // Additional info
    val notes: String? = null,
    val receiptNumber: String? = null
)

/**
 * Extracted line item for invoices/bills.
 */
@Serializable
data class ExtractedLineItem(
    val description: String? = null,
    val quantity: Double? = null,
    val unitPrice: Money? = null,
    val vatRate: VatRate? = null,
    val lineTotal: Money? = null,
    val vatAmount: Money? = null
)

// ============================================================================
// CONFIRMATION MODELS
// Used when user confirms extracted data to create financial entity
// ============================================================================

/**
 * Request to confirm extracted document and create entity.
 */
@Serializable
data class ConfirmDocumentRequest(
    /** Entity type to create (invoice, bill, expense) */
    val entityType: DocumentType,

    /** Corrections/overrides to apply to extracted data */
    val corrections: DocumentCorrections? = null
)

/**
 * Corrections that user can make to extracted data before confirmation.
 * All fields are nullable - only non-null fields override extracted values.
 */
@Serializable
data class DocumentCorrections(
    // Common fields
    val date: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val amount: Money? = null,
    val vatAmount: Money? = null,
    val vatRate: VatRate? = null,
    val category: ExpenseCategory? = null,
    val description: String? = null,
    val notes: String? = null,

    // Invoice-specific
    val contactId: String? = null,  // UUID string of existing contact (customer)
    val invoiceNumber: String? = null,
    val items: List<ExtractedLineItem>? = null,

    // Bill-specific
    val supplierName: String? = null,
    val supplierVatNumber: String? = null,

    // Expense-specific
    val merchant: String? = null,
    val isDeductible: Boolean? = null,
    val deductiblePercentage: Percentage? = null,
    val paymentMethod: PaymentMethod? = null
)

/**
 * Response after confirming a document.
 */
@Serializable
data class ConfirmDocumentResponse(
    /** The created entity ID */
    val entityId: String,

    /** Entity type that was created */
    val entityType: DocumentType,

    /** The processing record ID */
    val processingId: String
)

// ============================================================================
// DRAFT UPDATE MODELS
// Used when user edits extracted data before confirmation
// ============================================================================

/**
 * Request to update the extracted draft with user corrections.
 * This preserves the original AI draft for audit purposes.
 */
@Serializable
data class UpdateDraftRequest(
    /** Updated extracted data (merged with user corrections) */
    val extractedData: ExtractedDocumentData,

    /** Optional description of what was changed */
    val changeDescription: String? = null
)

/**
 * Response after updating draft.
 */
@Serializable
data class UpdateDraftResponse(
    /** The processing record ID */
    val processingId: String,

    /** Current draft version after update */
    val draftVersion: Int,

    /** The updated extracted data */
    val extractedData: ExtractedDocumentData,

    /** Timestamp of the update */
    val updatedAt: String
)

/**
 * Tracked user correction for audit trail.
 * Records what field was changed, from what AI value to what user value.
 */
@Serializable
data class TrackedCorrection(
    /** Field path that was changed (e.g., "invoice.clientName") */
    val field: String,

    /** Original AI-extracted value */
    val aiValue: String?,

    /** User-provided value */
    val userValue: String?,

    /** When this correction was made */
    val editedAt: String
)
