package ai.dokus.foundation.database.repository.audit

import ai.dokus.foundation.database.tables.audit.AuditLogsTable
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.ids.AuditLogId
import ai.dokus.foundation.domain.ids.BusinessUserId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.AuditLogDto
import ai.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi

/**
 * Repository for managing audit logs.
 *
 * CRITICAL SECURITY RULES:
 * 1. ALWAYS filter by tenant_id in every query
 * 2. Audit logs are IMMUTABLE - no updates or deletes
 * 3. 7-year retention for financial records
 */
@OptIn(ExperimentalUuidApi::class)
class AuditRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Create a new audit log entry.
     * CRITICAL: MUST include tenant_id for multi-tenancy security
     */
    suspend fun log(
        tenantId: TenantId,
        userId: BusinessUserId?,
        action: AuditAction,
        entityType: EntityType,
        entityId: String,
        oldValues: Map<String, String>?,
        newValues: Map<String, String>?,
        ipAddress: String?,
        userAgent: String?
    ): Result<AuditLogDto> = runCatching {
        dbQuery {
            val id = AuditLogsTable.insert {
                it[AuditLogsTable.tenantId] = UUID.fromString(tenantId.toString())
                it[AuditLogsTable.userId] = userId?.let { uid -> UUID.fromString(uid.toString()) }
                it[AuditLogsTable.action] = action
                it[AuditLogsTable.entityType] = entityType
                it[AuditLogsTable.entityId] = entityId
                it[AuditLogsTable.oldValues] = oldValues?.let { json.encodeToString(it) }
                it[AuditLogsTable.newValues] = newValues?.let { json.encodeToString(it) }
                it[AuditLogsTable.ipAddress] = ipAddress
                it[AuditLogsTable.userAgent] = userAgent
            } get AuditLogsTable.id

            AuditLogsTable.selectAll().where {
                AuditLogsTable.id eq id.value
            }.single().toAuditLogDto()
        }
    }

    /**
     * List audit logs for an entity.
     */
    suspend fun listByEntity(
        entityType: EntityType,
        entityId: String
    ): Result<List<AuditLogDto>> = runCatching {
        dbQuery {
            AuditLogsTable.selectAll().where {
                (AuditLogsTable.entityType eq entityType) and
                (AuditLogsTable.entityId eq entityId)
            }.orderBy(AuditLogsTable.createdAt, SortOrder.DESC)
                .map { it.toAuditLogDto() }
        }
    }

    /**
     * List audit logs for a tenant with filters.
     * CRITICAL: MUST filter by tenant_id
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        action: AuditAction? = null,
        entityType: EntityType? = null,
        userId: BusinessUserId? = null,
        fromDate: LocalDateTime? = null,
        toDate: LocalDateTime? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<AuditLogDto>> = runCatching {
        dbQuery {
            var query = AuditLogsTable.selectAll().where {
                AuditLogsTable.tenantId eq UUID.fromString(tenantId.toString())
            }

            action?.let {
                query = query.andWhere { AuditLogsTable.action eq it }
            }
            entityType?.let {
                query = query.andWhere { AuditLogsTable.entityType eq it }
            }
            userId?.let {
                query = query.andWhere { AuditLogsTable.userId eq UUID.fromString(it.toString()) }
            }
            fromDate?.let {
                query = query.andWhere { AuditLogsTable.createdAt greaterEq it }
            }
            toDate?.let {
                query = query.andWhere { AuditLogsTable.createdAt lessEq it }
            }

            query.orderBy(AuditLogsTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .offset(offset.toLong())
                .map { it.toAuditLogDto() }
        }
    }

    /**
     * Find audit log by ID.
     */
    suspend fun findById(id: AuditLogId): Result<AuditLogDto?> = runCatching {
        dbQuery {
            AuditLogsTable.selectAll().where {
                AuditLogsTable.id eq UUID.fromString(id.toString())
            }.singleOrNull()?.toAuditLogDto()
        }
    }

    /**
     * Get latest audit log for an entity.
     */
    suspend fun getLatestForEntity(
        entityType: EntityType,
        entityId: String
    ): Result<AuditLogDto?> = runCatching {
        dbQuery {
            AuditLogsTable.selectAll().where {
                (AuditLogsTable.entityType eq entityType) and
                (AuditLogsTable.entityId eq entityId)
            }.orderBy(AuditLogsTable.createdAt, SortOrder.DESC)
                .limit(1)
                .singleOrNull()?.toAuditLogDto()
        }
    }

    private fun ResultRow.toAuditLogDto(): AuditLogDto {
        return AuditLogDto(
            id = AuditLogId.parse(this[AuditLogsTable.id].value.toString()),
            tenantId = TenantId.parse(this[AuditLogsTable.tenantId].toString()),
            userId = this[AuditLogsTable.userId]?.let { BusinessUserId.parse(it.toString()) },
            action = this[AuditLogsTable.action],
            entityType = this[AuditLogsTable.entityType],
            entityId = this[AuditLogsTable.entityId],
            oldValues = this[AuditLogsTable.oldValues]?.let {
                json.decodeFromString<Map<String, String>>(it)
            },
            newValues = this[AuditLogsTable.newValues]?.let {
                json.decodeFromString<Map<String, String>>(it)
            },
            ipAddress = this[AuditLogsTable.ipAddress],
            userAgent = this[AuditLogsTable.userAgent],
            createdAt = this[AuditLogsTable.createdAt]
        )
    }
}
