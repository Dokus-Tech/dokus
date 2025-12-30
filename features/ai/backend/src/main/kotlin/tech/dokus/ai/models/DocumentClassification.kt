package tech.dokus.ai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result of document classification.
 */
@Serializable
data class DocumentClassification(
    val documentType: ClassifiedDocumentType,
    val confidence: Double,
    val reasoning: String
)

/**
 * Classified document types.
 */
@Serializable
enum class ClassifiedDocumentType {
    @SerialName("INVOICE")
    INVOICE,

    @SerialName("RECEIPT")
    RECEIPT,

    @SerialName("BILL")
    BILL,

    @SerialName("UNKNOWN")
    UNKNOWN
}
