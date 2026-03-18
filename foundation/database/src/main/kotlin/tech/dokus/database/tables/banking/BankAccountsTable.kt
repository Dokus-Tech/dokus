package tech.dokus.database.tables.banking

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.BankAccountProvider
import tech.dokus.domain.enums.BankAccountStatus
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.Currency
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Bank accounts — IBAN-canonical, auto-created from statements or live-sync providers.
 */
object BankAccountsTable : UUIDTable("bank_accounts") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    val iban = varchar("iban", 34).nullable()
    val name = varchar("name", 255)
    val institutionName = varchar("institution_name", 255)
    val parentAccountId = uuid("parent_account_id").nullable()
    val providerAccountId = varchar("provider_account_id", 255).nullable()
    val accountType = dbEnumeration<BankAccountType>("account_type")
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)
    val provider = dbEnumeration<BankAccountProvider>("provider").default(BankAccountProvider.Unknown)
    val balance = decimal("balance", 12, 2).nullable()
    val balanceUpdatedAt = datetime("balance_updated_at").nullable()
    val status = dbEnumeration<BankAccountStatus>("status").default(BankAccountStatus.Confirmed)
    val isActive = bool("is_active").default(true)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, isActive)
        index(false, tenantId, iban)
        // Note: IBAN uniqueness is enforced at application level via findByIban()
        // because pockets/sub-accounts may have null IBANs.
    }
}
