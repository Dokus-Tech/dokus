package tech.dokus.backend.services.banking

import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankAccountStatus
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.model.DocumentDraftData.BankStatementDraftData

/**
 * Calculates statement-level trust based on PDF balance reconciliation.
 *
 * Trust levels:
 * - HIGH: opening + sum(txns) == closing (exact reconciliation)
 * - MEDIUM: gap < 1% of closing balance, or account is PendingReview
 * - LOW: no balances available, or large reconciliation gap
 */
class StatementTrustCalculator {

    data class TrustResult(
        val trust: StatementTrust,
        val reconciliationGap: Money?,
    )

    /**
     * Calculate trust from draft data and validated row amounts.
     *
     * @param draftData The bank statement draft with optional opening/closing balances
     * @param validRowAmounts Sum of signed amounts from validated rows (minor units)
     * @param accountStatus Status of the resolved bank account, or null if unresolved
     */
    fun calculate(
        draftData: BankStatementDraftData,
        validRowAmounts: Long,
        accountStatus: BankAccountStatus? = null,
    ): TrustResult {
        val opening = draftData.openingBalance
        val closing = draftData.closingBalance

        // No balances → LOW trust
        if (opening == null || closing == null) {
            return TrustResult(trust = StatementTrust.Low, reconciliationGap = null)
        }

        // Compute expected closing: opening + sum of transaction amounts
        val expectedClosingMinor = opening.minor + validRowAmounts
        val actualClosingMinor = closing.minor
        val gapMinor = expectedClosingMinor - actualClosingMinor
        val gap = Money(gapMinor)

        // Cap at MEDIUM if account is PendingReview
        val maxTrust = if (accountStatus == BankAccountStatus.PendingReview) {
            StatementTrust.Medium
        } else {
            StatementTrust.High
        }

        return when {
            // Exact match → HIGH (or MEDIUM if capped)
            gapMinor == 0L -> TrustResult(trust = maxTrust, reconciliationGap = gap)

            // Gap < 1% of closing balance → MEDIUM
            isWithinTolerance(gapMinor, actualClosingMinor) -> {
                TrustResult(trust = minOf(StatementTrust.Medium, maxTrust), reconciliationGap = gap)
            }

            // Large gap → LOW
            else -> TrustResult(trust = StatementTrust.Low, reconciliationGap = gap)
        }
    }

    private fun isWithinTolerance(gapMinor: Long, closingMinor: Long): Boolean {
        if (closingMinor == 0L) return false
        val absGap = kotlin.math.abs(gapMinor)
        val absClosing = kotlin.math.abs(closingMinor)
        // Gap < 1% of closing balance
        return absGap * 100 <= absClosing
    }

    private fun minOf(a: StatementTrust, b: StatementTrust): StatementTrust {
        return if (a.ordinal <= b.ordinal) a else b
    }
}
