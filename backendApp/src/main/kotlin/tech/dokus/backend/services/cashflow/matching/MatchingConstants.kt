package tech.dokus.backend.services.cashflow.matching

/**
 * Central thresholds and weights for the Bayesian matching engine.
 *
 * All log-odds weights are calibrated so that the sigmoid output maps to
 * a probability (0–1). Positive = evidence for match, negative = evidence against.
 */
internal object MatchingConstants {

    // ── Decision thresholds ─────────────────────────────────────────────
    /** Minimum calibrated score for AUTO_MATCH classification. */
    const val AUTO_MATCH_THRESHOLD = 0.92

    /** Minimum margin (best − second-best) to allow AUTO_MATCH when multiple candidates exist. */
    const val CONFLICT_MARGIN = 0.15

    /** Below this score, the candidate is discarded entirely. */
    const val DISCARD_THRESHOLD = 0.30

    // ── Amount tolerance ────────────────────────────────────────────────
    /** Relative tolerance for "close enough" amount matching (2%). */
    const val AMOUNT_TOLERANCE_PCT = 0.02

    /** Absolute tolerance floor in minor units (€1.00 = 100 cents). */
    const val AMOUNT_TOLERANCE_MINOR = 100L

    // ── Date proximity ──────────────────────────────────────────────────
    /** Gaussian σ for date-proximity signal (days). */
    const val DATE_SIGMA_DAYS = 15.0

    /** Maximum distance beyond which date signal is zero. */
    const val DATE_MAX_DAYS = 90

    // ── Name similarity ─────────────────────────────────────────────────
    /** Jaro-Winkler threshold for "strong" name match. */
    const val STRONG_NAME_THRESHOLD = 0.90

    /** Jaro-Winkler threshold for token-set ratio fallback. */
    const val WEAK_NAME_THRESHOLD = 0.80

    // ── Bayesian log-odds weights ───────────────────────────────────────
    /** Prior log-odds (baseline assumption: unlikely match). */
    const val PRIOR_LOG_ODDS = -2.0

    /** OGM structured communication match (very strong). */
    const val WEIGHT_OGM = 5.0

    /** Invoice reference found in free-form communication. */
    const val WEIGHT_INVOICE_REF = 4.0

    /** Counterparty IBAN matches contact IBAN. */
    const val WEIGHT_COUNTERPARTY_IBAN = 3.5

    /** Exact amount match. */
    const val WEIGHT_AMOUNT_EXACT = 2.5

    /** Amount within tolerance (partial credit). */
    const val WEIGHT_AMOUNT_CLOSE = 1.0

    /** Strong name match (Jaro-Winkler ≥ STRONG_NAME_THRESHOLD). */
    const val WEIGHT_NAME_STRONG = 1.5

    /** Weak name match (Jaro-Winkler ≥ WEAK_NAME_THRESHOLD). */
    const val WEIGHT_NAME_WEAK = 0.5

    /** Date proximity — max contribution at distance = 0, Gaussian decay. */
    const val WEIGHT_DATE_PROXIMITY = 1.5

    /** Historical counterparty → contact pattern found. */
    const val WEIGHT_HISTORICAL_PATTERN = 1.0

    /** Penalty: this pair was previously rejected by a user. */
    const val WEIGHT_REJECTED_GUARD = -6.0
}
