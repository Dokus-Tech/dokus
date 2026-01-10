package tech.dokus.database.tables.peppol

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.enums.PeppolLookupSource
import tech.dokus.domain.enums.PeppolLookupStatus
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Cache table for PEPPOL directory lookups.
 * Stores results of Recommand directory searches to avoid repeated API calls.
 *
 * Staleness detection:
 * - Cache is stale if expiresAt is in the past
 * - Cache is stale if vatNumberSnapshot != contact's current vatNumber
 * - Cache is stale if companyNumberSnapshot != contact's current companyNumber
 *
 * TTL policy:
 * - Found/NotFound: 14 days
 * - Error: 1 day
 * - Manual entries: never expire (expiresAt = null)
 *
 * CRITICAL: All queries MUST filter by tenant_id
 */
object PeppolDirectoryCacheTable : UUIDTable("peppol_directory_cache") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Contact reference
    val contactId = uuid("contact_id").references(
        ContactsTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Snapshot of identifiers used for lookup (for staleness detection)
    val vatNumberSnapshot = varchar("vat_number_snapshot", 50).nullable()
    val companyNumberSnapshot = varchar("company_number_snapshot", 50).nullable()

    // Discovery result
    val participantId = varchar("participant_id", 255).nullable() // e.g., "0208:0123456789"
    val scheme = varchar("scheme", 10).nullable() // e.g., "0208"
    val supportedDocTypes = text("supported_doc_types").nullable() // JSON array

    // Status & source
    val status = dbEnumeration<PeppolLookupStatus>("status")
    val lookupSource = dbEnumeration<PeppolLookupSource>("source").default(PeppolLookupSource.Directory)

    // TTL management
    val lastCheckedAt = datetime("last_checked_at").defaultExpression(CurrentDateTime)
    val expiresAt = datetime("expires_at").nullable() // null = never expires (manual entry)

    // Error tracking
    val errorMessage = text("error_message").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // One cache entry per contact per tenant
        uniqueIndex(tenantId, contactId)
        // For cleanup queries (expired entries)
        index(false, tenantId, expiresAt)
    }
}
