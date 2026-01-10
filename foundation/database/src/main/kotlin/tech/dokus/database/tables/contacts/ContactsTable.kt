package tech.dokus.database.tables.contacts

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.documents.DocumentsTable
import tech.dokus.domain.enums.ClientType
import tech.dokus.domain.enums.ContactSource
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Contacts table - stores all contacts (customers AND vendors) for a tenant.
 * Replaces the legacy clients table with unified contact management.
 *
 * OWNER: contacts service
 * CRITICAL: All queries MUST filter by tenant_id
 */
object ContactsTable : UUIDTable("contacts") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").references(
        TenantTable.id,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // Contact identification
    val name = varchar("name", 255)
    val email = varchar("email", 255).nullable().index()
    val phone = varchar("phone", 50).nullable()
    val contactPerson = varchar("contact_person", 255).nullable()

    // Business identification
    val vatNumber = varchar("vat_number", 50).nullable()
    val companyNumber = varchar("company_number", 50).nullable()

    // Business type (Individual/Business/Government) - role is now derived from cashflow items
    val businessType = dbEnumeration<ClientType>("business_type").default(ClientType.Business)

    // NOTE: PEPPOL fields (peppolId, peppolEnabled) moved to PeppolDirectoryCacheTable
    // PEPPOL recipient capability is discovery data, not contact master data

    // Defaults for invoicing
    val defaultPaymentTerms = integer("default_payment_terms").default(30)
    val defaultVatRate = decimal("default_vat_rate", 5, 2).nullable()

    // Metadata
    val tags = text("tags").nullable()
    val isActive = bool("is_active").default(true)

    // System contact tracking
    val isSystemContact = bool("is_system_contact").default(false) // "Unknown Contact" placeholder
    val createdFromDocumentId = uuid("created_from_document_id")
        .references(DocumentsTable.id, onDelete = ReferenceOption.SET_NULL)
        .nullable() // Track which document led to this contact's creation (user-confirmed)
    val contactSource = dbEnumeration<ContactSource>("source").default(ContactSource.Manual)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Prevent duplicates per tenant on VAT number when provided
        uniqueIndex(tenantId, vatNumber)
        // Composite indexes for common queries
        index(false, tenantId, isActive)
        // For filtering system contacts (Unknown Contact placeholder)
        index(false, tenantId, isSystemContact)
    }
}
