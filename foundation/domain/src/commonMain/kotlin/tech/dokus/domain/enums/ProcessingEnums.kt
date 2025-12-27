package tech.dokus.domain.enums

import ai.dokus.foundation.domain.database.DbEnum
import kotlinx.serialization.Serializable

/**
 * Processing status for documents undergoing AI extraction.
 */
@Serializable
enum class ProcessingStatus(override val dbValue: String) : DbEnum {
    /** Just uploaded, awaiting processing */
    Pending("PENDING"),

    /** Picked up by background job, in message queue */
    Queued("QUEUED"),

    /** AI is actively extracting data */
    Processing("PROCESSING"),

    /** Extraction complete, awaiting user review */
    Processed("PROCESSED"),

    /** Extraction failed (may be retryable) */
    Failed("FAILED"),

    /** User confirmed extracted data, entity created */
    Confirmed("CONFIRMED"),

    /** User rejected extraction, will do manual entry */
    Rejected("REJECTED");

    companion object {
        fun fromDbValue(value: String): ProcessingStatus = entries.find { it.dbValue == value }!!
    }
}

/**
 * Type of document detected during AI extraction.
 */
@Serializable
enum class DocumentType(override val dbValue: String) : DbEnum {
    Invoice("INVOICE"),
    Bill("BILL"),
    Expense("EXPENSE"),
    Unknown("UNKNOWN")
}
