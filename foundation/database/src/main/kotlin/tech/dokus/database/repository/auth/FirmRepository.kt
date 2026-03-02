package tech.dokus.database.repository.auth

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.mapper.FirmMappers.toFirm
import tech.dokus.database.mapper.FirmMappers.toFirmAccess
import tech.dokus.database.mapper.FirmMappers.toFirmMembership
import tech.dokus.database.tables.auth.FirmAccessTable
import tech.dokus.database.tables.auth.FirmMembersTable
import tech.dokus.database.tables.auth.FirmsTable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.enums.FirmAccessStatus
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Firm
import tech.dokus.domain.model.FirmAccess
import tech.dokus.domain.model.FirmMembership
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class FirmRepository {
    private val logger = loggerFor()

    suspend fun createFirm(
        name: DisplayName,
        vatNumber: VatNumber,
        ownerUserId: UserId,
    ): Firm = dbQuery {
        val firmId = FirmsTable.insertAndGetId {
            it[FirmsTable.name] = name.value
            it[FirmsTable.vatNumber] = vatNumber.value
        }.value

        FirmMembersTable.insert {
            it[userId] = ownerUserId.value.toJavaUuid()
            it[this.firmId] = firmId
            it[role] = FirmRole.Owner
            it[isActive] = true
        }

        logger.info("Created firm {} for owner {}", firmId, ownerUserId)

        FirmsTable
            .selectAll()
            .where { FirmsTable.id eq firmId }
            .single()
            .toFirm()
    }

    suspend fun findById(firmId: FirmId): Firm? = dbQuery {
        FirmsTable
            .selectAll()
            .where { FirmsTable.id eq firmId.value.toJavaUuid() }
            .singleOrNull()
            ?.toFirm()
    }

    suspend fun getMembership(userId: UserId, firmId: FirmId): FirmMembership? = dbQuery {
        FirmMembersTable
            .selectAll()
            .where {
                (FirmMembersTable.userId eq userId.value.toJavaUuid()) and
                    (FirmMembersTable.firmId eq firmId.value.toJavaUuid())
            }
            .singleOrNull()
            ?.toFirmMembership()
    }

    suspend fun listUserMemberships(userId: UserId, activeOnly: Boolean = true): List<FirmMembership> = dbQuery {
        val base = FirmMembersTable
            .selectAll()
            .where { FirmMembersTable.userId eq userId.value.toJavaUuid() }

        if (!activeOnly) {
            base.map { it.toFirmMembership() }
        } else {
            base
                .filter { it[FirmMembersTable.isActive] }
                .map { it.toFirmMembership() }
        }
    }

    suspend fun listFirmsByIds(firmIds: List<FirmId>): List<Firm> = dbQuery {
        if (firmIds.isEmpty()) return@dbQuery emptyList()

        FirmsTable
            .selectAll()
            .where { FirmsTable.id inList firmIds.map { it.value.toJavaUuid() } }
            .map { it.toFirm() }
    }

    suspend fun countActiveClientsByFirmIds(firmIds: List<FirmId>): Map<FirmId, Int> = dbQuery {
        if (firmIds.isEmpty()) return@dbQuery emptyMap()

        FirmAccessTable
            .selectAll()
            .where {
                (FirmAccessTable.firmId inList firmIds.map { it.value.toJavaUuid() }) and
                    (FirmAccessTable.status eq FirmAccessStatus.Active)
            }
            .groupBy { FirmId(it[FirmAccessTable.firmId].value.toKotlinUuid()) }
            .mapValues { (_, rows) -> rows.size }
    }

    suspend fun listActiveAccessByFirm(firmId: FirmId): List<FirmAccess> = dbQuery {
        FirmAccessTable
            .selectAll()
            .where {
                (FirmAccessTable.firmId eq firmId.value.toJavaUuid()) and
                    (FirmAccessTable.status eq FirmAccessStatus.Active)
            }
            .map { it.toFirmAccess() }
    }

    suspend fun hasActiveAccess(firmId: FirmId, tenantId: TenantId): Boolean = dbQuery {
        FirmAccessTable
            .selectAll()
            .where {
                (FirmAccessTable.firmId eq firmId.value.toJavaUuid()) and
                    (FirmAccessTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (FirmAccessTable.status eq FirmAccessStatus.Active)
            }
            .singleOrNull() != null
    }

    suspend fun activateAccess(
        firmId: FirmId,
        tenantId: TenantId,
        grantedByUserId: UserId,
    ): Boolean = dbQuery {
        val existing = FirmAccessTable
            .selectAll()
            .where {
                (FirmAccessTable.firmId eq firmId.value.toJavaUuid()) and
                    (FirmAccessTable.tenantId eq tenantId.value.toJavaUuid())
            }
            .singleOrNull()

        if (existing == null) {
            FirmAccessTable.insert {
                it[this.firmId] = firmId.value.toJavaUuid()
                it[this.tenantId] = tenantId.value.toJavaUuid()
                it[status] = FirmAccessStatus.Active
                it[this.grantedByUserId] = grantedByUserId.value.toJavaUuid()
            }
            true
        } else {
            val wasActive = existing[FirmAccessTable.status] == FirmAccessStatus.Active
            FirmAccessTable.update({
                (FirmAccessTable.firmId eq firmId.value.toJavaUuid()) and
                    (FirmAccessTable.tenantId eq tenantId.value.toJavaUuid())
            }) {
                it[status] = FirmAccessStatus.Active
                it[FirmAccessTable.grantedByUserId] = grantedByUserId.value.toJavaUuid()
            }
            !wasActive
        }
    }

    suspend fun revokeAccess(firmId: FirmId, tenantId: TenantId): Boolean = dbQuery {
        val updated = FirmAccessTable.update({
            (FirmAccessTable.firmId eq firmId.value.toJavaUuid()) and
                (FirmAccessTable.tenantId eq tenantId.value.toJavaUuid()) and
                (FirmAccessTable.status eq FirmAccessStatus.Active)
        }) {
            it[status] = FirmAccessStatus.Revoked
        }
        updated > 0
    }
}
