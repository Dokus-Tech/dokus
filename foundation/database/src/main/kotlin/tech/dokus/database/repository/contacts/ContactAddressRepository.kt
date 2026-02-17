@file:Suppress("UseRequire") // Custom exception messaging

package tech.dokus.database.repository.contacts

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.auth.AddressTable
import tech.dokus.database.tables.contacts.ContactAddressesTable
import tech.dokus.database.tables.contacts.ContactsTable
import tech.dokus.domain.enums.AddressType
import tech.dokus.domain.ids.AddressId
import tech.dokus.domain.ids.ContactAddressId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.AddressDto
import tech.dokus.domain.model.contact.ContactAddressDto
import tech.dokus.domain.model.contact.ContactAddressInput
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.database.now
import kotlin.uuid.Uuid

/**
 * Repository for managing contact addresses.
 *
 * Ownership model:
 * - Each Address row is owned by exactly one ContactAddress (1:1, no sharing)
 * - Update mutates the owned Address row
 * - Remove deletes both join row AND Address in same transaction
 *
 * CRITICAL SECURITY RULES:
 * 1. All methods validate contact belongs to tenant
 * 2. All methods validate address.tenant_id matches contact.tenant_id
 * 3. No cross-tenant links allowed
 */
class ContactAddressRepository {

    /**
     * Add an address to a contact.
     * Creates Address + ContactAddress join row in single transaction.
     *
     * CRITICAL: Validates contact belongs to tenant before creating.
     */
    suspend fun addAddress(
        tenantId: TenantId,
        contactId: ContactId,
        input: ContactAddressInput
    ): Result<ContactAddressDto> = runCatching {
        dbQuery {
            val tenantUuid = tenantId.value
            val contactUuid = contactId.value

            // Validate contact belongs to tenant
            val contactExists = ContactsTable.selectAll().where {
                (ContactsTable.id eq contactUuid) and (ContactsTable.tenantId eq tenantUuid)
            }.count() > 0

            if (!contactExists) {
                throw IllegalArgumentException("Contact not found or access denied")
            }

            val now = now().toLocalDateTime(TimeZone.UTC)

            // Create the Address row (owned by this contact address)
            val addressId = Uuid.random()
            AddressTable.insert {
                it[AddressTable.id] = addressId
                it[AddressTable.tenantId] = tenantUuid
                it[streetLine1] = input.streetLine1?.trim()
                it[streetLine2] = input.streetLine2?.trim()
                it[city] = input.city?.trim()
                it[postalCode] = input.postalCode?.trim()
                it[country] = input.country?.trim()
                it[createdAt] = now
                it[updatedAt] = now
            }

            // Handle default enforcement - clear existing defaults for this type if needed
            if (input.isDefault) {
                ContactAddressesTable.update({
                    (ContactAddressesTable.contactId eq contactUuid) and
                        (ContactAddressesTable.addressType eq input.addressType) and
                        (ContactAddressesTable.isDefault eq true)
                }) {
                    it[isDefault] = false
                }
            }

            // Create the join row
            val contactAddressId = Uuid.random()
            ContactAddressesTable.insert {
                it[ContactAddressesTable.id] = contactAddressId
                it[ContactAddressesTable.contactId] = contactUuid
                it[ContactAddressesTable.addressId] = addressId
                it[addressType] = input.addressType
                it[isDefault] = input.isDefault
                it[createdAt] = now
                it[updatedAt] = now
            }

            ContactAddressDto(
                id = ContactAddressId(contactAddressId),
                address = AddressDto(
                    id = AddressId(addressId),
                    streetLine1 = input.streetLine1?.trim(),
                    streetLine2 = input.streetLine2?.trim(),
                    city = input.city?.trim(),
                    postalCode = input.postalCode?.trim(),
                    country = input.country?.trim()
                ),
                addressType = input.addressType,
                isDefault = input.isDefault
            )
        }
    }

    /**
     * Update an existing contact address.
     * Mutates the owned Address row.
     *
     * CRITICAL: Validates contact address belongs to contact which belongs to tenant.
     */
    suspend fun updateAddress(
        tenantId: TenantId,
        contactAddressId: ContactAddressId,
        input: ContactAddressInput
    ): Result<ContactAddressDto> = runCatching {
        dbQuery {
            val tenantUuid = tenantId.value
            val contactAddressUuid = contactAddressId.value

            // Get the contact address and validate ownership chain
            val joinRow = ContactAddressesTable
                .selectAll()
                .where { ContactAddressesTable.id eq contactAddressUuid }
                .singleOrNull() ?: throw IllegalArgumentException("Contact address not found")

            val contactUuid = joinRow[ContactAddressesTable.contactId]
            val addressUuid = joinRow[ContactAddressesTable.addressId]

            // Validate contact belongs to tenant
            val contactBelongsToTenant = ContactsTable.selectAll().where {
                (ContactsTable.id eq contactUuid) and (ContactsTable.tenantId eq tenantUuid)
            }.count() > 0

            if (!contactBelongsToTenant) {
                throw IllegalArgumentException("Contact address not found or access denied")
            }

            val now = now().toLocalDateTime(TimeZone.UTC)

            // Update the owned Address row
            AddressTable.update({
                (AddressTable.id eq addressUuid) and (AddressTable.tenantId eq tenantUuid)
            }) {
                it[streetLine1] = input.streetLine1?.trim()
                it[streetLine2] = input.streetLine2?.trim()
                it[city] = input.city?.trim()
                it[postalCode] = input.postalCode?.trim()
                it[country] = input.country?.trim()
                it[updatedAt] = now
            }

            // Handle default enforcement if changing to default
            if (input.isDefault) {
                ContactAddressesTable.update({
                    (ContactAddressesTable.contactId eq contactUuid) and
                        (ContactAddressesTable.addressType eq input.addressType) and
                        (ContactAddressesTable.isDefault eq true) and
                        (ContactAddressesTable.id neq contactAddressUuid)
                }) {
                    it[isDefault] = false
                }
            }

            // Update the join row metadata
            ContactAddressesTable.update({
                ContactAddressesTable.id eq contactAddressUuid
            }) {
                it[addressType] = input.addressType
                it[isDefault] = input.isDefault
                it[updatedAt] = now
            }

            ContactAddressDto(
                id = contactAddressId,
                address = AddressDto(
                    id = AddressId(addressUuid),
                    streetLine1 = input.streetLine1?.trim(),
                    streetLine2 = input.streetLine2?.trim(),
                    city = input.city?.trim(),
                    postalCode = input.postalCode?.trim(),
                    country = input.country?.trim()
                ),
                addressType = input.addressType,
                isDefault = input.isDefault
            )
        }
    }

    /**
     * Remove a contact address.
     * Deletes both join row AND Address row in same transaction.
     *
     * CRITICAL: Validates ownership before deleting.
     */
    suspend fun removeAddress(
        tenantId: TenantId,
        contactAddressId: ContactAddressId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val tenantUuid = tenantId.value
            val contactAddressUuid = contactAddressId.value

            // Get the contact address and validate ownership chain
            val joinRow = ContactAddressesTable
                .selectAll()
                .where { ContactAddressesTable.id eq contactAddressUuid }
                .singleOrNull() ?: return@dbQuery false

            val contactUuid = joinRow[ContactAddressesTable.contactId]
            val addressUuid = joinRow[ContactAddressesTable.addressId]

            // Validate contact belongs to tenant
            val contactBelongsToTenant = ContactsTable.selectAll().where {
                (ContactsTable.id eq contactUuid) and (ContactsTable.tenantId eq tenantUuid)
            }.count() > 0

            if (!contactBelongsToTenant) {
                throw IllegalArgumentException("Contact address not found or access denied")
            }

            // Delete join row first (FK constraint)
            ContactAddressesTable.deleteWhere {
                ContactAddressesTable.id eq contactAddressUuid
            }

            // Delete owned Address row
            AddressTable.deleteWhere {
                (AddressTable.id eq addressUuid) and (AddressTable.tenantId eq tenantUuid)
            }

            true
        }
    }

    /**
     * List all addresses for a contact.
     * Batch-loads addresses to avoid N+1 queries.
     *
     * CRITICAL: Validates contact belongs to tenant.
     */
    suspend fun listAddresses(
        tenantId: TenantId,
        contactId: ContactId
    ): Result<List<ContactAddressDto>> = runCatching {
        dbQuery {
            val tenantUuid = tenantId.value
            val contactUuid = contactId.value

            // Validate contact belongs to tenant
            val contactExists = ContactsTable.selectAll().where {
                (ContactsTable.id eq contactUuid) and (ContactsTable.tenantId eq tenantUuid)
            }.count() > 0

            if (!contactExists) {
                throw IllegalArgumentException("Contact not found or access denied")
            }

            // Join query to get all addresses in one round-trip
            (ContactAddressesTable innerJoin AddressTable)
                .selectAll()
                .where {
                    (ContactAddressesTable.contactId eq contactUuid) and
                        (AddressTable.tenantId eq tenantUuid)
                }
                .map { row ->
                    ContactAddressDto(
                        id = ContactAddressId(row[ContactAddressesTable.id].value),
                        address = AddressDto(
                            id = AddressId(row[AddressTable.id].value),
                            streetLine1 = row[AddressTable.streetLine1],
                            streetLine2 = row[AddressTable.streetLine2],
                            city = row[AddressTable.city],
                            postalCode = row[AddressTable.postalCode],
                            country = row[AddressTable.country]
                        ),
                        addressType = row[ContactAddressesTable.addressType],
                        isDefault = row[ContactAddressesTable.isDefault]
                    )
                }
        }
    }

    /**
     * Batch-load addresses for multiple contacts.
     * Returns a map of contactId -> list of addresses.
     * Used to avoid N+1 queries when listing contacts.
     */
    suspend fun batchLoadAddresses(
        tenantId: TenantId,
        contactIds: List<ContactId>
    ): Result<Map<ContactId, List<ContactAddressDto>>> = runCatching {
        if (contactIds.isEmpty()) {
            return@runCatching emptyMap()
        }

        dbQuery {
            val tenantUuid = tenantId.value
            val contactUuids = contactIds.map { it.value }

            // Single query to load all addresses for all contacts
            (ContactAddressesTable innerJoin AddressTable)
                .selectAll()
                .where {
                    (ContactAddressesTable.contactId inList contactUuids) and
                        (AddressTable.tenantId eq tenantUuid)
                }
                .groupBy { row ->
                    ContactId(row[ContactAddressesTable.contactId])
                }
                .mapValues { (_, rows) ->
                    rows.map { row ->
                        ContactAddressDto(
                            id = ContactAddressId(row[ContactAddressesTable.id].value),
                            address = AddressDto(
                                id = AddressId(row[AddressTable.id].value),
                                streetLine1 = row[AddressTable.streetLine1],
                                streetLine2 = row[AddressTable.streetLine2],
                                city = row[AddressTable.city],
                                postalCode = row[AddressTable.postalCode],
                                country = row[AddressTable.country]
                            ),
                            addressType = row[ContactAddressesTable.addressType],
                            isDefault = row[ContactAddressesTable.isDefault]
                        )
                    }
                }
        }
    }

    /**
     * Get the default address for a contact, optionally filtered by type.
     */
    suspend fun getDefaultAddress(
        tenantId: TenantId,
        contactId: ContactId,
        addressType: AddressType? = null
    ): Result<ContactAddressDto?> = runCatching {
        dbQuery {
            val tenantUuid = tenantId.value
            val contactUuid = contactId.value

            // Validate contact belongs to tenant
            val contactExists = ContactsTable.selectAll().where {
                (ContactsTable.id eq contactUuid) and (ContactsTable.tenantId eq tenantUuid)
            }.count() > 0

            if (!contactExists) {
                throw IllegalArgumentException("Contact not found or access denied")
            }

            var query = (ContactAddressesTable innerJoin AddressTable)
                .selectAll()
                .where {
                    (ContactAddressesTable.contactId eq contactUuid) and
                        (ContactAddressesTable.isDefault eq true) and
                        (AddressTable.tenantId eq tenantUuid)
                }

            if (addressType != null) {
                query = query.andWhere {
                    ContactAddressesTable.addressType eq addressType
                }
            }

            query.singleOrNull()?.let { row ->
                ContactAddressDto(
                    id = ContactAddressId(row[ContactAddressesTable.id].value),
                    address = AddressDto(
                        id = AddressId(row[AddressTable.id].value),
                        streetLine1 = row[AddressTable.streetLine1],
                        streetLine2 = row[AddressTable.streetLine2],
                        city = row[AddressTable.city],
                        postalCode = row[AddressTable.postalCode],
                        country = row[AddressTable.country]
                    ),
                    addressType = row[ContactAddressesTable.addressType],
                    isDefault = row[ContactAddressesTable.isDefault]
                )
            }
        }
    }

    /**
     * Set an address as the default for its type.
     * Clears other defaults for that (contact, type) pair first.
     */
    suspend fun setDefaultAddress(
        tenantId: TenantId,
        contactAddressId: ContactAddressId
    ): Result<Boolean> = runCatching {
        dbQuery {
            val tenantUuid = tenantId.value
            val contactAddressUuid = contactAddressId.value

            // Get the contact address and validate ownership
            val joinRow = ContactAddressesTable
                .selectAll()
                .where { ContactAddressesTable.id eq contactAddressUuid }
                .singleOrNull() ?: throw IllegalArgumentException("Contact address not found")

            val contactUuid = joinRow[ContactAddressesTable.contactId]
            val addressType = joinRow[ContactAddressesTable.addressType]

            // Validate contact belongs to tenant
            val contactBelongsToTenant = ContactsTable.selectAll().where {
                (ContactsTable.id eq contactUuid) and (ContactsTable.tenantId eq tenantUuid)
            }.count() > 0

            if (!contactBelongsToTenant) {
                throw IllegalArgumentException("Contact address not found or access denied")
            }

            val now = now().toLocalDateTime(TimeZone.UTC)

            // Clear existing defaults for this (contact, type)
            ContactAddressesTable.update({
                (ContactAddressesTable.contactId eq contactUuid) and
                    (ContactAddressesTable.addressType eq addressType) and
                    (ContactAddressesTable.isDefault eq true)
            }) {
                it[isDefault] = false
                it[updatedAt] = now
            }

            // Set the new default
            val updated = ContactAddressesTable.update({
                ContactAddressesTable.id eq contactAddressUuid
            }) {
                it[isDefault] = true
                it[updatedAt] = now
            }

            updated > 0
        }
    }
}
