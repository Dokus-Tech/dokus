package tech.dokus.database.tables.auth

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Shared address storage table.
 * Used for both tenant company addresses and contact addresses.
 *
 * OWNER: auth service (tenant addresses), contacts service (contact addresses)
 *
 * Note: Fields are nullable to support AI partial extraction for contacts.
 * Tenant company addresses enforce required fields at the service layer.
 */
object AddressTable : UuidTable("addresses") {
    // Changed from uniqueIndex() to index() to allow multiple addresses per tenant
    val tenantId = reference(
        name = "tenant_id",
        foreign = TenantTable,
        onDelete = ReferenceOption.CASCADE
    ).index()

    // All fields nullable to support AI partial extraction
    val streetLine1 = varchar("street_line_1", 255).nullable()
    val streetLine2 = varchar("street_line_2", 255).nullable()
    val city = varchar("city", 100).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 2).nullable() // ISO 3166-1 alpha-2

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
