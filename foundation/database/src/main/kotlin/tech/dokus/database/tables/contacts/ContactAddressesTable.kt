package tech.dokus.database.tables.contacts

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import tech.dokus.database.tables.auth.AddressTable
import tech.dokus.domain.enums.AddressType
import tech.dokus.foundation.backend.database.dbEnumeration

/**
 * Join table linking contacts to addresses with type classification.
 * Contacts can have multiple addresses of different types.
 *
 * OWNER: contacts service
 *
 * Note: No tenant_id column - tenant is derived via ContactsTable.tenantId.
 * Repository validates that address.tenant_id matches contact.tenant_id.
 *
 * Ownership model:
 * - Each Address row is owned by exactly one ContactAddress (1:1, no sharing)
 * - Update mutates the owned Address row
 * - Remove deletes both join row AND Address in same transaction
 */
object ContactAddressesTable : UuidTable("contact_addresses") {
    val contactId = uuid("contact_id")
        .references(ContactsTable.id, onDelete = ReferenceOption.CASCADE)

    val addressId = uuid("address_id")
        .references(AddressTable.id, onDelete = ReferenceOption.CASCADE)

    val addressType = dbEnumeration<AddressType>("address_type")
        .default(AddressType.Registered)

    val isDefault = bool("is_default").default(false)

    // Timestamps
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(CurrentDateTime)

    init {
        // Unique per (contact, address, type) - implicitly indexes these columns
        uniqueIndex(contactId, addressId, addressType)
        // Extra index for type filtering queries
        index(false, contactId, addressType)
    }
}
