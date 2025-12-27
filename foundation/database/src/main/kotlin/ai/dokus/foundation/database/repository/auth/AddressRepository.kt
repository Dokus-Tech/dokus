@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.foundation.database.repository.auth

import ai.dokus.foundation.database.mappers.auth.TenantMappers.toAddress
import ai.dokus.foundation.database.tables.auth.AddressTable
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.UpsertTenantAddressRequest
import tech.dokus.foundation.ktor.database.dbQuery
import tech.dokus.foundation.ktor.database.now
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

class AddressRepository {

    suspend fun getCompanyAddress(tenantId: TenantId): Address? = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        AddressTable
            .selectAll()
            .where { AddressTable.tenantId eq tenantUuid }
            .singleOrNull()
            ?.toAddress()
    }

    suspend fun upsertCompanyAddress(
        tenantId: TenantId,
        request: UpsertTenantAddressRequest,
    ): Address = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        val now = now().toLocalDateTime(TimeZone.UTC)

        val existing = AddressTable
            .selectAll()
            .where { AddressTable.tenantId eq tenantUuid }
            .singleOrNull()

        if (existing == null) {
            AddressTable.insert {
                it[AddressTable.id] = tenantUuid
                it[AddressTable.tenantId] = tenantUuid
                it[AddressTable.streetLine1] = request.streetLine1.trim()
                it[AddressTable.streetLine2] = request.streetLine2?.trim()
                it[AddressTable.city] = request.city.trim()
                it[AddressTable.postalCode] = request.postalCode.trim()
                it[AddressTable.country] = request.country
                it[AddressTable.createdAt] = now
                it[AddressTable.updatedAt] = now
            }
        } else {
            AddressTable.update({ AddressTable.tenantId eq tenantUuid }) {
                it[streetLine1] = request.streetLine1.trim()
                it[streetLine2] = request.streetLine2?.trim()
                it[city] = request.city.trim()
                it[postalCode] = request.postalCode.trim()
                it[country] = request.country
                it[updatedAt] = now
            }
        }

        AddressTable
            .selectAll()
            .where { AddressTable.tenantId eq tenantUuid }
            .single()
            .toAddress()
    }
}

