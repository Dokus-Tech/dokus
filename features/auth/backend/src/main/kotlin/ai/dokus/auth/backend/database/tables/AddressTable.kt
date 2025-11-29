package ai.dokus.auth.backend.database.tables

import ai.dokus.foundation.domain.enums.Country
import ai.dokus.foundation.ktor.database.dbEnumeration
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Address table linked to tenant. Cascade delete ensures addresses
 * are removed when the associated tenant is deleted.
 */
object AddressTable : UUIDTable("addresses") {
    val tenantId = reference(
        name = "tenant_id",
        foreign = TenantTable,
        onDelete = ReferenceOption.CASCADE
    ).index()

    val streetLine1 = varchar("street_line_1", 255)
    val streetLine2 = varchar("street_line_2", 255).nullable()
    val city = varchar("city", 100)
    val postalCode = varchar("postal_code", 20)
    val country = dbEnumeration<Country>("country")

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)
}
