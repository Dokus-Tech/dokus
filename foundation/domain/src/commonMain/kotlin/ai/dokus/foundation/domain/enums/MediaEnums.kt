package ai.dokus.foundation.domain.enums

import ai.dokus.foundation.domain.database.DbEnum
import kotlinx.serialization.Serializable

@Serializable
enum class MediaStatus(override val dbValue: String) : DbEnum {
    Pending("PENDING"),
    Processing("PROCESSING"),
    Processed("PROCESSED"),
    Failed("FAILED")
}

@Serializable
enum class MediaDocumentType {
    Invoice,
    Expense,
    Bill,
    Unknown
}
