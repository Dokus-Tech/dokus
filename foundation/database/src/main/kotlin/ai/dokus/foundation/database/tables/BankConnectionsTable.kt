package ai.dokus.foundation.database.tables

import ai.dokus.foundation.database.*
import ai.dokus.foundation.domain.enums.BankAccountType
import ai.dokus.foundation.domain.enums.BankProvider
import ai.dokus.foundation.domain.enums.Currency
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Connected bank accounts via Plaid/Tink/Nordigen
 * Enable automatic transaction import
 */
object BankConnectionsTable : UUIDTable("bank_connections") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)

    // Provider
    val provider = bankProviderEnumeration("provider")
    val institutionId = varchar("institution_id", 100)
    val institutionName = varchar("institution_name", 255)

    // Account
    val accountId = varchar("account_id", 255)
    val accountName = varchar("account_name", 255).nullable()
    val accountType = bankAccountTypeEnumeration("account_type").nullable()
    val currency = currencyEnumeration("currency").default(Currency.EUR)

    // CRITICAL: Must be encrypted at rest
    val accessToken = text("access_token")  // AES-256-GCM encrypted

    val lastSyncedAt = datetime("last_synced_at").nullable()
    val isActive = bool("is_active").default(true)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, tenantId, isActive)
    }
}