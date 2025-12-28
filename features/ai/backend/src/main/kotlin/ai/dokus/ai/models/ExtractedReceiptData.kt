package ai.dokus.ai.models

import kotlinx.serialization.Serializable

// =============================================================================
// Provenance Models for Receipt Extraction
// =============================================================================

/**
 * Provenance data for all extracted receipt fields.
 * Maps field names to their provenance information.
 */
@Serializable
data class ReceiptProvenance(
    // Merchant fields provenance
    val merchantName: FieldProvenance? = null,
    val merchantAddress: FieldProvenance? = null,
    val merchantVatNumber: FieldProvenance? = null,

    // Transaction details provenance
    val receiptNumber: FieldProvenance? = null,
    val transactionDate: FieldProvenance? = null,
    val transactionTime: FieldProvenance? = null,

    // Totals provenance
    val currency: FieldProvenance? = null,
    val subtotal: FieldProvenance? = null,
    val vatAmount: FieldProvenance? = null,
    val totalAmount: FieldProvenance? = null,

    // Payment provenance
    val paymentMethod: FieldProvenance? = null,
    val cardLastFour: FieldProvenance? = null,

    // Category provenance
    val category: FieldProvenance? = null
)

// =============================================================================
// Extracted Receipt Data with Provenance
// =============================================================================

/**
 * Extracted data from a receipt document.
 */
@Serializable
data class ExtractedReceiptData(
    // Merchant information
    val merchantName: String? = null,
    val merchantAddress: String? = null,
    val merchantVatNumber: String? = null,

    // Transaction details
    val receiptNumber: String? = null,
    val transactionDate: String? = null,  // ISO format YYYY-MM-DD
    val transactionTime: String? = null,  // HH:mm format

    // Items (simplified compared to invoices)
    val items: List<ReceiptItem> = emptyList(),

    // Totals
    val currency: String? = null,
    val subtotal: String? = null,
    val vatAmount: String? = null,
    val totalAmount: String? = null,

    // Payment
    val paymentMethod: String? = null,    // Cash, Card, etc.
    val cardLastFour: String? = null,     // Last 4 digits if card payment

    // Category suggestion
    val suggestedCategory: String? = null,

    // Metadata
    val confidence: Double = 0.0,

    // Provenance - links extracted values to source locations
    val provenance: ReceiptProvenance? = null
)

/**
 * An item on a receipt.
 */
@Serializable
data class ReceiptItem(
    val description: String,
    val quantity: Double? = null,
    val price: String? = null
)
