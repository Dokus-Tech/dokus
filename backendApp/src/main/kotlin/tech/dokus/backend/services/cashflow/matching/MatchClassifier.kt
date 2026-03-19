package tech.dokus.backend.services.cashflow.matching

/**
 * Classifies a match decision based on the AUTO_MATCH gates:
 *
 * 1. Score ≥ AUTO_MATCH_THRESHOLD (0.92)
 * 2. At least one hard signal present (OGM, InvoiceRef, or CounterpartyIban)
 * 3. Not disqualified (rejected guard not fired)
 * 4. Sufficient margin over second-best candidate
 */
object MatchClassifier {

    fun classify(
        best: ScoredCandidate,
        margin: Double,
    ): MatchDecisionType {
        // Gate 1: Score threshold
        if (best.score < MatchingConstants.AUTO_MATCH_THRESHOLD) {
            return if (best.score >= MatchingConstants.DISCARD_THRESHOLD) {
                MatchDecisionType.NeedsReview
            } else {
                MatchDecisionType.Discard
            }
        }

        // Gate 2: Hard signal required
        if (!best.hasHardSignal) return MatchDecisionType.NeedsReview

        // Gate 3: Rejected guard must not be active
        val rejectedFired = best.signals.any {
            it.signal == tech.dokus.domain.enums.MatchSignalType.RejectedGuard && it.fired
        }
        if (rejectedFired) return MatchDecisionType.NeedsReview

        // Gate 4: Sufficient margin
        if (!MatchConflictResolver.hasSufficientMargin(margin)) return MatchDecisionType.NeedsReview

        return MatchDecisionType.AutoMatch
    }
}
