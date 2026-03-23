package tech.dokus.backend.services.banking

import kotlinx.datetime.LocalDate
import tech.dokus.database.repository.banking.BankStatementRepository
import tech.dokus.domain.Money
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching

/**
 * Prevents duplicate statement imports.
 *
 * Dedup strategies:
 * 1. **Strong:** SHA-256 file hash matches existing statement → Skip
 * 2. **Weak:** IBAN + period end + closing balance matches → NeedsReview
 * 3. **No match** → New
 */
class StatementDedupService(
    private val bankStatementRepository: BankStatementRepository,
) {
    private val logger = loggerFor()

    sealed class StatementDedupOutcome {
        /** Exact file hash match — skip entirely */
        data object Skip : StatementDedupOutcome()

        /** Weak match (IBAN + period) — allow but flag for review */
        data object NeedsReview : StatementDedupOutcome()

        /** No match — new statement */
        data object New : StatementDedupOutcome()
    }

    suspend fun checkDedup(
        tenantId: TenantId,
        fileHash: String,
        accountIban: Iban?,
        periodEnd: LocalDate?,
        closingBalance: Money?,
    ): Result<StatementDedupOutcome> = runSuspendCatching {
        // Strong dedup: exact file hash match
        val existingByHash = bankStatementRepository.findByFileHash(tenantId, fileHash)
        if (existingByHash != null) {
            logger.info("Statement dedup: exact hash match for tenant {} (hash={})", tenantId, fileHash.take(16))
            return@runSuspendCatching StatementDedupOutcome.Skip
        }

        // Weak dedup: IBAN + period end + closing balance
        if (accountIban != null && periodEnd != null) {
            val candidates = bankStatementRepository.findByIbanAndPeriod(tenantId, accountIban, periodEnd)
            if (candidates.isNotEmpty()) {
                // Check if closing balance also matches
                val balanceMatch = closingBalance != null && candidates.any { it.closingBalance == closingBalance }
                if (balanceMatch) {
                    logger.info(
                        "Statement dedup: weak match (IBAN+period+balance) for tenant {} (iban={})",
                        tenantId,
                        accountIban
                    )
                    return@runSuspendCatching StatementDedupOutcome.NeedsReview
                }
            }
        }

        StatementDedupOutcome.New
    }
}
