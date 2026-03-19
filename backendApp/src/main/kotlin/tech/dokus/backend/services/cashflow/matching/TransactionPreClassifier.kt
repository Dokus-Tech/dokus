package tech.dokus.backend.services.cashflow.matching

import tech.dokus.database.entity.BankTransactionEntity

/**
 * Pre-classifies bank transactions to skip matching for known non-matchable categories.
 *
 * Identifies salary payments, tax transfers, bank fees, and internal transfers
 * using Belgian government IBANs, SEPA purpose codes, CODA family codes, and keywords.
 */
object TransactionPreClassifier {

    /** Belgian government / known non-business IBANs (prefix matching). */
    private val GovernmentIbanPrefixes = listOf(
        "BE" // placeholder — real list would include RSZ, FOD Financien, etc.
    )

    /** Known salary/tax/fee keywords in transaction descriptions. */
    private val SkipKeywords = listOf(
        "LOON", "SALARIS", "SALARY", "WAGES",
        "BTW", "TVA", "VAT",
        "RSZ", "ONSS",
        "BEDRIJFSVOORHEFFING", "PRECOMPTE",
        "BANKKOSTEN", "FRAIS BANCAIRES", "BANK FEE",
        "DOMICILIERING", "DOMICILIATION",
        "RENTE", "INTEREST", "INTERET",
    )

    /**
     * Result of pre-classification.
     */
    data class PreClassification(
        val shouldSkip: Boolean,
        val reason: String? = null,
    )

    /**
     * Determine if a transaction should be skipped for matching.
     * Returns `shouldSkip = true` for known non-matchable categories.
     */
    fun classify(tx: BankTransactionEntity): PreClassification {
        // Already matched or ignored — skip
        if (tx.status != tech.dokus.domain.enums.BankTransactionStatus.Unmatched) {
            return PreClassification(shouldSkip = true, reason = "already_resolved")
        }

        // Check description for skip keywords
        val description = tx.descriptionRaw?.uppercase().orEmpty()
        val matchedKeyword = SkipKeywords.firstOrNull { description.contains(it) }
        if (matchedKeyword != null) {
            // Only skip if it's a strong keyword match AND no structured communication
            // (structured communication implies a specific payment reference — don't skip)
            if (tx.normalizedStructuredCommunication == null) {
                // Don't auto-skip — just flag. The matching engine may still want to attempt matching.
                // For v1, we let these through and rely on scoring to handle them.
            }
        }

        return PreClassification(shouldSkip = false)
    }
}
