package tech.dokus.features.ai.models

import kotlinx.serialization.Serializable

// =============================================================================
// Provenance Models - Track where extracted values came from in the document
// =============================================================================

/**
 * Provenance information for a single extracted field.
 * Links the extracted value back to its source location in the document.
 *
 * This enables:
 * - Highlighting source text in document preview
 * - Confidence verification by users
 * - Audit trail for extracted data
 * - Training data collection for future improvements
 */
@Serializable
data class FieldProvenance(
    /** The exact text snippet from the document that this value was extracted from */
    val sourceText: String? = null,

    /** Page number where the value was found (1-indexed for PDFs) */
    val pageNumber: Int? = null,

    /** Bounding box coordinates [x1, y1, x2, y2] if available from OCR */
    val boundingBox: List<Float>? = null,

    /** Character offset where the source text starts in the raw OCR text */
    val startOffset: Int? = null,

    /** Character offset where the source text ends in the raw OCR text */
    val endOffset: Int? = null,

    /** Confidence score for this specific field extraction (0.0 - 1.0) */
    val fieldConfidence: Double? = null,

    /** Explanation of how the value was extracted or inferred */
    val extractionNotes: String? = null
)

/**
 * Provenance data for all extracted invoice fields.
 * Maps field names to their provenance information.
 */
@Serializable
data class InvoiceProvenance(
    // Vendor fields provenance
    val vendorName: FieldProvenance? = null,
    val vendorVatNumber: FieldProvenance? = null,
    val vendorAddress: FieldProvenance? = null,

    // Invoice details provenance
    val invoiceNumber: FieldProvenance? = null,
    val issueDate: FieldProvenance? = null,
    val dueDate: FieldProvenance? = null,
    val paymentTerms: FieldProvenance? = null,

    // Totals provenance
    val currency: FieldProvenance? = null,
    val subtotal: FieldProvenance? = null,
    val totalVatAmount: FieldProvenance? = null,
    val totalAmount: FieldProvenance? = null,

    // Payment info provenance
    val iban: FieldProvenance? = null,
    val bic: FieldProvenance? = null,
    val paymentReference: FieldProvenance? = null
)

// =============================================================================
// Extracted Invoice Data with Provenance
// =============================================================================

/**
 * Extracted data from an invoice document.
 */
@Serializable
data class ExtractedInvoiceData(
    // Vendor information
    val vendorName: String? = null,
    val vendorVatNumber: String? = null,
    val vendorAddress: String? = null,

    // Invoice details
    val invoiceNumber: String? = null,
    val issueDate: String? = null,       // ISO format YYYY-MM-DD
    val dueDate: String? = null,
    val paymentTerms: String? = null,    // e.g., "Net 30"

    // Line items
    val lineItems: List<InvoiceLineItem> = emptyList(),

    // Totals
    val currency: String? = null,        // EUR, USD, etc.
    val subtotal: String? = null,
    val vatBreakdown: List<VatBreakdown> = emptyList(),
    val totalVatAmount: String? = null,
    val totalAmount: String? = null,

    // Payment information
    val iban: String? = null,
    val bic: String? = null,
    val paymentReference: String? = null,

    // Metadata
    val confidence: Double = 0.0,

    // Provenance - links extracted values to source locations
    val provenance: InvoiceProvenance? = null
)

/**
 * A line item on an invoice.
 */
@Serializable
data class InvoiceLineItem(
    val description: String,
    val quantity: Double? = null,
    val unitPrice: String? = null,
    val vatRate: String? = null,         // e.g., "21%"
    val total: String? = null
)

/**
 * VAT breakdown by rate.
 */
@Serializable
data class VatBreakdown(
    val rate: String,                    // e.g., "21%"
    val base: String? = null,
    val amount: String? = null
)
