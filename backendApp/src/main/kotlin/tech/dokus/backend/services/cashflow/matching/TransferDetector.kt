package tech.dokus.backend.services.cashflow.matching

import kotlin.math.abs
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import tech.dokus.database.repository.banking.BankAccountRepository
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Detects internal transfers between tenant-owned accounts.
 *
 * Auto-detection requires ALL of:
 * - Counterparty IBAN matches a known owned account
 * - Destination account is different from source
 * - Same currency
 * - Same absolute amount (within rounding tolerance)
 * - Date within tight window (±2 days)
 * - Opposite-direction counterpart exists on the other account
 *
 * If only one side exists → [TransferResult.LikelyTransfer] (NEEDS_REVIEW)
 * If both sides exist and match → [TransferResult.ClearPair] (auto-match)
 */
class TransferDetector(
    private val bankAccountRepository: BankAccountRepository,
    private val bankTransactionRepository: BankTransactionRepository,
) {
    private val logger = loggerFor()

    suspend fun detect(
        tenantId: TenantId,
        transaction: BankTransactionDto,
    ): TransferResult? {
        val sourceAccountId = transaction.bankAccountId ?: return null
        val counterpartyIban = transaction.counterparty.iban?.value ?: return null

        // Check if counterparty IBAN matches a known owned account
        val destinationAccount = bankAccountRepository.findByIban(tenantId, Iban(counterpartyIban))
            ?: return null

        // Must be a different account
        if (destinationAccount.id == sourceAccountId) return null

        // Must not be inactive/stale
        if (!destinationAccount.isActive) return null

        // Must be same currency
        if (transaction.currency != destinationAccount.currency) {
            logger.debug("Transfer skipped: currency mismatch {} vs {}", transaction.currency, destinationAccount.currency)
            return TransferResult.LikelyTransfer(
                destinationAccountId = destinationAccount.id,
                reason = "Currency mismatch — manual review required",
            )
        }

        // Look for matching counterpart on destination account
        val counterpart = findCounterpart(tenantId, destinationAccount.id, transaction)

        return if (counterpart != null) {
            logger.info(
                "Clear transfer pair detected: {} ↔ {} (amount={}, accounts={} → {})",
                transaction.id, counterpart.id, transaction.signedAmount, sourceAccountId, destinationAccount.id
            )
            TransferResult.ClearPair(
                counterpartTransactionId = counterpart.id,
                destinationAccountId = destinationAccount.id,
            )
        } else {
            logger.info(
                "Likely one-sided transfer: {} to account {} (counterpart not yet imported)",
                transaction.id, destinationAccount.id
            )
            TransferResult.LikelyTransfer(
                destinationAccountId = destinationAccount.id,
                reason = "Counterpart not yet imported — one-sided pending transfer",
            )
        }
    }

    private suspend fun findCounterpart(
        tenantId: TenantId,
        destinationAccountId: BankAccountId,
        source: BankTransactionDto,
    ): BankTransactionDto? {
        val dateWindow = 2L // ±2 days
        val startDate = source.transactionDate.minus(dateWindow, DateTimeUnit.DAY)
        val endDate = source.transactionDate.plus(dateWindow, DateTimeUnit.DAY)
        val expectedAmount = -source.signedAmount.minor // Opposite sign

        val candidates = bankTransactionRepository.findUnmatchedByAccountAndDateRange(
            tenantId = tenantId,
            accountId = destinationAccountId,
            startDate = startDate,
            endDate = endDate,
        )

        return candidates.firstOrNull { candidate ->
            // Same absolute amount (within small tolerance for rounding)
            val amountDiff = abs(candidate.signedAmount.minor - expectedAmount)
            amountDiff <= AMOUNT_TOLERANCE_MINOR &&
                candidate.currency == source.currency &&
                candidate.status in MATCHABLE_STATUSES
        }
    }

    companion object {
        private const val AMOUNT_TOLERANCE_MINOR = 5L // ±0.05 in minor units
        private val MATCHABLE_STATUSES = setOf(
            BankTransactionStatus.Unmatched,
            BankTransactionStatus.NeedsReview,
        )
    }
}

sealed interface TransferResult {
    data class ClearPair(
        val counterpartTransactionId: BankTransactionId,
        val destinationAccountId: BankAccountId,
    ) : TransferResult

    data class LikelyTransfer(
        val destinationAccountId: BankAccountId,
        val reason: String,
    ) : TransferResult
}
