package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

/**
 * Source attachment lifecycle status for a canonical document.
 */
@Serializable
enum class DocumentSourceStatus(override val dbValue: String) : DbEnum {
    @SerialName("LINKED")
    Linked("LINKED"),

    @SerialName("PENDING_REVIEW")
    PendingReview("PENDING_REVIEW"),

    @SerialName("DETACHED")
    Detached("DETACHED");
}

/**
 * Why a source was sent to manual match review.
 */
@Serializable
enum class DocumentMatchReviewReasonType(override val dbValue: String) : DbEnum {
    @SerialName("MATERIAL_CONFLICT")
    MaterialConflict("MATERIAL_CONFLICT"),

    @SerialName("FUZZY_CANDIDATE")
    FuzzyCandidate("FUZZY_CANDIDATE");
}

/**
 * Match review workflow status.
 */
@Serializable
enum class DocumentMatchReviewStatus(override val dbValue: String) : DbEnum {
    @SerialName("PENDING")
    Pending("PENDING"),

    @SerialName("RESOLVED_SAME")
    ResolvedSame("RESOLVED_SAME"),

    @SerialName("RESOLVED_DIFFERENT")
    ResolvedDifferent("RESOLVED_DIFFERENT");
}

/**
 * Intake decision surfaced to clients after a source arrives.
 */
@Serializable
enum class DocumentIntakeOutcome(override val dbValue: String) : DbEnum {
    @SerialName("NEW_DOCUMENT")
    NewDocument("NEW_DOCUMENT"),

    @SerialName("LINKED_TO_EXISTING")
    LinkedToExisting("LINKED_TO_EXISTING"),

    @SerialName("PENDING_MATCH_REVIEW")
    PendingMatchReview("PENDING_MATCH_REVIEW");
}

/**
 * Internal match layer used for explainability.
 */
@Serializable
enum class DocumentMatchType(override val dbValue: String) : DbEnum {
    @SerialName("EXACT_FILE")
    ExactFile("EXACT_FILE"),

    @SerialName("SAME_CONTENT")
    SameContent("SAME_CONTENT"),

    @SerialName("SAME_DOCUMENT")
    SameDocument("SAME_DOCUMENT"),

    @SerialName("FUZZY_CANDIDATE")
    FuzzyCandidate("FUZZY_CANDIDATE");
}
