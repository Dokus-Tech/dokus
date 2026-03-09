package tech.dokus.backend.services.banking

import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.banking.BankAccountRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.AccountBalanceSeries
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.BalanceHistoryPoint
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class BankingService(
    private val bankTransactionRepository: BankTransactionRepository,
    private val bankAccountRepository: BankAccountRepository,
) {
    private val logger = loggerFor()

    suspend fun listAccounts(tenantId: TenantId): Result<List<BankAccountDto>> = runSuspendCatching {
        bankAccountRepository.listAccounts(tenantId)
    }

    suspend fun getAccountSummary(tenantId: TenantId): Result<BankAccountSummary> = runSuspendCatching {
        val accounts = bankAccountRepository.listAccounts(tenantId)
        val statusCounts = bankTransactionRepository.countByStatus(tenantId)
        val unmatchedCount = (statusCounts[BankTransactionStatus.Unmatched] ?: 0L).toInt()
        val needsReviewCount = (statusCounts[BankTransactionStatus.NeedsReview] ?: 0L).toInt()
        val matchedCount = (statusCounts[BankTransactionStatus.Matched] ?: 0L).toInt()
        val unresolvedMinor = bankTransactionRepository.sumUnresolved(tenantId)

        val totalBalanceMinor = accounts.mapNotNull { it.balance?.minor }.sum()
        val lastSynced = accounts.mapNotNull { it.balanceUpdatedAt }.maxOrNull()

        BankAccountSummary(
            totalBalance = Money(totalBalanceMinor),
            accountCount = accounts.size,
            unmatchedCount = unmatchedCount + needsReviewCount,
            totalUnresolvedAmount = Money(unresolvedMinor),
            matchedThisPeriod = matchedCount,
            lastSyncedAt = lastSynced,
        )
    }

    suspend fun listTransactions(
        tenantId: TenantId,
        status: BankTransactionStatus? = null,
        source: BankTransactionSource? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null
    ): Result<List<BankTransactionDto>> = runSuspendCatching {
        bankTransactionRepository.listAll(
            tenantId = tenantId,
            status = status,
            source = source,
            fromDate = fromDate,
            toDate = toDate
        )
    }

    suspend fun getTransactionSummary(tenantId: TenantId): Result<BankTransactionSummary> = runSuspendCatching {
        val statusCounts = bankTransactionRepository.countByStatus(tenantId)
        val unresolvedMinor = bankTransactionRepository.sumUnresolved(tenantId)

        BankTransactionSummary(
            unmatchedCount = (statusCounts[BankTransactionStatus.Unmatched] ?: 0L).toInt(),
            needsReviewCount = (statusCounts[BankTransactionStatus.NeedsReview] ?: 0L).toInt(),
            matchedCount = (statusCounts[BankTransactionStatus.Matched] ?: 0L).toInt(),
            ignoredCount = (statusCounts[BankTransactionStatus.Ignored] ?: 0L).toInt(),
            totalCount = statusCounts.values.sum().toInt(),
            totalUnresolvedAmount = Money(unresolvedMinor)
        )
    }

    suspend fun getTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runSuspendCatching {
        bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")
    }

    suspend fun linkTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId
    ): Result<BankTransactionDto> = runSuspendCatching {
        val updated = bankTransactionRepository.markMatched(
            tenantId = tenantId,
            transactionId = transactionId,
            cashflowEntryId = cashflowEntryId,
            matchedBy = MatchedBy.Manual,
            resolutionType = ResolutionType.Document,
        )
        if (!updated) throw DokusException.NotFound("Bank transaction not found")
        logger.info("Linked transaction {} to entry {} for tenant {}", transactionId, cashflowEntryId, tenantId)
        bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found after update")
    }

    suspend fun ignoreTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        reason: IgnoredReason,
        ignoredBy: String,
    ): Result<BankTransactionDto> = runSuspendCatching {
        val updated = bankTransactionRepository.markIgnored(tenantId, transactionId, reason, ignoredBy)
        if (!updated) throw DokusException.NotFound("Bank transaction not found")
        logger.info("Ignored transaction {} (reason={}) for tenant {}", transactionId, reason, tenantId)
        bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found after update")
    }

    suspend fun confirmSuggestedMatch(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runSuspendCatching {
        val transaction = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")

        val matchedEntryId = transaction.matchedCashflowId
            ?: throw DokusException.BadRequest("Transaction has no suggested match to confirm")

        val updated = bankTransactionRepository.markMatched(
            tenantId = tenantId,
            transactionId = transactionId,
            cashflowEntryId = matchedEntryId,
            matchedBy = MatchedBy.Review,
            resolutionType = ResolutionType.Document,
        )
        if (!updated) throw DokusException.InternalError("Failed to confirm match")
        logger.info("Confirmed suggested match for transaction {} -> entry {} for tenant {}", transactionId, matchedEntryId, tenantId)
        bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found after update")
    }

    /**
     * Compute daily balance history per account over [days] days.
     * Works backwards from current balance using transaction amounts.
     */
    suspend fun getBalanceHistory(
        tenantId: TenantId,
        days: Int,
    ): Result<BalanceHistoryResponse> = runSuspendCatching {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val startDate = today.minus(days, DateTimeUnit.DAY)
        val accounts = bankAccountRepository.listAccounts(tenantId)
        val transactions = bankTransactionRepository.listAll(
            tenantId = tenantId,
            fromDate = startDate,
            toDate = today,
        )

        // Group transactions by account and date
        val txByAccount = transactions.groupBy { it.bankAccountId }

        val accountSeries = accounts.map { account ->
            val currentBalance = account.balance?.minor ?: 0L
            val accountTxs = txByAccount[account.id] ?: emptyList()

            // Group by date, sum amounts per day
            val dailyAmounts = accountTxs
                .groupBy { it.transactionDate }
                .mapValues { (_, txs) -> txs.sumOf { it.signedAmount.minor } }

            // Build daily points: work backwards from current balance
            val dates = generateDateRange(startDate, today)
            var runningBalance = currentBalance
            val points = dates.reversed().map { date ->
                val point = BalanceHistoryPoint(date = date, balance = Money(runningBalance))
                val dayAmount = dailyAmounts[date] ?: 0L
                runningBalance -= dayAmount
                point
            }.reversed()

            AccountBalanceSeries(
                accountId = account.id,
                accountName = account.name,
                points = points,
            )
        }

        // Compute total series by summing balances across accounts per day
        val dates = generateDateRange(startDate, today)
        val totalSeries = dates.mapIndexed { index, date ->
            val totalMinor = accountSeries.sumOf { series ->
                series.points.getOrNull(index)?.balance?.minor ?: 0L
            }
            BalanceHistoryPoint(date = date, balance = Money(totalMinor))
        }

        BalanceHistoryResponse(series = accountSeries, totalSeries = totalSeries)
    }

    private fun generateDateRange(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var current = start
        while (current <= end) {
            dates.add(current)
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return dates
    }
}
