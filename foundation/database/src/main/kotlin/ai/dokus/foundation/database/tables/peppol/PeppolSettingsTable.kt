package ai.dokus.foundation.database.tables.peppol

import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Peppol settings per tenant - stores Recommand API credentials.
 * CRITICAL: API credentials are encrypted at rest.
 */
object PeppolSettingsTable : UUIDTable("peppol_settings") {
    // Multi-tenancy (CRITICAL) - one settings record per tenant
    val tenantId = uuid("tenant_id").uniqueIndex()

    // Recommand API configuration
    val companyId = varchar("company_id", 255)
    val apiKey = varchar("api_key", 512)  // Encrypted
    val apiSecret = varchar("api_secret", 512)  // Encrypted

    // Tenant's Peppol participant ID (format: scheme:identifier)
    val peppolId = varchar("peppol_id", 255)

    // Feature flags
    val isEnabled = bool("is_enabled").default(false)
    val testMode = bool("test_mode").default(true)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
    }
}
