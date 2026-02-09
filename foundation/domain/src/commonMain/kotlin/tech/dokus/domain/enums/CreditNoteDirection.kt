package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class CreditNoteDirection {
    @SerialName("sales")
    Sales,

    @SerialName("purchase")
    Purchase,

    @SerialName("unknown")
    Unknown
}
