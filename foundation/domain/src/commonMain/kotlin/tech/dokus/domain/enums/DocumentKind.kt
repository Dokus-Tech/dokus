package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Narrow set of document kinds supported by the AI draft pipeline.
 * Separate from DocumentType to avoid nullable soup in draft data.
 */
@Serializable
enum class DocumentKind {
    @SerialName("invoice")
    Invoice,

    @SerialName("bill")
    Bill,

    @SerialName("credit_note")
    CreditNote,

    @SerialName("receipt")
    Receipt
}
