package tech.dokus.domain.enums

import tech.dokus.domain.database.DbEnum
import kotlinx.serialization.Serializable

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

/**
 * Status of a document ingestion run (AI extraction attempt).
 * Simplified lifecycle: Queued -> Processing -> Succeeded/Failed
 */
@Serializable
enum class IngestionStatus(override val dbValue: String) : DbEnum {
    /** Run is queued for processing */
    Queued("QUEUED"),

    /** AI is actively extracting data */
    Processing("PROCESSING"),

    /** Extraction completed successfully */
    Succeeded("SUCCEEDED"),

    /** Extraction failed (may be retryable) */
    Failed("FAILED");

    companion object {
        fun fromDbValue(value: String): IngestionStatus = entries.find { it.dbValue == value }!!
    }
}

/**
 * Status of a document draft (editable extraction state).
 * Represents business review state, separate from ingestion lifecycle.
 */
@Serializable
enum class DraftStatus(override val dbValue: String) : DbEnum {
    /** Draft needs user review */
    NeedsReview("NEEDS_REVIEW"),

    /** Draft is ready for confirmation */
    Ready("READY"),

    /** User confirmed, financial entity created */
    Confirmed("CONFIRMED"),

    /** User rejected extraction */
    Rejected("REJECTED");

    companion object {
        fun fromDbValue(value: String): DraftStatus = entries.find { it.dbValue == value }!!
    }
}
