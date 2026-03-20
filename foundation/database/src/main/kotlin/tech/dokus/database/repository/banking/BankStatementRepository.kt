package tech.dokus.database.repository.banking

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import tech.dokus.database.entity.BankStatementEntity
import tech.dokus.database.mapper.from
import tech.dokus.database.tables.banking.BankStatementsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankTransactionSource
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.toDbDecimal
import tech.dokus.foundation.backend.database.dbQuery
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

@OptIn(ExperimentalUuidApi::class)
class BankStatementRepository {

    suspend fun findByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId,
    ): BankStatementEntity? = dbQuery {
        BankStatementsTable.selectAll().where {
            (BankStatementsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankStatementsTable.documentId eq documentId.value.toJavaUuid())
        }.firstOrNull()?.let { BankStatementEntity.from(it) }
    }

    /**
     * Find a statement by file hash (strong dedup).
     */
    suspend fun findByFileHash(tenantId: TenantId, fileHash: String): BankStatementEntity? = dbQuery {
        BankStatementsTable.selectAll().where {
            (BankStatementsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankStatementsTable.fileHash eq fileHash)
        }.firstOrNull()?.let { BankStatementEntity.from(it) }
    }

    /**
     * Find statements by IBAN + period end (weak dedup).
     */
    suspend fun findByIbanAndPeriod(
        tenantId: TenantId,
        accountIban: Iban,
        periodEnd: LocalDate,
    ): List<BankStatementEntity> = dbQuery {
        BankStatementsTable.selectAll().where {
            (BankStatementsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankStatementsTable.accountIban eq accountIban.value) and
                (BankStatementsTable.periodEnd eq periodEnd)
        }.map { BankStatementEntity.from(it) }
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

    suspend fun deleteByDocumentId(
        tenantId: TenantId,
        documentId: DocumentId,
    ): Int = dbQuery {
        BankStatementsTable.deleteWhere {
            (BankStatementsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankStatementsTable.documentId eq documentId.value.toJavaUuid())
        }
    }

}
