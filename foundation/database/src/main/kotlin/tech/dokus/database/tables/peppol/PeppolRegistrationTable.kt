package tech.dokus.database.tables.peppol

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.enums.PeppolRegistrationStatus

/**
 * PEPPOL registration state per tenant.
 *
 * This table tracks the registration lifecycle for PEPPOL e-invoicing.
 * Credentials are managed via environment variables (PEPPOL_MASTER_API_KEY/SECRET).
 *
 * One registration per tenant - uniquely indexed on tenantId.
 */
object PeppolRegistrationTable : UUIDTable("peppol_registrations") {
    // Multi-tenancy (CRITICAL) - one registration per tenant
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).uniqueIndex()

    // Tenant's PEPPOL participant ID (format: scheme:identifier, e.g., "0208:BE0123456789")
    val peppolId = varchar("peppol_id", 50)

    // Recommand company ID (null until registered with Recommand)
    val recommandCompanyId = varchar("recommand_company_id", 100).nullable()

    // Registration state machine status
    val status = enumerationByName<PeppolRegistrationStatus>("status", 30)
        .default(PeppolRegistrationStatus.NotConfigured)

    // Capability flags
    val canReceive = bool("can_receive").default(false)
    val canSend = bool("can_send").default(false)

    // Test mode flag
    val testMode = bool("test_mode").default(false)

    // Transfer waiting tracking
    val waitingSince = datetime("waiting_since").nullable()
    val lastPolledAt = datetime("last_polled_at").nullable()

    // Error information
    val errorMessage = text("error_message").nullable()

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
