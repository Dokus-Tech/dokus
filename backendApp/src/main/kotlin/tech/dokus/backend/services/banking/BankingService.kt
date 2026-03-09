package tech.dokus.backend.services.banking

import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.banking.BankingRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.AccountBalanceSeries
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankConnectionDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.domain.model.BalanceHistoryPoint
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

class BankingService(
    private val bankTransactionRepository: BankTransactionRepository,
    private val bankingRepository: BankingRepository
) {
    private val logger = loggerFor()

    suspend fun listConnections(tenantId: TenantId): Result<List<BankConnectionDto>> {
        return bankingRepository.listConnections(tenantId)
    }

    suspend fun getAccountSummary(tenantId: TenantId): Result<BankAccountSummary> = runSuspendCatching {
        val connections = bankingRepository.listConnections(tenantId).getOrThrow()
        val statusCounts = bankTransactionRepository.countByStatus(tenantId)
        val unmatchedCount = (statusCounts[BankTransactionStatus.Unmatched] ?: 0L).toInt()
        val suggestedCount = (statusCounts[BankTransactionStatus.Suggested] ?: 0L).toInt()
        val matchedCount = (statusCounts[BankTransactionStatus.Linked] ?: 0L).toInt()
        val unresolvedMinor = bankTransactionRepository.sumUnresolved(tenantId)

        val totalBalanceMinor = connections.mapNotNull { it.balance?.minor }.sum()
        val lastSynced = connections.mapNotNull { it.lastSyncedAt }.maxOrNull()

        BankAccountSummary(
            totalBalance = Money(totalBalanceMinor),
            accountCount = connections.size,
            unmatchedCount = unmatchedCount + suggestedCount,
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
            needsReviewCount = (statusCounts[BankTransactionStatus.Suggested] ?: 0L).toInt(),
            matchedCount = (statusCounts[BankTransactionStatus.Linked] ?: 0L).toInt(),
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
        val updated = bankTransactionRepository.markLinked(tenantId, transactionId, cashflowEntryId)
        if (!updated) throw DokusException.NotFound("Bank transaction not found")
        logger.info("Linked transaction {} to entry {} for tenant {}", transactionId, cashflowEntryId, tenantId)
        bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found after update")
    }

    suspend fun ignoreTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runSuspendCatching {
        val updated = bankTransactionRepository.markIgnored(tenantId, transactionId)
        if (!updated) throw DokusException.NotFound("Bank transaction not found")
        logger.info("Ignored transaction {} for tenant {}", transactionId, tenantId)
        bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found after update")
    }

    suspend fun confirmSuggestedMatch(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runSuspendCatching {
        val transaction = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")

        val suggestedEntryId = transaction.suggestedCashflowEntryId
            ?: throw DokusException.BadRequest("Transaction has no suggested match to confirm")

        val updated = bankTransactionRepository.markLinked(tenantId, transactionId, suggestedEntryId)
        if (!updated) throw DokusException.InternalError("Failed to confirm match")
        logger.info("Confirmed suggested match for transaction {} -> entry {} for tenant {}", transactionId, suggestedEntryId, tenantId)
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
        val connections = bankingRepository.listConnections(tenantId).getOrThrow()
        val transactions = bankTransactionRepository.listAll(
            tenantId = tenantId,
            fromDate = startDate,
            toDate = today,
        )

        // Group transactions by connection and date
        val txByConnection = transactions.groupBy { it.bankConnectionId }

        val accountSeries = connections.map { conn ->
            val currentBalance = conn.balance?.minor ?: 0L
            val connTxs = txByConnection[conn.id] ?: emptyList()

            // Group by date, sum amounts per day
            val dailyAmounts = connTxs
                .groupBy { it.transactionDate }
                .mapValues { (_, txs) -> txs.sumOf { it.signedAmount.minor } }

            // Build daily points: work backwards from current balance
            val dates = generateDateRange(startDate, today)
            var runningBalance = currentBalance
            val points = dates.reversed().map { date ->
                val point = BalanceHistoryPoint(date = date, balance = Money(runningBalance))
                // Subtract today's transactions to get previous day's balance
                val dayAmount = dailyAmounts[date] ?: 0L
                runningBalance -= dayAmount
                point
            }.reversed()

            AccountBalanceSeries(
                connectionId = conn.id,
                accountName = conn.accountName ?: conn.institutionName,
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
