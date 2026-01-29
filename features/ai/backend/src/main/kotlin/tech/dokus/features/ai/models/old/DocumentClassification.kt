package tech.dokus.features.ai.models.old

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

    @SerialName("CREDIT_NOTE")
    CREDIT_NOTE,

    @SerialName("PRO_FORMA")
    PRO_FORMA,

    @SerialName("BILL")
    BILL,

    @SerialName("RECEIPT")
    RECEIPT,

    @SerialName("EXPENSE")
    EXPENSE,

    @SerialName("UNKNOWN")
    UNKNOWN
}
