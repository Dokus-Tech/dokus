package tech.dokus.database.tables.peppol

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable

/**
 * Peppol settings per tenant - stores provider credentials.
 * CRITICAL: API credentials are encrypted at rest.
 *
 * Supports multiple providers (Recommand, Storecove, etc.)
 */
object PeppolSettingsTable : UUIDTable("peppol_settings") {
    // Multi-tenancy (CRITICAL) - one settings record per tenant
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // Provider identification - supports multiple providers
    val providerId = varchar("provider_id", 50).default("recommand").index()

    // Provider-specific configuration (JSON) for future extensibility
    val providerConfig = text("provider_config").nullable()

    // Common API configuration (used by Recommand and similar providers)
    val companyId = varchar("company_id", 255)
    val apiKey = varchar("api_key", 512) // Encrypted
    val apiSecret = varchar("api_secret", 512) // Encrypted

    // Tenant's Peppol participant ID (format: scheme:identifier)
    val peppolId = varchar("peppol_id", 255)

    // Feature flags
    val isEnabled = bool("is_enabled").default(false)
    val testMode = bool("test_mode").default(true)

    // Webhook configuration
    val webhookToken = varchar("webhook_token", 64).nullable().uniqueIndex()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, providerId)
    }
}
