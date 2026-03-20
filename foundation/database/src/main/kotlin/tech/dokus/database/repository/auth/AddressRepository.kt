@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.database.repository.auth

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.mapper.from
import tech.dokus.database.tables.auth.AddressTable
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.database.now
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

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
            ?.let { Address.from(it) }
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

        Address.from(
            AddressTable
                .selectAll()
                .where { (AddressTable.tenantId eq tenantUuid) and (AddressTable.id eq tenantUuid) }
                .single()
        )
    }

}
