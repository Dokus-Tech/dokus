package ai.dokus.foundation.database.tables.cashflow

import ai.dokus.foundation.domain.enums.ClientType
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Clients table - stores all client/customer data for a tenant.
 *
 * OWNER: cashflow service
 * CRITICAL: All queries MUST filter by tenant_id
 */
object ClientsTable : UUIDTable("clients") {
    // Multi-tenancy (CRITICAL)
    val tenantId = uuid("tenant_id").index()

    // Client identification
    val name = varchar("name", 255)
    val email = varchar("email", 255).nullable()
    val phone = varchar("phone", 50).nullable()
    val contactPerson = varchar("contact_person", 255).nullable()

    // Business identification
    val vatNumber = varchar("vat_number", 50).nullable()
    val companyNumber = varchar("company_number", 50).nullable()

    // Peppol e-invoicing
    val peppolId = varchar("peppol_id", 255).nullable()
    val peppolEnabled = bool("peppol_enabled").default(false)

    // Address
    val addressLine1 = varchar("address_line_1", 255).nullable()
    val addressLine2 = varchar("address_line_2", 255).nullable()
    val city = varchar("city", 100).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 2).nullable() // ISO 3166-1 alpha-2

    // Client type
    val clientType = dbEnumeration<ClientType>("client_type").default(ClientType.Business)

    // Defaults for invoicing
    val defaultPaymentTerms = integer("default_payment_terms").default(30)
    val defaultVatRate = decimal("default_vat_rate", 5, 2).nullable()

    // Metadata
    val tags = text("tags").nullable()
    val notes = text("notes").nullable()
    val isActive = bool("is_active").default(true)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
