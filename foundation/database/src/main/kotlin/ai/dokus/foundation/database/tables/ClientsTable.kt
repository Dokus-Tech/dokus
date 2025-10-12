package ai.dokus.foundation.database.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Customers who receive invoices
 * The freelancer's clients/customers
 */
object ClientsTable : UUIDTable("clients") {
    val tenantId = reference("tenant_id", TenantsTable, onDelete = ReferenceOption.CASCADE)

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

    // Additional
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