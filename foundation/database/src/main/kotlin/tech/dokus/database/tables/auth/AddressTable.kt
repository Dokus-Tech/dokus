package tech.dokus.database.tables.auth

import tech.dokus.domain.enums.Country
import tech.dokus.foundation.backend.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Address table linked to tenant. Cascade delete ensures addresses
 * are removed when the associated tenant is deleted.
 *
 * OWNER: auth service
 */
object AddressTable : UUIDTable("addresses") {
    val tenantId = reference(
        name = "tenant_id",
        foreign = TenantTable,
        onDelete = ReferenceOption.CASCADE
    ).uniqueIndex()

    val streetLine1 = varchar("street_line_1", 255)
    val streetLine2 = varchar("street_line_2", 255).nullable()
    val city = varchar("city", 100)
    val postalCode = varchar("postal_code", 20)
    val country = dbEnumeration<Country>("country")

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
