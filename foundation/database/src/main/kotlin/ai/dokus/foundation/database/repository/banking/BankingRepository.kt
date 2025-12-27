package ai.dokus.foundation.database.repository.banking

import ai.dokus.foundation.database.tables.banking.BankConnectionsTable
import ai.dokus.foundation.database.tables.banking.BankTransactionsTable
import tech.dokus.domain.Money
import ai.dokus.foundation.domain.enums.BankAccountType
import ai.dokus.foundation.domain.enums.BankProvider
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.ids.BankConnectionId
import ai.dokus.foundation.domain.ids.BankTransactionId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.BankConnectionDto
import ai.dokus.foundation.domain.model.BankTransactionDto
import tech.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * Repository for managing bank connections and transactions.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. Access tokens must be encrypted before storage
 * 3. Never expose raw access tokens in responses
 */
@OptIn(ExperimentalUuidApi::class)
class BankingRepository {

    // ========================================================================
    // BANK CONNECTIONS
    // ========================================================================

    /**
     * Create a new bank connection.
     * CRITICAL: Access token should be encrypted before passing to this method
     */
    suspend fun createConnection(
        tenantId: TenantId,
        provider: BankProvider,
        institutionId: String,
        institutionName: String,
        accountId: String,
        accountName: String?,
        accountType: BankAccountType?,
        currency: Currency,
        encryptedAccessToken: String
    ): Result<BankConnectionDto> = runCatching {
        dbQuery {
            val id = BankConnectionsTable.insert {
                it[BankConnectionsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[BankConnectionsTable.provider] = provider
                it[BankConnectionsTable.institutionId] = institutionId
                it[BankConnectionsTable.institutionName] = institutionName
                it[BankConnectionsTable.accountId] = accountId
                it[BankConnectionsTable.accountName] = accountName
                it[BankConnectionsTable.accountType] = accountType
                it[BankConnectionsTable.currency] = currency
                it[BankConnectionsTable.accessToken] = encryptedAccessToken
            } get BankConnectionsTable.id

            BankConnectionsTable.selectAll().where {
                BankConnectionsTable.id eq id.value
            }.single().toBankConnectionDto()
        }
    }

    /**
     * Get a bank connection by ID.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getConnection(
        connectionId: BankConnectionId,
        tenantId: TenantId
    ): Result<BankConnectionDto?> = runCatching {
        dbQuery {
            BankConnectionsTable.selectAll().where {
                (BankConnectionsTable.id eq UUID.fromString(connectionId.toString())) and
                (BankConnectionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.toBankConnectionDto()
        }
    }

    /**
     * List bank connections for a tenant.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listConnections(
        tenantId: TenantId,
        activeOnly: Boolean = true
    ): Result<List<BankConnectionDto>> = runCatching {
        dbQuery {
            var query = BankConnectionsTable.selectAll().where {
                BankConnectionsTable.tenantId eq UUID.fromString(tenantId.toString())
            }
            if (activeOnly) {
                query = query.andWhere { BankConnectionsTable.isActive eq true }
            }
            query.orderBy(BankConnectionsTable.createdAt, SortOrder.DESC)
                .map { it.toBankConnectionDto() }
        }
    }

    /**
     * Update last synced time for a connection.
     */
    suspend fun updateLastSyncedAt(
        connectionId: BankConnectionId,
        tenantId: TenantId,
        syncedAt: LocalDateTime
    ): Result<Boolean> = runCatching {
        dbQuery {
            BankConnectionsTable.update({
                (BankConnectionsTable.id eq UUID.fromString(connectionId.toString())) and
                (BankConnectionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[lastSyncedAt] = syncedAt
                it[updatedAt] = syncedAt
            } > 0
        }
    }

    /**
     * Deactivate a bank connection (soft delete).
     */
    suspend fun deactivateConnection(
        connectionId: BankConnectionId,
        tenantId: TenantId
    ): Result<Boolean> = runCatching {
        dbQuery {
            BankConnectionsTable.update({
                (BankConnectionsTable.id eq UUID.fromString(connectionId.toString())) and
                (BankConnectionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[isActive] = false
            } > 0
        }
    }

    // ========================================================================
    // BANK TRANSACTIONS
    // ========================================================================

    /**
     * Create a bank transaction.
     */
    suspend fun createTransaction(
        bankConnectionId: BankConnectionId,
        tenantId: TenantId,
        externalId: String,
        date: LocalDate,
        amount: Money,
        description: String,
        merchantName: String?,
        category: String?,
        isPending: Boolean
    ): Result<BankTransactionDto> = runCatching {
        dbQuery {
            val id = BankTransactionsTable.insert {
                it[BankTransactionsTable.bankConnectionId] = UUID.fromString(bankConnectionId.toString())
                it[BankTransactionsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[BankTransactionsTable.externalId] = externalId
                it[BankTransactionsTable.date] = date
                it[BankTransactionsTable.amount] = java.math.BigDecimal(amount.value)
                it[BankTransactionsTable.description] = description
                it[BankTransactionsTable.merchantName] = merchantName
                it[BankTransactionsTable.category] = category
                it[BankTransactionsTable.isPending] = isPending
            } get BankTransactionsTable.id

            BankTransactionsTable.selectAll().where {
                BankTransactionsTable.id eq id.value
            }.single().toBankTransactionDto()
        }
    }

    /**
     * Get a bank transaction by ID.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun getTransaction(
        transactionId: BankTransactionId,
        tenantId: TenantId
    ): Result<BankTransactionDto?> = runCatching {
        dbQuery {
            BankTransactionsTable.selectAll().where {
                (BankTransactionsTable.id eq UUID.fromString(transactionId.toString())) and
                (BankTransactionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }.singleOrNull()?.toBankTransactionDto()
        }
    }

    /**
     * List transactions for a tenant with filters.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listTransactions(
        tenantId: TenantId,
        connectionId: BankConnectionId? = null,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
        reconciled: Boolean? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<BankTransactionDto>> = runCatching {
        dbQuery {
            var query = BankTransactionsTable.selectAll().where {
                BankTransactionsTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            connectionId?.let {
                query = query.andWhere {
                    BankTransactionsTable.bankConnectionId eq UUID.fromString(it.toString())
                }
            }
            fromDate?.let {
                query = query.andWhere { BankTransactionsTable.date greaterEq it }
            }
            toDate?.let {
                query = query.andWhere { BankTransactionsTable.date lessEq it }
            }
            reconciled?.let {
                query = query.andWhere { BankTransactionsTable.isReconciled eq it }
            }

            query.orderBy(BankTransactionsTable.date, SortOrder.DESC)
                .limit(limit)
                .offset(offset.toLong())
                .map { it.toBankTransactionDto() }
        }
    }

    /**
     * Reconcile a transaction with an expense.
     */
    suspend fun reconcileWithExpense(
        transactionId: BankTransactionId,
        tenantId: TenantId,
        expenseId: ExpenseId
    ): Result<Boolean> = runCatching {
        dbQuery {
            BankTransactionsTable.update({
                (BankTransactionsTable.id eq UUID.fromString(transactionId.toString())) and
                (BankTransactionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[BankTransactionsTable.expenseId] = UUID.fromString(expenseId.toString())
                it[isReconciled] = true
            } > 0
        }
    }

    /**
     * Reconcile a transaction with an invoice.
     */
    suspend fun reconcileWithInvoice(
        transactionId: BankTransactionId,
        tenantId: TenantId,
        invoiceId: InvoiceId
    ): Result<Boolean> = runCatching {
        dbQuery {
            BankTransactionsTable.update({
                (BankTransactionsTable.id eq UUID.fromString(transactionId.toString())) and
                (BankTransactionsTable.tenantId eq UUID.fromString(tenantId.toString()))
            }) {
                it[BankTransactionsTable.invoiceId] = UUID.fromString(invoiceId.toString())
                it[isReconciled] = true
            } > 0
        }
    }

    // ========================================================================
    // MAPPERS
    // ========================================================================

    private fun ResultRow.toBankConnectionDto(): BankConnectionDto {
        return BankConnectionDto(
            id = BankConnectionId.parse(this[BankConnectionsTable.id].value.toString()),
            tenantId = TenantId.parse(this[BankConnectionsTable.tenantId].toString()),
            provider = this[BankConnectionsTable.provider],
            institutionId = this[BankConnectionsTable.institutionId],
            institutionName = this[BankConnectionsTable.institutionName],
            accountId = this[BankConnectionsTable.accountId],
            accountName = this[BankConnectionsTable.accountName],
            accountType = this[BankConnectionsTable.accountType],
            currency = this[BankConnectionsTable.currency],
            lastSyncedAt = this[BankConnectionsTable.lastSyncedAt],
            isActive = this[BankConnectionsTable.isActive],
            createdAt = this[BankConnectionsTable.createdAt],
            updatedAt = this[BankConnectionsTable.updatedAt]
        )
    }

    private fun ResultRow.toBankTransactionDto(): BankTransactionDto {
        return BankTransactionDto(
            id = BankTransactionId.parse(this[BankTransactionsTable.id].value.toString()),
            bankConnectionId = BankConnectionId.parse(this[BankTransactionsTable.bankConnectionId].toString()),
            tenantId = TenantId.parse(this[BankTransactionsTable.tenantId].toString()),
            externalId = this[BankTransactionsTable.externalId],
            date = this[BankTransactionsTable.date],
            amount = Money(this[BankTransactionsTable.amount].toString()),
            description = this[BankTransactionsTable.description],
            merchantName = this[BankTransactionsTable.merchantName],
            category = this[BankTransactionsTable.category],
            isPending = this[BankTransactionsTable.isPending],
            expenseId = this[BankTransactionsTable.expenseId]?.let { ExpenseId.parse(it.toString()) },
            invoiceId = this[BankTransactionsTable.invoiceId]?.let { InvoiceId.parse(it.toString()) },
            isReconciled = this[BankTransactionsTable.isReconciled],
            createdAt = this[BankTransactionsTable.createdAt]
        )
    }
}
