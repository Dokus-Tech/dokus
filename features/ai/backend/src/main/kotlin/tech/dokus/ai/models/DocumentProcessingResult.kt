package tech.dokus.ai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of two-step document processing (classification + extraction).
 */
@Serializable
data class DocumentProcessingResult(
    val classification: DocumentClassification,
    val extractedData: ExtractedDocumentData
)

/**
 * Sealed class for type-safe extracted data.
 */
@Serializable
sealed class ExtractedDocumentData {
    @Serializable
    @SerialName("Invoice")
    data class Invoice(val data: ExtractedInvoiceData) : ExtractedDocumentData()

    @Serializable
    @SerialName("Receipt")
    data class Receipt(val data: ExtractedReceiptData) : ExtractedDocumentData()

    @Serializable
    @SerialName("Bill")
    data class Bill(val data: ExtractedInvoiceData) : ExtractedDocumentData()  // Bills use invoice structure
}
