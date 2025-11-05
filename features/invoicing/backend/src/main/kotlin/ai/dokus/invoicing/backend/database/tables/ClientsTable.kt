package ai.dokus.invoicing.backend.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import java.util.UUID as JavaUUID

/**
 * Customers who receive invoices
 * The freelancer's clients/customers
 */
object ClientsTable : UUIDTable("clients") {
    val tenantId = uuid("tenant_id")

    // Basic info
    val name = varchar("name", 255)
    val email = varchar("email", 255).nullable()
    val vatNumber = varchar("vat_number", 50).nullable()

    // Address
    val addressLine1 = varchar("address_line_1", 255).nullable()
    val addressLine2 = varchar("address_line_2", 255).nullable()
    val city = varchar("city", 100).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 2).nullable() // ISO 3166-1 alpha-2

    // Contact
    val contactPerson = varchar("contact_person", 255).nullable()
    val phone = varchar("phone", 50).nullable()

    // Financial settings
    val companyNumber = varchar("company_number", 50).nullable()
    val defaultPaymentTerms = integer("default_payment_terms").default(30) // days
    val defaultVatRate = decimal("default_vat_rate", 5, 2).nullable()

    // Peppol e-invoicing (required for Belgium 2026)
    val peppolId = varchar("peppol_id", 100).nullable() // e.g., "0208:BE0123456789"
    val peppolEnabled = bool("peppol_enabled").default(false)

    // Additional
    val tags = varchar("tags", 500).nullable() // Comma-separated tags
    val notes = text("notes").nullable()
    val isActive = bool("is_active").default(true)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        index(false, tenantId)
        index(false, tenantId, name)      // Client search
        index(false, tenantId, isActive)  // Active clients list
    }
}