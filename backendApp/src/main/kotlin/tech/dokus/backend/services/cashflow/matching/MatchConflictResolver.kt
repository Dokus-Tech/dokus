package tech.dokus.backend.services.cashflow.matching

/**
 * Resolves 1:N conflicts when multiple candidates score above threshold
 * for a single transaction.
 *
 * Uses the margin between best and second-best scores to determine
 * whether the best candidate can be auto-matched or needs review.
 */
internal object MatchConflictResolver {

    /**
     * Given a list of scored candidates (sorted descending by score),
     * compute the margin between the best and second-best.
     *
     * Returns the margin (0.0 if no candidates or only one).
     */
    fun computeMargin(sortedCandidates: List<ScoredCandidate>): Double {
        if (sortedCandidates.size < 2) return 1.0 // No competition
        return sortedCandidates[0].score - sortedCandidates[1].score
    }

    /**
     * Check if the margin is sufficient for auto-match.
     */
    fun hasSufficientMargin(margin: Double): Boolean {
        return margin >= MatchingConstants.CONFLICT_MARGIN
    }
}
