package tech.dokus.database.tables.enrichment

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.EnrichmentEntityType
import tech.dokus.domain.enums.EnrichmentStatus
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Business descriptions table - stores AI-enriched business information
 * for both tenants and contacts.
 *
 * Uses a polymorphic reference pattern: entity_type + entity_id
 * to link to either a tenant or a contact.
 *
 * CRITICAL: All queries MUST filter by tenant_id
 */
object BusinessDescriptionsTable : UUIDTable("business_descriptions") {
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    val entityType = dbEnumeration<EnrichmentEntityType>("entity_type")
    val entityId = uuid("entity_id")

    val websiteUrl = varchar("website_url", 500).nullable()
    val summary = text("summary").nullable()
    val activities = text("activities").nullable()

    val enrichmentStatus = dbEnumeration<EnrichmentStatus>("enrichment_status")
        .default(EnrichmentStatus.Pending)

    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        uniqueIndex(entityType, entityId)
        index(false, tenantId, entityType)
    }
}
