package tech.dokus.database.tables.banking

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.BankAccountType
import tech.dokus.domain.enums.BankProvider
import tech.dokus.domain.enums.Currency
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Bank connections via Plaid/Tink/Nordigen
 * Encrypted access tokens for transaction syncing
 */
object BankConnectionsTable : UUIDTable("bank_connections") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    val provider = dbEnumeration<BankProvider>("provider")
    val institutionId = varchar("institution_id", 255)
    val institutionName = varchar("institution_name", 255)
    val accountId = varchar("account_id", 255)
    val accountName = varchar("account_name", 255).nullable()
    val accountType = dbEnumeration<BankAccountType>("account_type").nullable()
    val currency = dbEnumeration<Currency>("currency").default(Currency.Eur)

    // Encrypted access token
    val accessToken = text("access_token")

    val lastSyncedAt = datetime("last_synced_at").nullable()
    val isActive = bool("is_active").default(true)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId, isActive)
        // Prevent duplicate external accounts per tenant/provider
        uniqueIndex(tenantId, provider, accountId)
    }
}
