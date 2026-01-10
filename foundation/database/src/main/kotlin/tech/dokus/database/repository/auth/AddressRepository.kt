@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.database.repository.auth

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.mapper.TenantMappers.toAddress
import tech.dokus.database.tables.auth.AddressTable
import tech.dokus.domain.ids.AddressId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.domain.model.contact.ContactAddressInput
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.database.now
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

class AddressRepository {

    // ============================================================================
    // TENANT COMPANY ADDRESS OPERATIONS
    // ============================================================================

    /**
     * Get the primary company address for a tenant.
     * Tenant addresses use the tenant ID as the address ID for 1:1 lookup.
     */
    suspend fun getCompanyAddress(tenantId: TenantId): Address? = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        AddressTable
            .selectAll()
            .where { (AddressTable.tenantId eq tenantUuid) and (AddressTable.id eq tenantUuid) }
            .singleOrNull()
            ?.toAddress()
    }

    /**
     * Create or update the primary company address for a tenant.
     * Uses tenant ID as address ID for 1:1 relationship.
     * Converts Country enum to ISO 3166-1 alpha-2 string for storage.
     */
    suspend fun upsertCompanyAddress(
        tenantId: TenantId,
        request: UpsertTenantAddressRequest,
    ): Address = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        val now = now().toLocalDateTime(TimeZone.UTC)

        val existing = AddressTable
            .selectAll()
            .where { (AddressTable.tenantId eq tenantUuid) and (AddressTable.id eq tenantUuid) }
            .singleOrNull()

        if (existing == null) {
            AddressTable.insert {
                it[AddressTable.id] = tenantUuid
                it[AddressTable.tenantId] = tenantUuid
                it[AddressTable.streetLine1] = request.streetLine1.trim()
                it[AddressTable.streetLine2] = request.streetLine2?.trim()
                it[AddressTable.city] = request.city.trim()
                it[AddressTable.postalCode] = request.postalCode.trim()
                it[AddressTable.country] = request.country.dbValue // Convert enum to ISO-2 string
                it[AddressTable.createdAt] = now
                it[AddressTable.updatedAt] = now
            }
        } else {
            AddressTable.update({
                (AddressTable.tenantId eq tenantUuid) and (AddressTable.id eq tenantUuid)
            }) {
                it[streetLine1] = request.streetLine1.trim()
                it[streetLine2] = request.streetLine2?.trim()
                it[city] = request.city.trim()
                it[postalCode] = request.postalCode.trim()
                it[country] = request.country.dbValue // Convert enum to ISO-2 string
                it[updatedAt] = now
            }
        }

        AddressTable
            .selectAll()
            .where { (AddressTable.tenantId eq tenantUuid) and (AddressTable.id eq tenantUuid) }
            .single()
            .toAddress()
    }

    // ============================================================================
    // GENERIC ADDRESS OPERATIONS (for contact addresses)
    // ============================================================================

    /**
     * Create a new address owned by the given tenant.
     * Used for contact addresses - each contact address owns its Address row.
     */
    suspend fun createAddress(
        tenantId: TenantId,
        input: ContactAddressInput
    ): Address = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        val addressId = Uuid.random().toJavaUuid()
        val now = now().toLocalDateTime(TimeZone.UTC)

        AddressTable.insert {
            it[AddressTable.id] = addressId
            it[AddressTable.tenantId] = tenantUuid
            it[AddressTable.streetLine1] = input.streetLine1?.trim()
            it[AddressTable.streetLine2] = input.streetLine2?.trim()
            it[AddressTable.city] = input.city?.trim()
            it[AddressTable.postalCode] = input.postalCode?.trim()
            it[AddressTable.country] = input.country?.trim()
            it[AddressTable.createdAt] = now
            it[AddressTable.updatedAt] = now
        }

        Address(
            id = AddressId(addressId.toKotlinUuid()),
            tenantId = tenantId,
            streetLine1 = input.streetLine1?.trim(),
            streetLine2 = input.streetLine2?.trim(),
            city = input.city?.trim(),
            postalCode = input.postalCode?.trim(),
            country = input.country?.trim(),
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * Get an address by ID, validating tenant ownership.
     * Returns null if not found or belongs to different tenant.
     */
    suspend fun getAddressById(tenantId: TenantId, addressId: AddressId): Address? = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        val addressUuid = addressId.value.toJavaUuid()

        AddressTable
            .selectAll()
            .where { (AddressTable.id eq addressUuid) and (AddressTable.tenantId eq tenantUuid) }
            .singleOrNull()
            ?.toAddress()
    }

    /**
     * Update an existing address.
     * Only updates fields that are provided (non-null).
     */
    suspend fun updateAddress(
        tenantId: TenantId,
        addressId: AddressId,
        input: ContactAddressInput
    ): Address? = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        val addressUuid = addressId.value.toJavaUuid()
        val now = now().toLocalDateTime(TimeZone.UTC)

        val updated = AddressTable.update({
            (AddressTable.id eq addressUuid) and (AddressTable.tenantId eq tenantUuid)
        }) {
            it[streetLine1] = input.streetLine1?.trim()
            it[streetLine2] = input.streetLine2?.trim()
            it[city] = input.city?.trim()
            it[postalCode] = input.postalCode?.trim()
            it[country] = input.country?.trim()
            it[updatedAt] = now
        }

        if (updated > 0) {
            AddressTable
                .selectAll()
                .where { (AddressTable.id eq addressUuid) and (AddressTable.tenantId eq tenantUuid) }
                .single()
                .toAddress()
        } else {
            null
        }
    }

    /**
     * Delete an address by ID.
     * Used when removing a contact address - deletes the owned Address row.
     */
    suspend fun deleteAddress(tenantId: TenantId, addressId: AddressId): Boolean = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        val addressUuid = addressId.value.toJavaUuid()

        val deleted = AddressTable.deleteWhere {
            (AddressTable.id eq addressUuid) and (AddressTable.tenantId eq tenantUuid)
        }
        deleted > 0
    }
}
