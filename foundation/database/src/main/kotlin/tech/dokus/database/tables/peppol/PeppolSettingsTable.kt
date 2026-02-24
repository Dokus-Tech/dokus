package tech.dokus.database.tables.peppol

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable

/**
 * Peppol settings per tenant.
 * Credentials are managed via environment variables (PEPPOL_MASTER_API_KEY/SECRET),
 * not stored in the database.
 */
object PeppolSettingsTable : UUIDTable("peppol_settings") {
    // Multi-tenancy (CRITICAL) - one settings record per tenant
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    )

    // Provider identification - currently only "recommand" supported
    val providerId = varchar("provider_id", 50).default("recommand").index()

    // Provider-specific configuration (JSON) for future extensibility
    val providerConfig = text("provider_config").nullable()

    // Company ID at the Peppol provider (e.g., Recommand company ID)
    val companyId = varchar("company_id", 255)

    // Tenant's Peppol participant ID (format: scheme:identifier)
    val peppolId = varchar("peppol_id", 255)

    // Feature flags
    val isEnabled = bool("is_enabled").default(false)
    val testMode = bool("test_mode").default(true)

    // Webhook configuration
    val webhookToken = varchar("webhook_token", 64).nullable().uniqueIndex()
    val lastWebhookPollTriggeredAt = datetime("last_webhook_poll_triggered_at").nullable()

    // Sync tracking - used for initial sync and weekly full sync
    val lastFullSyncAt = datetime("last_full_sync_at").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(tenantId, providerId)
        index(false, isEnabled, companyId)
    }
}
