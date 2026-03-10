package tech.dokus.database.repository.banking

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import tech.dokus.database.tables.banking.BankStatementsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
data class BankStatementRecord(
    val id: kotlin.uuid.Uuid,
    val tenantId: TenantId,
    val bankAccountId: BankAccountId?,
    val documentId: DocumentId?,
    val source: BankTransactionSource,
    val statementTrust: StatementTrust,
    val fileHash: String?,
    val accountIban: Iban?,
    val periodStart: LocalDate?,
    val periodEnd: LocalDate?,
    val openingBalance: Money?,
    val closingBalance: Money?,
    val transactionCount: Int,
)

@OptIn(ExperimentalUuidApi::class)
class BankStatementRepository {

    /**
     * Find a statement by file hash (strong dedup).
     */
    suspend fun findByFileHash(tenantId: TenantId, fileHash: String): BankStatementRecord? = dbQuery {
        BankStatementsTable.selectAll().where {
            (BankStatementsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankStatementsTable.fileHash eq fileHash)
        }.firstOrNull()?.let(::toRecord)
    }

    /**
     * Find statements by IBAN + period end (weak dedup).
     */
    suspend fun findByIbanAndPeriod(
        tenantId: TenantId,
        accountIban: Iban,
        periodEnd: LocalDate,
    ): List<BankStatementRecord> = dbQuery {
        BankStatementsTable.selectAll().where {
            (BankStatementsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankStatementsTable.accountIban eq accountIban.value) and
                (BankStatementsTable.periodEnd eq periodEnd)
        }.map(::toRecord)
    }

    /**
     * Create a new statement record.
     */
    suspend fun create(
        tenantId: TenantId,
        bankAccountId: BankAccountId?,
        documentId: DocumentId?,
        source: BankTransactionSource,
        statementTrust: StatementTrust,
        fileHash: String?,
        accountIban: Iban?,
        periodStart: LocalDate?,
        periodEnd: LocalDate?,
        openingBalance: Money?,
        closingBalance: Money?,
        transactionCount: Int,
    ): Unit = dbQuery {
        BankStatementsTable.insert {
            it[BankStatementsTable.tenantId] = tenantId.value.toJavaUuid()
            it[BankStatementsTable.bankAccountId] = bankAccountId?.value?.toJavaUuid()
            it[BankStatementsTable.documentId] = documentId?.value?.toJavaUuid()
            it[BankStatementsTable.statementSource] = source
            it[BankStatementsTable.statementTrust] = statementTrust
            it[BankStatementsTable.fileHash] = fileHash
            it[BankStatementsTable.accountIban] = accountIban?.value
            it[BankStatementsTable.periodStart] = periodStart
            it[BankStatementsTable.periodEnd] = periodEnd
            it[BankStatementsTable.openingBalance] = openingBalance?.toDbDecimal()
            it[BankStatementsTable.closingBalance] = closingBalance?.toDbDecimal()
            it[BankStatementsTable.transactionCount] = transactionCount
        }
    }

    private fun toRecord(row: org.jetbrains.exposed.v1.core.ResultRow): BankStatementRecord {
        return BankStatementRecord(
            id = row[BankStatementsTable.id].value.toKotlinUuid(),
            tenantId = TenantId(row[BankStatementsTable.tenantId].toKotlinUuid()),
            bankAccountId = row[BankStatementsTable.bankAccountId]?.toKotlinUuid()?.let(::BankAccountId),
            documentId = row[BankStatementsTable.documentId]?.toKotlinUuid()?.let(::DocumentId),
            source = row[BankStatementsTable.statementSource],
            statementTrust = row[BankStatementsTable.statementTrust],
            fileHash = row[BankStatementsTable.fileHash],
            accountIban = row[BankStatementsTable.accountIban]?.let(::Iban),
            periodStart = row[BankStatementsTable.periodStart],
            periodEnd = row[BankStatementsTable.periodEnd],
            openingBalance = row[BankStatementsTable.openingBalance]?.let { Money.fromDbDecimal(it) },
            closingBalance = row[BankStatementsTable.closingBalance]?.let { Money.fromDbDecimal(it) },
            transactionCount = row[BankStatementsTable.transactionCount],
        )
    }
}
