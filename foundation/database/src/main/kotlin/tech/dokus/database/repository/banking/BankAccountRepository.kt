package tech.dokus.database.repository.banking

import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.banking.BankAccountsTable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.BankAccountProvider
import tech.dokus.domain.enums.BankAccountStatus
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.toDbDecimal
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Repository for managing bank accounts.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 */
@OptIn(ExperimentalUuidApi::class)
class BankAccountRepository {

    suspend fun createAccount(
        tenantId: TenantId,
        iban: Iban,
        name: String,
        institutionName: String,
        accountType: BankAccountType,
        currency: Currency,
        provider: BankAccountProvider,
        status: BankAccountStatus = BankAccountStatus.Confirmed,
    ): BankAccountDto = newSuspendedTransaction {
        val id = BankAccountsTable.insert {
            it[BankAccountsTable.tenantId] = tenantId.value.toJavaUuid()
            it[BankAccountsTable.iban] = iban.value
            it[BankAccountsTable.name] = name
            it[BankAccountsTable.institutionName] = institutionName
            it[BankAccountsTable.accountType] = accountType
            it[BankAccountsTable.currency] = currency
            it[BankAccountsTable.provider] = provider
            it[BankAccountsTable.status] = status
        } get BankAccountsTable.id

        BankAccountsTable.selectAll().where {
            BankAccountsTable.id eq id.value
        }.single().toBankAccountDto()
    }

    suspend fun getAccount(
        tenantId: TenantId,
        accountId: BankAccountId,
    ): BankAccountDto? = newSuspendedTransaction {
        BankAccountsTable.selectAll().where {
            (BankAccountsTable.id eq accountId.value.toJavaUuid()) and
                (BankAccountsTable.tenantId eq tenantId.value.toJavaUuid())
        }.singleOrNull()?.toBankAccountDto()
    }

    suspend fun listAccounts(
        tenantId: TenantId,
        activeOnly: Boolean = true,
    ): List<BankAccountDto> = newSuspendedTransaction {
        var query = BankAccountsTable.selectAll().where {
            BankAccountsTable.tenantId eq tenantId.value.toJavaUuid()
        }
        if (activeOnly) {
            query = query.andWhere { BankAccountsTable.isActive eq true }
        }
        query.orderBy(BankAccountsTable.createdAt, SortOrder.DESC)
            .map { it.toBankAccountDto() }
    }

    suspend fun findByIban(
        tenantId: TenantId,
        iban: Iban,
    ): BankAccountDto? = newSuspendedTransaction {
        BankAccountsTable.selectAll().where {
            (BankAccountsTable.tenantId eq tenantId.value.toJavaUuid()) and
                (BankAccountsTable.iban eq iban.value)
        }.singleOrNull()?.toBankAccountDto()
    }

    suspend fun updateBalance(
        tenantId: TenantId,
        accountId: BankAccountId,
        balance: Money,
        balanceUpdatedAt: LocalDateTime,
    ): Boolean = newSuspendedTransaction {
        BankAccountsTable.update({
            (BankAccountsTable.id eq accountId.value.toJavaUuid()) and
                (BankAccountsTable.tenantId eq tenantId.value.toJavaUuid())
        }) {
            it[BankAccountsTable.balance] = balance.toDbDecimal()
            it[BankAccountsTable.balanceUpdatedAt] = balanceUpdatedAt
        } > 0
    }

    suspend fun updateStatus(
        tenantId: TenantId,
        accountId: BankAccountId,
        status: BankAccountStatus,
    ): Boolean = newSuspendedTransaction {
        BankAccountsTable.update({
            (BankAccountsTable.id eq accountId.value.toJavaUuid()) and
                (BankAccountsTable.tenantId eq tenantId.value.toJavaUuid())
        }) {
            it[BankAccountsTable.status] = status
        } > 0
    }

    suspend fun deactivateAccount(
        tenantId: TenantId,
        accountId: BankAccountId,
    ): Boolean = newSuspendedTransaction {
        BankAccountsTable.update({
            (BankAccountsTable.id eq accountId.value.toJavaUuid()) and
                (BankAccountsTable.tenantId eq tenantId.value.toJavaUuid())
        }) {
            it[isActive] = false
        } > 0
    }

    private fun ResultRow.toBankAccountDto(): BankAccountDto {
        return BankAccountDto(
            id = BankAccountId(this[BankAccountsTable.id].value.toKotlinUuid()),
            tenantId = TenantId(this[BankAccountsTable.tenantId].toKotlinUuid()),
            iban = Iban(this[BankAccountsTable.iban]),
            name = this[BankAccountsTable.name],
            institutionName = this[BankAccountsTable.institutionName],
            accountType = this[BankAccountsTable.accountType],
            currency = this[BankAccountsTable.currency],
            provider = this[BankAccountsTable.provider],
            balance = this[BankAccountsTable.balance]?.let { Money.fromDbDecimal(it) },
            balanceUpdatedAt = this[BankAccountsTable.balanceUpdatedAt],
            status = this[BankAccountsTable.status],
            isActive = this[BankAccountsTable.isActive],
            createdAt = this[BankAccountsTable.createdAt],
        )
    }
}
