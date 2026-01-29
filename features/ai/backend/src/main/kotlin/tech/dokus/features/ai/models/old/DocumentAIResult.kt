package tech.dokus.features.ai.models.old

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sealed class representing the result of AI document processing.
 *
 * Contains:
 * - Classification result
 * - Type-specific extracted payload with provenance (AI-only, not for domain DTOs)
 * - Confidence and warnings
 *
 * The worker converts this to:
 * - ExtractedDocumentData (domain, business-only) -> stored in extracted_data
 * - provenance json -> provenance_data column
 * - field confidences json -> field_confidences column
 */
@Serializable
sealed class DocumentAIResult {

    /** The classification that determined this document type */
    abstract val classification: DocumentClassification

    /** Overall extraction confidence (0.0 - 1.0) */
    abstract val confidence: Double

    /** Warnings generated during extraction (non-fatal issues) */
    abstract val warnings: List<String>

    /** Raw text used for extraction (for storage/debugging) */
    abstract val rawText: String

    /**
     * Invoice document extraction result.
     * Outgoing invoices to clients.
     */
    @Serializable
    @SerialName("Invoice")
    data class Invoice(
        override val classification: DocumentClassification,
        val extractedData: ExtractedInvoiceData,
        override val confidence: Double,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()

    /**
     * Credit note extraction result.
     * A document that reduces/refunds a previous invoice.
     * Uses same ExtractedInvoiceData with creditNoteMeta populated.
     */
    @Serializable
    @SerialName("CreditNote")
    data class CreditNote(
        override val classification: DocumentClassification,
        val extractedData: ExtractedInvoiceData,
        override val confidence: Double,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()

    /**
     * Proforma invoice extraction result.
     * A quote/estimate that is not a legal tax invoice.
     * Uses same ExtractedInvoiceData structure.
     */
    @Serializable
    @SerialName("ProForma")
    data class ProForma(
        override val classification: DocumentClassification,
        val extractedData: ExtractedInvoiceData,
        override val confidence: Double,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()

    /**
     * Bill document extraction result.
     * Incoming invoices from suppliers.
     */
    @Serializable
    @SerialName("Bill")
    data class Bill(
        override val classification: DocumentClassification,
        val extractedData: ExtractedBillData,
        override val confidence: Double,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()

    /**
     * Receipt document extraction result.
     * Point-of-sale receipts with itemized purchases.
     */
    @Serializable
    @SerialName("Receipt")
    data class Receipt(
        override val classification: DocumentClassification,
        val extractedData: ExtractedReceiptData,
        override val confidence: Double,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()

    /**
     * Expense document extraction result.
     * Simple cost documents without itemization (parking, transport, etc).
     */
    @Serializable
    @SerialName("Expense")
    data class Expense(
        override val classification: DocumentClassification,
        val extractedData: ExtractedExpenseData,
        override val confidence: Double,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()

    /**
     * Unknown document type - could not be classified.
     */
    @Serializable
    @SerialName("Unknown")
    data class Unknown(
        override val classification: DocumentClassification,
        override val confidence: Double = 0.0,
        override val warnings: List<String> = emptyList(),
        override val rawText: String
    ) : DocumentAIResult()
}
