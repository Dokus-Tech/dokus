package tech.dokus.features.ai.models

import kotlinx.serialization.Serializable

// =============================================================================
// Provenance Models for Expense Extraction
// =============================================================================

/**
 * Provenance data for all extracted expense fields.
 * Maps field names to their provenance information.
 */
@Serializable
data class ExpenseProvenance(
    val merchantName: FieldProvenance? = null,
    val description: FieldProvenance? = null,
    val date: FieldProvenance? = null,
    val totalAmount: FieldProvenance? = null,
    val currency: FieldProvenance? = null,
    val category: FieldProvenance? = null,
    val paymentMethod: FieldProvenance? = null
)

// =============================================================================
// Extracted Expense Data with Provenance
// =============================================================================

/**
 * Extracted data from an expense document.
 *
 * An expense is a simplified financial document that represents
 * a cost without detailed line items. Examples include:
 * - Parking tickets
 * - Public transport tickets
 * - Simple service fees
 * - Subscriptions without itemization
 *
 * For itemized purchases, use Receipt instead.
 */
@Serializable
data class ExtractedExpenseData(
    /** Name of the merchant or service provider */
    val merchantName: String? = null,

    /** Description of the expense */
    val description: String? = null,

    /** Date of the expense (ISO format YYYY-MM-DD) */
    val date: String? = null,

    /** Total amount paid */
    val totalAmount: String? = null,

    /** Currency (EUR, USD, etc.) */
    val currency: String? = null,

    /** Suggested expense category */
    val category: String? = null,

    /** Payment method used */
    val paymentMethod: String? = null,

    /** VAT amount if visible */
    val vatAmount: String? = null,

    /** VAT rate if visible */
    val vatRate: String? = null,

    /** Reference or transaction number */
    val reference: String? = null,

    /** Metadata */
    val confidence: Double = 0.0,

    /** Text transcription for RAG indexing */
    val extractedText: String? = null,

    /** Provenance - links extracted values to source locations */
    val provenance: ExpenseProvenance? = null
)
