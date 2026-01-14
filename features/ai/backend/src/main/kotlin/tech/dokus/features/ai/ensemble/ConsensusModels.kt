package tech.dokus.features.ai.ensemble

import kotlinx.serialization.Serializable

/**
 * Models for the Consensus Engine (Layer 2).
 *
 * The consensus system compares field-by-field extractions from multiple models
 * and resolves conflicts based on configurable weights.
 */

// =============================================================================
// Model Weight Configuration
// =============================================================================

/**
 * Weight preference when models disagree on a field value.
 */
enum class ModelWeight {
    /** Use the fast model's value when there's a conflict */
    PREFER_FAST,

    /** Use the expert model's value when there's a conflict (default) */
    PREFER_EXPERT,

    /** Both models must agree - if they don't, flag for human review */
    REQUIRE_MATCH
}

// =============================================================================
// Conflict Tracking
// =============================================================================

/**
 * Severity of a field conflict between models.
 */
enum class ConflictSeverity {
    /** Minor disagreement - can be resolved automatically */
    WARNING,

    /** Critical disagreement - requires review or blocks auto-approval */
    CRITICAL
}

/**
 * Represents a disagreement between models on a specific field.
 */
@Serializable
data class FieldConflict(
    /** Name of the field that has conflicting values */
    val field: String,

    /** Value extracted by the fast model */
    val fastValue: String,

    /** Value extracted by the expert model */
    val expertValue: String,

    /** Value chosen for the merged result (may be null if REQUIRE_MATCH) */
    val chosenValue: String?,

    /** Which model's value was chosen */
    val chosenSource: String,

    /** Severity of this conflict */
    val severity: ConflictSeverity
)

/**
 * Report of all conflicts found during consensus merging.
 */
@Serializable
data class ConflictReport(
    /** All conflicts detected */
    val conflicts: List<FieldConflict>
) {
    /** Whether any conflicts were detected */
    val hasConflicts: Boolean = conflicts.isNotEmpty()

    /** Critical conflicts that may block auto-approval */
    val criticalConflicts: List<FieldConflict> = conflicts.filter {
        it.severity == ConflictSeverity.CRITICAL
    }

    /** Warning-level conflicts (resolved automatically) */
    val warningConflicts: List<FieldConflict> = conflicts.filter {
        it.severity == ConflictSeverity.WARNING
    }

    /** Count of conflicts by field name */
    val conflictsByField: Map<String, FieldConflict> = conflicts.associateBy { it.field }

    companion object {
        /** Empty report with no conflicts */
        val EMPTY = ConflictReport(emptyList())
    }
}

// =============================================================================
// Consensus Result
// =============================================================================

/**
 * Result of consensus merging between two model extractions.
 *
 * @param T The type of extracted data (e.g., ExtractedInvoiceData)
 */
sealed class ConsensusResult<out T> {

    /**
     * Neither model produced data.
     */
    data object NoData : ConsensusResult<Nothing>()

    /**
     * Only one model produced data.
     *
     * @param data The extracted data from the single source
     * @param source Which model produced the data ("fast" or "expert")
     */
    data class SingleSource<T>(
        val data: T,
        val source: String
    ) : ConsensusResult<T>()

    /**
     * Both models agreed on all fields.
     *
     * @param data The merged data (unanimous agreement)
     */
    data class Unanimous<T>(
        val data: T
    ) : ConsensusResult<T>()

    /**
     * Models had some disagreements that were resolved.
     *
     * @param data The merged data with conflicts resolved
     * @param report Details of the conflicts and resolutions
     */
    data class WithConflicts<T>(
        val data: T,
        val report: ConflictReport
    ) : ConsensusResult<T>()

    /**
     * Get the merged data if available.
     */
    fun dataOrNull(): T? = when (this) {
        is NoData -> null
        is SingleSource -> data
        is Unanimous -> data
        is WithConflicts -> data
    }

    /**
     * Get the conflict report if any conflicts exist.
     */
    fun reportOrNull(): ConflictReport? = when (this) {
        is WithConflicts -> report
        else -> null
    }

    /**
     * Whether this result has any data.
     */
    val hasData: Boolean
        get() = this !is NoData

    /**
     * Whether both models contributed to this result.
     */
    val hasBothSources: Boolean
        get() = this is Unanimous || this is WithConflicts
}

// =============================================================================
// Ensemble Result (Layer 1 output)
// =============================================================================

/**
 * Result from running the perception ensemble (parallel model execution).
 *
 * @param T The type of extracted data
 */
data class EnsembleResult<T>(
    /** Extraction result from the fast model (may be null if failed) */
    val fastCandidate: T?,

    /** Extraction result from the expert model (may be null if failed) */
    val expertCandidate: T?,

    /** Error from fast model if it failed */
    val fastError: Throwable?,

    /** Error from expert model if it failed */
    val expertError: Throwable?
) {
    /** Whether both models produced results */
    val hasBothCandidates: Boolean = fastCandidate != null && expertCandidate != null

    /** Whether at least one model produced a result */
    val hasAnyCandidate: Boolean = fastCandidate != null || expertCandidate != null

    /** Whether the fast model succeeded */
    val fastSucceeded: Boolean = fastCandidate != null

    /** Whether the expert model succeeded */
    val expertSucceeded: Boolean = expertCandidate != null

    /** Get the best available candidate (prefer expert) */
    val bestCandidate: T?
        get() = expertCandidate ?: fastCandidate
}
