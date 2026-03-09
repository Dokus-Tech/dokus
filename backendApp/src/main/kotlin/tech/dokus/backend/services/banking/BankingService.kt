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
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankConnectionDto
import tech.dokus.domain.model.BankTransactionDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.foundation.backend.utils.loggerFor
import tech.dokus.foundation.backend.utils.runSuspendCatching
import kotlinx.datetime.LocalDate

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
        val unresolvedMinor = bankTransactionRepository.sumUnresolved(tenantId)

        BankAccountSummary(
            totalBalance = Money(0), // Balance tracking not yet implemented
            accountCount = connections.size,
            unmatchedCount = unmatchedCount + suggestedCount,
            totalUnresolvedAmount = Money(unresolvedMinor)
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
}
