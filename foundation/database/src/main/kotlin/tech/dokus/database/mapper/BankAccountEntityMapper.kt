package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.entity.BankAccountEntity
import tech.dokus.database.tables.banking.BankAccountsTable
import tech.dokus.domain.Money
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
fun BankAccountEntity.Companion.from(row: ResultRow): BankAccountEntity {
    val currency = row[BankAccountsTable.currency]
    return BankAccountEntity(
    id = BankAccountId(row[BankAccountsTable.id].value.toKotlinUuid()),
    tenantId = TenantId(row[BankAccountsTable.tenantId].toKotlinUuid()),
    iban = row[BankAccountsTable.iban]?.let { Iban(it) },
    name = row[BankAccountsTable.name],
    institutionName = row[BankAccountsTable.institutionName],
    accountType = row[BankAccountsTable.accountType],
    currency = currency,
    provider = row[BankAccountsTable.provider],
    balance = row[BankAccountsTable.balance]?.let { Money.fromDbDecimal(it, currency) },
    balanceUpdatedAt = row[BankAccountsTable.balanceUpdatedAt],
    status = row[BankAccountsTable.status],
    isActive = row[BankAccountsTable.isActive],
    createdAt = row[BankAccountsTable.createdAt],
    parentAccountId = row[BankAccountsTable.parentAccountId]?.toKotlinUuid()
        ?.let { BankAccountId(it) },
    providerAccountId = row[BankAccountsTable.providerAccountId],
)
}
