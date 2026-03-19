package tech.dokus.database.repository.banking

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import tech.dokus.database.entity.BankAccountEntity
import tech.dokus.database.mapper.from
import tech.dokus.database.tables.banking.BankAccountsTable
import tech.dokus.domain.enums.BankAccountProvider
import tech.dokus.domain.enums.BankAccountStatus
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.ids.BankAccountId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

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
        iban: Iban? = null,
        name: String,
        institutionName: String,
        accountType: BankAccountType,
        currency: Currency,
        provider: BankAccountProvider,
        status: BankAccountStatus = BankAccountStatus.Confirmed,
        parentAccountId: BankAccountId? = null,
        providerAccountId: String? = null,
    ): BankAccountEntity = newSuspendedTransaction {
        val id = BankAccountsTable.insert {
            it[BankAccountsTable.tenantId] = tenantId.value.toJavaUuid()
            it[BankAccountsTable.iban] = iban?.value
            it[BankAccountsTable.name] = name
            it[BankAccountsTable.institutionName] = institutionName
            it[BankAccountsTable.accountType] = accountType
            it[BankAccountsTable.currency] = currency
            it[BankAccountsTable.provider] = provider
            it[BankAccountsTable.status] = status
            it[BankAccountsTable.parentAccountId] = parentAccountId?.value?.toJavaUuid()
            it[BankAccountsTable.providerAccountId] = providerAccountId
        } get BankAccountsTable.id

        BankAccountsTable.selectAll().where {
            BankAccountsTable.id eq id.value
        }.single().let { BankAccountEntity.from(it) }
    }

    suspend fun listAccounts(
        tenantId: TenantId,
        activeOnly: Boolean = true,
    ): List<BankAccountEntity> = newSuspendedTransaction {
        var query = BankAccountsTable.selectAll().where {
            BankAccountsTable.tenantId eq tenantId.value.toJavaUuid()
        }
        if (activeOnly) {
            query = query.andWhere { BankAccountsTable.isActive eq true }
        }
        query.orderBy(BankAccountsTable.createdAt, SortOrder.DESC)
            .map { BankAccountEntity.from(it) }
    }

    suspend fun findByIban(
        tenantId: TenantId,
        iban: Iban,
    ): BankAccountEntity? = newSuspendedTransaction {
        BankAccountsTable.selectAll().where {
            (BankAccountsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankAccountsTable.iban eq iban.value)
        }.singleOrNull()?.let { BankAccountEntity.from(it) }
    }

    suspend fun findByProviderAccountId(
        tenantId: TenantId,
        providerAccountId: String,
    ): BankAccountEntity? = newSuspendedTransaction {
        BankAccountsTable.selectAll().where {
            (BankAccountsTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BankAccountsTable.providerAccountId eq providerAccountId)
        }.singleOrNull()?.let { BankAccountEntity.from(it) }
    }
}
