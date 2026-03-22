package tech.dokus.backend.services.banking

import tech.dokus.backend.services.banking.sse.BankingSsePublisher
import tech.dokus.backend.services.cashflow.ExpenseService
import tech.dokus.backend.services.cashflow.matching.MatchFeedbackStore
import tech.dokus.database.repository.banking.BankTransactionRepository
import tech.dokus.database.repository.banking.BankAccountRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.BankTransactionStatus
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.IgnoredReason
import tech.dokus.domain.enums.MatchedBy
import tech.dokus.domain.enums.ResolutionType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.BankTransactionId
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.TenantId
import tech.dokus.backend.mappers.from
import tech.dokus.domain.model.AccountBalanceSeriesDto
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankAccountSummaryDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummaryDto
import tech.dokus.domain.model.BalanceHistoryPointDto
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.CreateExpenseRequest
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import kotlin.math.abs
import kotlin.uuid.ExperimentalUuidApi
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
    private val expenseService: ExpenseService,
    private val bankingSsePublisher: BankingSsePublisher,
    private val matchFeedbackStore: MatchFeedbackStore,
) {
    private val logger = loggerFor()

    suspend fun listAccounts(tenantId: TenantId): Result<List<BankAccountDto>> = runSuspendCatching {
        bankAccountRepository.listAccounts(tenantId).map { BankAccountDto.from(it) }
    }

    suspend fun getAccountSummary(tenantId: TenantId): Result<BankAccountSummaryDto> = runSuspendCatching {
        val accounts = bankAccountRepository.listAccounts(tenantId)
        val counts = getStatusCounts(tenantId)
        val unresolvedMinor = bankTransactionRepository.sumUnresolved(tenantId)

        val totalBalanceMinor = accounts.mapNotNull { it.balance?.minor }.sum()
        val lastSynced = accounts.mapNotNull { it.balanceUpdatedAt }.maxOrNull()

        BankAccountSummaryDto(
            totalBalance = Money(totalBalanceMinor),
            accountCount = accounts.size,
            unmatchedCount = counts.unmatched + counts.needsReview,
            totalUnresolvedAmount = Money(unresolvedMinor),
            matchedThisPeriod = counts.matched,
            lastSyncedAt = lastSynced,
        )
    }

    data class TransactionPage(
        val items: List<BankTransactionDto>,
        val total: Long,
    )

    suspend fun listTransactions(
        tenantId: TenantId,
        status: BankTransactionStatus? = null,
        source: BankTransactionSource? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        limit: Int? = null,
        offset: Long? = null,
    ): Result<TransactionPage> = runSuspendCatching {
        val items = bankTransactionRepository.listAll(
            tenantId = tenantId,
            status = status,
            source = source,
            fromDate = fromDate,
            toDate = toDate,
            limit = limit,
            offset = offset,
        )
        val total = bankTransactionRepository.countAll(
            tenantId = tenantId,
            status = status,
            source = source,
            fromDate = fromDate,
            toDate = toDate,
        )
        TransactionPage(items = items.map { BankTransactionDto.from(it) }, total = total)
    }

    suspend fun getTransactionSummary(tenantId: TenantId): Result<BankTransactionSummaryDto> = runSuspendCatching {
        val counts = getStatusCounts(tenantId)
        val unresolvedMinor = bankTransactionRepository.sumUnresolved(tenantId)

        BankTransactionSummaryDto(
            unmatchedCount = counts.unmatched,
            needsReviewCount = counts.needsReview,
            matchedCount = counts.matched,
            ignoredCount = counts.ignored,
            totalCount = counts.unmatched + counts.needsReview + counts.matched + counts.ignored,
            totalUnresolvedAmount = Money(unresolvedMinor)
        )
    }

    suspend fun getTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runSuspendCatching {
        val entity = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")
        BankTransactionDto.from(entity)
    }

    suspend fun linkTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        cashflowEntryId: CashflowEntryId
    ): Result<BankTransactionDto> = runSuspendCatching {
        val result = updateAndRefetch(tenantId, transactionId, "Linked transaction {} to entry {} for tenant {}", transactionId, cashflowEntryId, tenantId) {
            bankTransactionRepository.markMatched(
                tenantId = tenantId,
                transactionId = transactionId,
                cashflowEntryId = cashflowEntryId,
                matchedBy = MatchedBy.Manual,
                resolutionType = ResolutionType.Document,
            )
        }
        bankingSsePublisher.publishMatchUpdated(tenantId, transactionId)
        result
    }

    suspend fun ignoreTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        reason: IgnoredReason,
        ignoredBy: String,
    ): Result<BankTransactionDto> = runSuspendCatching {
        updateAndRefetch(tenantId, transactionId, "Ignored transaction {} (reason={}) for tenant {}", transactionId, reason, tenantId) {
            bankTransactionRepository.markIgnored(tenantId, transactionId, reason, ignoredBy)
        }
    }

    suspend fun confirmSuggestedMatch(
        tenantId: TenantId,
        transactionId: BankTransactionId
    ): Result<BankTransactionDto> = runSuspendCatching {
        val transaction = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")

        val matchedEntryId = transaction.matchedCashflowId
            ?: throw DokusException.BadRequest("Transaction has no suggested match to confirm")

        val result = updateAndRefetch(tenantId, transactionId, "Confirmed match for transaction {} -> entry {} for tenant {}", transactionId, matchedEntryId, tenantId) {
            bankTransactionRepository.markMatched(
                tenantId = tenantId,
                transactionId = transactionId,
                cashflowEntryId = matchedEntryId,
                matchedBy = MatchedBy.Review,
                resolutionType = ResolutionType.Document,
            )
        }

        // Record confirmed pattern for learning
        runSuspendCatching {
            matchFeedbackStore.recordConfirmedMatch(
                tenantId = tenantId,
                counterpartyIban = transaction.counterpartyIban?.value,
                contactId = null, // Contact resolved at matching time, not available here
            )
        }

        bankingSsePublisher.publishMatchUpdated(tenantId, transactionId)
        result
    }

    suspend fun rejectMatch(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        rejectedBy: java.util.UUID?,
    ): Result<BankTransactionDto> = runSuspendCatching {
        val transaction = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")

        if (transaction.status != BankTransactionStatus.NeedsReview) {
            throw DokusException.BadRequest("Only NeedsReview transactions can be rejected")
        }

        val documentId = transaction.matchedDocumentId

        val result = updateAndRefetch(tenantId, transactionId, "Rejected match for transaction {} for tenant {}", transactionId, tenantId) {
            bankTransactionRepository.clearMatch(tenantId, transactionId)
        }

        // Record rejected pair to prevent re-matching
        if (documentId != null) {
            runSuspendCatching {
                matchFeedbackStore.recordRejectedMatch(
                    tenantId = tenantId,
                    transactionId = transactionId,
                    documentId = documentId,
                    rejectedBy = rejectedBy,
                )
            }
        }

        bankingSsePublisher.publishMatchRemoved(tenantId, transactionId)
        result
    }

    suspend fun undoMatch(
        tenantId: TenantId,
        transactionId: BankTransactionId,
    ): Result<BankTransactionDto> = runSuspendCatching {
        val transaction = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")

        if (transaction.status != BankTransactionStatus.Matched &&
            transaction.status != BankTransactionStatus.NeedsReview
        ) {
            throw DokusException.BadRequest("Only matched or needs-review transactions can be undone")
        }

        val result = updateAndRefetch(tenantId, transactionId, "Undid match for transaction {} for tenant {}", transactionId, tenantId) {
            bankTransactionRepository.clearMatch(tenantId, transactionId)
        }

        bankingSsePublisher.publishMatchRemoved(tenantId, transactionId)
        result
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun markTransfer(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        mode: tech.dokus.domain.model.MarkTransferMode,
        counterpartTransactionId: BankTransactionId?,
        destinationAccountId: tech.dokus.domain.ids.BankAccountId?,
    ): Result<BankTransactionDto> = runSuspendCatching {
        val transaction = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")

        if (transaction.status != BankTransactionStatus.Unmatched &&
            transaction.status != BankTransactionStatus.NeedsReview
        ) {
            throw DokusException.BadRequest("Transaction must be unmatched or needs review to mark as transfer")
        }

        val pairId = kotlin.uuid.Uuid.random()

        when (mode) {
            tech.dokus.domain.model.MarkTransferMode.Pair -> {
                requireNotNull(counterpartTransactionId) { "counterpartTransactionId required for PAIR mode" }
                bankTransactionRepository.markTransfer(tenantId, transactionId, pairId, MatchedBy.Manual)
                bankTransactionRepository.markTransfer(tenantId, counterpartTransactionId, pairId, MatchedBy.Manual)
                bankingSsePublisher.publishMatchUpdated(tenantId, counterpartTransactionId)
            }
            tech.dokus.domain.model.MarkTransferMode.OneSided -> {
                bankTransactionRepository.markTransfer(tenantId, transactionId, pairId, MatchedBy.Manual)
            }
        }

        bankingSsePublisher.publishMatchUpdated(tenantId, transactionId)
        logger.info("Marked transfer: {} mode={} pairId={}", transactionId, mode, pairId)

        val entity = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.InternalError("Transaction disappeared after marking transfer")
        BankTransactionDto.from(entity)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun undoTransfer(
        tenantId: TenantId,
        transactionId: BankTransactionId,
    ): Result<BankTransactionDto> = runSuspendCatching {
        val transaction = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")

        if (transaction.resolutionType != ResolutionType.Transfer) {
            throw DokusException.BadRequest("Transaction is not a transfer")
        }

        bankTransactionRepository.undoTransfer(tenantId, transactionId)

        bankingSsePublisher.publishMatchRemoved(tenantId, transactionId)
        logger.info("Undid transfer: {}", transactionId)

        val entity = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.InternalError("Transaction disappeared after undoing transfer")
        BankTransactionDto.from(entity)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun createExpenseFromTransaction(
        tenantId: TenantId,
        transactionId: BankTransactionId,
    ): Result<BankTransactionDto> = runSuspendCatching {
        val transaction = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found")

        if (transaction.status != BankTransactionStatus.Unmatched &&
            transaction.status != BankTransactionStatus.NeedsReview
        ) {
            throw DokusException.BadRequest("Transaction must be unmatched or needs review to create expense")
        }

        val expense = expenseService.createExpense(
            tenantId = tenantId,
            request = CreateExpenseRequest(
                date = transaction.transactionDate,
                merchant = transaction.counterpartyName ?: "Unknown",
                amount = Money(abs(transaction.signedAmount.minor)),
                category = ExpenseCategory.Other,
                description = transaction.descriptionRaw,
            ),
        ).getOrThrow()

        bankTransactionRepository.markMatched(
            tenantId = tenantId,
            transactionId = transactionId,
            cashflowEntryId = CashflowEntryId(expense.id.value),
            matchedBy = MatchedBy.Manual,
            resolutionType = ResolutionType.Document,
            score = 1.0,
            evidence = listOf("manual_expense_creation"),
        )

        logger.info("Created expense {} from transaction {} for tenant {}", expense.id, transactionId, tenantId)
        val entity = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found after update")
        BankTransactionDto.from(entity)
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

            // Build daily points forward: start from (current - sum of all txns in range)
            val dates = generateDateRange(startDate, today)
            val totalTxAmount = dailyAmounts.values.sum()
            var runningBalance = currentBalance - totalTxAmount
            val points = dates.map { date ->
                val dayAmount = dailyAmounts[date] ?: 0L
                runningBalance += dayAmount
                BalanceHistoryPointDto(date = date, balance = Money(runningBalance))
            }

            AccountBalanceSeriesDto(
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
            BalanceHistoryPointDto(date = date, balance = Money(totalMinor))
        }

        BalanceHistoryResponse(series = accountSeries, totalSeries = totalSeries)
    }

    private suspend fun updateAndRefetch(
        tenantId: TenantId,
        transactionId: BankTransactionId,
        logMessage: String,
        vararg logArgs: Any?,
        update: suspend () -> Boolean,
    ): BankTransactionDto {
        val updated = update()
        if (!updated) throw DokusException.NotFound("Bank transaction not found")
        logger.info(logMessage, *logArgs)
        val entity = bankTransactionRepository.findById(tenantId, transactionId)
            ?: throw DokusException.NotFound("Bank transaction not found after update")
        return BankTransactionDto.from(entity)
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

    private data class StatusCounts(
        val unmatched: Int,
        val needsReview: Int,
        val matched: Int,
        val ignored: Int,
    )

    private suspend fun getStatusCounts(tenantId: TenantId): StatusCounts {
        val raw = bankTransactionRepository.countByStatus(tenantId)
        return StatusCounts(
            unmatched = (raw[BankTransactionStatus.Unmatched] ?: 0L).toInt(),
            needsReview = (raw[BankTransactionStatus.NeedsReview] ?: 0L).toInt(),
            matched = (raw[BankTransactionStatus.Matched] ?: 0L).toInt(),
            ignored = (raw[BankTransactionStatus.Ignored] ?: 0L).toInt(),
        )
    }
}
