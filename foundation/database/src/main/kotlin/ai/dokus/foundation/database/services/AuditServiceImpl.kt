package ai.dokus.foundation.database.services

import ai.dokus.foundation.database.tables.AuditLogsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.BusinessUserId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Audit logging service for compliance and debugging
 *
 * GDPR Compliance: Provides immutable audit trail for 7+ years
 * Financial Audit: Tracks all financial operations
 * Security: Helps detect unauthorized access
 */
@OptIn(ExperimentalUuidApi::class)
class AuditServiceImpl {
    private val logger = LoggerFactory.getLogger(AuditServiceImpl::class.java)
    private val json = Json { prettyPrint = false }

    /**
     * Log an audit action
     *
     * @param tenantId Tenant performing the action (required for multi-tenancy)
     * @param userId User performing the action (null for system actions)
     * @param action Type of action (Create, Update, Delete, etc.)
     * @param entityType Type of entity affected
     * @param entityId UUID of the entity
     * @param oldValues Previous state (for updates/deletes)
     * @param newValues New state (for creates/updates)
     * @param ipAddress IP address of the request
     * @param userAgent User agent from the request
     */
    suspend fun logAction(
        tenantId: TenantId,
        userId: BusinessUserId? = null,
        action: AuditAction,
        entityType: EntityType,
        entityId: Uuid,
        oldValues: Map<String, Any?>? = null,
        newValues: Map<String, Any?>? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ) = dbQuery {
        AuditLogsTable.insert {
            it[AuditLogsTable.tenantId] = tenantId.value.toJavaUuid()
            it[AuditLogsTable.userId] = userId?.value?.toJavaUuid()
            it[AuditLogsTable.action] = action
            it[AuditLogsTable.entityType] = entityType
            it[AuditLogsTable.entityId] = entityId.toJavaUuid()
            it[AuditLogsTable.oldValues] = oldValues?.let { values -> serializeValues(values) }
            it[AuditLogsTable.newValues] = newValues?.let { values -> serializeValues(values) }
            it[AuditLogsTable.ipAddress] = ipAddress
            it[AuditLogsTable.userAgent] = userAgent
        }

        logger.info(
            "Audit log: tenant=${tenantId.value}, user=${userId?.value}, " +
            "action=$action, entityType=$entityType, entityId=$entityId"
        )
    }

    /**
     * Get audit log for a specific entity
     * Useful for viewing entity history
     */
    suspend fun getEntityHistory(
        tenantId: TenantId,
        entityType: EntityType,
        entityId: Uuid,
        limit: Int = 100
    ): List<AuditLogEntry> = dbQuery {
        AuditLogsTable
            .selectAll()
            .where {
                (AuditLogsTable.tenantId eq tenantId.value.toJavaUuid()) and
                        (AuditLogsTable.entityType eq entityType) and
                        (AuditLogsTable.entityId eq entityId.toJavaUuid())
            }
            .orderBy(AuditLogsTable.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit)
            .map { row ->
                AuditLogEntry(
                    id = row[AuditLogsTable.id].value.toKotlinUuid(),
                    tenantId = TenantId(row[AuditLogsTable.tenantId].value.toKotlinUuid()),
                    userId = row[AuditLogsTable.userId]?.value?.toKotlinUuid()?.let { BusinessUserId(it) },
                    action = row[AuditLogsTable.action],
                    entityType = row[AuditLogsTable.entityType],
                    entityId = row[AuditLogsTable.entityId].toKotlinUuid(),
                    oldValues = row[AuditLogsTable.oldValues]?.let { deserializeValues(it) },
                    newValues = row[AuditLogsTable.newValues]?.let { deserializeValues(it) },
                    ipAddress = row[AuditLogsTable.ipAddress],
                    userAgent = row[AuditLogsTable.userAgent],
                    createdAt = row[AuditLogsTable.createdAt]
                )
            }
    }

    /**
     * Get recent audit logs for a tenant
     * Useful for security monitoring and debugging
     */
    suspend fun getRecentLogs(
        tenantId: TenantId,
        limit: Int = 100
    ): List<AuditLogEntry> = dbQuery {
        AuditLogsTable
            .selectAll()
            .where { AuditLogsTable.tenantId eq tenantId.value.toJavaUuid() }
            .orderBy(AuditLogsTable.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit)
            .map { row ->
                AuditLogEntry(
                    id = row[AuditLogsTable.id].value.toKotlinUuid(),
                    tenantId = TenantId(row[AuditLogsTable.tenantId].value.toKotlinUuid()),
                    userId = row[AuditLogsTable.userId]?.value?.toKotlinUuid()?.let { BusinessUserId(it) },
                    action = row[AuditLogsTable.action],
                    entityType = row[AuditLogsTable.entityType],
                    entityId = row[AuditLogsTable.entityId].toKotlinUuid(),
                    oldValues = row[AuditLogsTable.oldValues]?.let { deserializeValues(it) },
                    newValues = row[AuditLogsTable.newValues]?.let { deserializeValues(it) },
                    ipAddress = row[AuditLogsTable.ipAddress],
                    userAgent = row[AuditLogsTable.userAgent],
                    createdAt = row[AuditLogsTable.createdAt]
                )
            }
    }

    /**
     * Get audit logs for a specific user
     * Useful for reviewing user activity
     */
    suspend fun getUserActivity(
        tenantId: TenantId,
        userId: BusinessUserId,
        limit: Int = 100
    ): List<AuditLogEntry> = dbQuery {
        AuditLogsTable
            .selectAll()
            .where {
                (AuditLogsTable.tenantId eq tenantId.value.toJavaUuid()) and
                        (AuditLogsTable.userId eq userId.value.toJavaUuid())
            }
            .orderBy(AuditLogsTable.createdAt to org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit)
            .map { row ->
                AuditLogEntry(
                    id = row[AuditLogsTable.id].value.toKotlinUuid(),
                    tenantId = TenantId(row[AuditLogsTable.tenantId].value.toKotlinUuid()),
                    userId = row[AuditLogsTable.userId]?.value?.toKotlinUuid()?.let { BusinessUserId(it) },
                    action = row[AuditLogsTable.action],
                    entityType = row[AuditLogsTable.entityType],
                    entityId = row[AuditLogsTable.entityId].toKotlinUuid(),
                    oldValues = row[AuditLogsTable.oldValues]?.let { deserializeValues(it) },
                    newValues = row[AuditLogsTable.newValues]?.let { deserializeValues(it) },
                    ipAddress = row[AuditLogsTable.ipAddress],
                    userAgent = row[AuditLogsTable.userAgent],
                    createdAt = row[AuditLogsTable.createdAt]
                )
            }
    }

    private fun serializeValues(values: Map<String, Any?>): String {
        // Convert values to JSON-serializable format
        val serializable = values.mapValues { (_, value) ->
            when (value) {
                null -> null
                is String -> value
                is Number -> value.toString()
                is Boolean -> value
                else -> value.toString()
            }
        }
        return json.encodeToString(serializable)
    }

    private fun deserializeValues(json: String): Map<String, Any?> {
        return try {
            Json.decodeFromString<Map<String, Any?>>(json)
        } catch (e: Exception) {
            logger.error("Failed to deserialize audit log values: $json", e)
            emptyMap()
        }
    }
}

/**
 * Audit log entry data class
 */
@OptIn(ExperimentalUuidApi::class)
data class AuditLogEntry(
    val id: Uuid,
    val tenantId: TenantId,
    val userId: BusinessUserId?,
    val action: AuditAction,
    val entityType: EntityType,
    val entityId: Uuid,
    val oldValues: Map<String, Any?>?,
    val newValues: Map<String, Any?>?,
    val ipAddress: String?,
    val userAgent: String?,
    val createdAt: kotlinx.datetime.LocalDateTime
)
