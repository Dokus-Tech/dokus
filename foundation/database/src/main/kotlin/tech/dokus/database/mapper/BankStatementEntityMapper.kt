package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.BankStatementEntity
import tech.dokus.database.tables.banking.BankStatementsTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
fun BankStatementEntity.Companion.from(row: ResultRow): BankStatementEntity = BankStatementEntity(
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
    createdAt = row[BankStatementsTable.createdAt],
)
