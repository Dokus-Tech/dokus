package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

/**
 * Type of document detected during AI extraction.
 *
 * Note: This represents legal document types per the 4-axis classification system.
 * See CLASSIFICATION.md for full documentation.
 */
@Serializable
enum class DocumentType(override val dbValue: String) : DbEnum {
    @SerialName("INVOICE")
    Invoice("INVOICE"),

    @SerialName("BILL")
    Bill("BILL"),

    @SerialName("EXPENSE")
    Expense("EXPENSE"),

    @SerialName("CREDIT_NOTE")
    CreditNote("CREDIT_NOTE"),

    @SerialName("RECEIPT")
    Receipt("RECEIPT"),

    @SerialName("PRO_FORMA")
    ProForma("PRO_FORMA"),

    @SerialName("UNKNOWN")
    Unknown("UNKNOWN")
}

/**
 * Status of a document ingestion run (AI extraction attempt).
 * Simplified lifecycle: Queued -> Processing -> Succeeded/Failed
 */
@Serializable
enum class IngestionStatus(override val dbValue: String) : DbEnum {
    /** Run is queued for processing */
    @SerialName("QUEUED")
    Queued("QUEUED"),

    /** AI is actively extracting data */
    @SerialName("PROCESSING")
    Processing("PROCESSING"),

    /** Extraction completed successfully */
    @SerialName("SUCCEEDED")
    Succeeded("SUCCEEDED"),

    /** Extraction failed (may be retryable) */
    @SerialName("FAILED")
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
    /** AI ran but threshold not met - user must fill fields manually */
    @SerialName("NEEDS_INPUT")
    NeedsInput("NEEDS_INPUT"),

    /** Draft has partial data and needs user review */
    @SerialName("NEEDS_REVIEW")
    NeedsReview("NEEDS_REVIEW"),

    /** Draft is ready for confirmation (all required fields present) */
    @SerialName("READY")
    Ready("READY"),

    /** User confirmed, financial entity created */
    @SerialName("CONFIRMED")
    Confirmed("CONFIRMED"),

    /** User rejected extraction */
    @SerialName("REJECTED")
    Rejected("REJECTED");

    companion object {
        fun fromDbValue(value: String): DraftStatus = entries.find { it.dbValue == value }!!
    }
}

/**
 * Counterparty intent for document drafts.
 * Tracks whether the user has explicitly chosen to link or skip a counterparty.
 */
@Serializable
enum class CounterpartyIntent(override val dbValue: String) : DbEnum {
    @SerialName("NONE")
    None("NONE"),

    @SerialName("PENDING")
    Pending("PENDING");

    companion object {
        fun fromDbValue(value: String): CounterpartyIntent = entries.find { it.dbValue == value }!!
    }
}

/**
 * Reason for rejecting a document during confirmation.
 */
@Serializable
enum class DocumentRejectReason(override val dbValue: String) : DbEnum {
    @SerialName("NOT_MY_BUSINESS")
    NotMyBusiness("NOT_MY_BUSINESS"),

    @SerialName("DUPLICATE")
    Duplicate("DUPLICATE"),

    @SerialName("SPAM")
    Spam("SPAM"),

    @SerialName("TEST")
    Test("TEST"),

    @SerialName("OTHER")
    Other("OTHER");

    companion object {
        fun fromDbValue(value: String): DocumentRejectReason = entries.find { it.dbValue == value }!!
    }
}

/**
 * Status of document chunk indexing (RAG preparation).
 * Tracked separately from ingestion status to allow retry of indexing.
 */
@Serializable
enum class IndexingStatus(override val dbValue: String) : DbEnum {
    /** Indexing not yet started */
    @SerialName("PENDING")
    Pending("PENDING"),

    /** Chunks created and indexed successfully */
    @SerialName("SUCCEEDED")
    Succeeded("SUCCEEDED"),

    /** Indexing failed (retryable) */
    @SerialName("FAILED")
    Failed("FAILED");

    companion object {
        fun fromDbValue(value: String): IndexingStatus = entries.find { it.dbValue == value }!!
    }
}

/**
 * Type of link between documents.
 *
 * Used in DocumentLinksTable to track document-to-document relationships:
 * - ConvertedTo: ProForma converted to Invoice
 * - OriginalDocument: CreditNote referencing original Invoice/Bill
 * - RelatedTo: Generic document relationship
 *
 * No source/target type enums needed - they are derivable from the linked documents.
 */
@Serializable
enum class DocumentLinkType(override val dbValue: String) : DbEnum {
    /** ProForma → Invoice conversion */
    @SerialName("CONVERTED_TO")
    ConvertedTo("CONVERTED_TO"),

    /** CreditNote → Original Invoice/Bill reference */
    @SerialName("ORIGINAL_DOC")
    OriginalDocument("ORIGINAL_DOC"),

    /** Generic document relationship */
    @SerialName("RELATED_TO")
    RelatedTo("RELATED_TO");

    companion object {
        fun fromDbValue(value: String): DocumentLinkType = entries.find { it.dbValue == value }!!
    }
}
