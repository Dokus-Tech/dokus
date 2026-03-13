package tech.dokus.backend.services.cashflow.matching

import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.CashflowEntry
import kotlin.math.abs
import kotlin.math.max

/**
 * Filters candidate entries to only those within matching range of a transaction.
 *
 * Applies direction coherence + amount tolerance window to reduce the candidate set
 * before the (more expensive) scoring pass.
 */
internal object MatchCandidateBlocker {

    /**
     * Determine the expected direction for matching based on the transaction's signed amount.
     */
    fun inferDirection(signedAmount: Money): CashflowDirection {
        return when {
            signedAmount.isPositive -> CashflowDirection.In
            signedAmount.isNegative -> CashflowDirection.Out
            else -> CashflowDirection.Neutral
        }
    }

    /**
     * Filter candidate entries to those within the amount tolerance window.
     *
     * Uses both relative (%) and absolute (floor) tolerance.
     */
    fun filterByAmountRange(
        entries: List<CashflowEntry>,
        absoluteTransactionAmount: Money,
        tolerancePct: Double = MatchingConstants.AMOUNT_TOLERANCE_PCT,
        toleranceFloor: Long = MatchingConstants.AMOUNT_TOLERANCE_MINOR,
    ): List<CashflowEntry> {
        val txMinor = abs(absoluteTransactionAmount.minor)
        val relativeTolerance = (txMinor * tolerancePct).toLong()
        val tolerance = max(relativeTolerance, toleranceFloor)

        return entries.filter { entry ->
            if (entry.remainingAmount.isZero) return@filter false
            val entryMinor = abs(entry.remainingAmount.minor)
            val delta = abs(txMinor - entryMinor)
            delta <= tolerance
        }
    }
}
