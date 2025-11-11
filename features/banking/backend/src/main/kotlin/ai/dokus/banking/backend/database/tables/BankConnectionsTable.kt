package ai.dokus.banking.backend.database.tables

import ai.dokus.foundation.ktor.database.dbEnumeration
import ai.dokus.foundation.domain.enums.BankAccountType
import ai.dokus.foundation.domain.enums.BankProvider
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import java.util.UUID as JavaUUID

/**
 * Bank connections via Plaid/Tink/Nordigen
 * Encrypted access tokens for transaction syncing
 */
object BankConnectionsTable : UUIDTable("bank_connections") {
    val tenantId = uuid("tenant_id")

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
        index(false, tenantId)
        index(false, tenantId, isActive)
    }
}
