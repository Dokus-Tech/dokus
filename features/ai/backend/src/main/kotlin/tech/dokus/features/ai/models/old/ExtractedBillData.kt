package tech.dokus.features.ai.models.old

import kotlinx.serialization.Serializable

// =============================================================================
// Provenance Models for Bill Extraction
// =============================================================================

/**
 * Provenance data for all extracted bill fields.
 * Maps field names to their provenance information.
 */
@Serializable
data class BillProvenance(
    // Supplier fields provenance
    val supplierName: FieldProvenance? = null,
    val supplierVatNumber: FieldProvenance? = null,
    val supplierAddress: FieldProvenance? = null,

    // Bill details provenance
    val invoiceNumber: FieldProvenance? = null,
    val issueDate: FieldProvenance? = null,
    val dueDate: FieldProvenance? = null,

    // Amount provenance
    val amount: FieldProvenance? = null,
    val vatAmount: FieldProvenance? = null,
    val vatRate: FieldProvenance? = null,
    val currency: FieldProvenance? = null,

    // Category provenance
    val category: FieldProvenance? = null,
    val description: FieldProvenance? = null,

    // Payment info provenance
    val paymentTerms: FieldProvenance? = null,
    val bankAccount: FieldProvenance? = null
)

// =============================================================================
// Extracted Bill Data with Provenance
// =============================================================================

/**
 * Extracted data from a bill (supplier invoice) document.
 *
 * Bills are incoming invoices from suppliers, as opposed to
 * outgoing invoices to clients.
 */
@Serializable
data class ExtractedBillData(
    // Supplier information
    val supplierName: String? = null,
    val supplierVatNumber: String? = null,
    val supplierAddress: String? = null,

    // Bill identification
    val invoiceNumber: String? = null,
    val issueDate: String? = null, // ISO format YYYY-MM-DD
    val dueDate: String? = null,

    // Amounts
    val currency: String? = null, // EUR, USD, etc.
    val amount: String? = null, // Total amount including VAT (gross)
    val vatAmount: String? = null,
    val vatRate: String? = null, // e.g., "21%"
    val totalAmount: String? = null, // Optional explicit total line (may match amount)

    // Line items (optional, some bills don't have itemized breakdown)
    val lineItems: List<BillLineItem> = emptyList(),

    // Categorization
    val category: String? = null, // Expense category suggestion
    val description: String? = null,

    // Payment information
    val paymentTerms: String? = null, // e.g., "Net 30"
    val bankAccount: String? = null, // IBAN or account number

    // Additional notes
    val notes: String? = null,

    // Metadata
    val confidence: Double = 0.0,

    // Text transcription for RAG indexing (extracted by vision model)
    val extractedText: String? = null,

    // Provenance - links extracted values to source locations
    val provenance: BillProvenance? = null
)

/**
 * A line item on a bill.
 */
@Serializable
data class BillLineItem(
    val description: String,
    val quantity: Double? = null,
    val unitPrice: String? = null,
    val vatRate: String? = null, // e.g., "21%"
    val total: String? = null
)
