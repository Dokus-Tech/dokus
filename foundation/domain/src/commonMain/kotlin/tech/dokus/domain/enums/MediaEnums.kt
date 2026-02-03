package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

@Serializable
enum class MediaStatus(override val dbValue: String) : DbEnum {
    @SerialName("PENDING")
    Pending("PENDING"),

    @SerialName("PROCESSING")
    Processing("PROCESSING"),

    @SerialName("PROCESSED")
    Processed("PROCESSED"),

    @SerialName("FAILED")
    Failed("FAILED")
}

@Serializable
enum class MediaDocumentType {
    @SerialName("INVOICE")
    Invoice,

    @SerialName("BILL")
    Bill,

    @SerialName("UNKNOWN")
    Unknown
}
