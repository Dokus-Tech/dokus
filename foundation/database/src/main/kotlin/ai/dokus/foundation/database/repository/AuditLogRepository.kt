package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.tables.AuditLogsTable
import ai.dokus.foundation.database.utils.dbQuery
import ai.dokus.foundation.domain.BusinessUserId
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.slf4j.LoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * Repository for immutable audit logging
 * Critical for compliance and debugging
 */
@OptIn(ExperimentalUuidApi::class)
class AuditLogRepository {
    private val logger = LoggerFactory.getLogger(AuditLogRepository::class.java)
    private val json = Json { prettyPrint = false }

    suspend fun log(
        tenantId: TenantId,
        userId: BusinessUserId? = null,
        action: AuditAction,
        entityType: EntityType,
        entityId: String, // Generic entity ID as string since it could be any entity type
        oldValues: Map<String, Any?>? = null,
        newValues: Map<String, Any?>? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ): Unit = dbQuery {
        val tenantJavaUuid = tenantId.value.toJavaUuid()
        val userJavaUuid = userId?.value?.toJavaUuid()
        val entityJavaUuid = Uuid.parse(entityId).toJavaUuid()

        AuditLogsTable.insert {
            it[AuditLogsTable.tenantId] = tenantJavaUuid
            it[AuditLogsTable.userId] = userJavaUuid
            it[AuditLogsTable.action] = action
            it[AuditLogsTable.entityType] = entityType
            it[AuditLogsTable.entityId] = entityJavaUuid
            it[AuditLogsTable.oldValues] = oldValues?.let { values ->
                json.encodeToString(values.mapValues { it.value.toString() })
            }
            it[AuditLogsTable.newValues] = newValues?.let { values ->
                json.encodeToString(values.mapValues { it.value.toString() })
            }
            it[AuditLogsTable.ipAddress] = ipAddress
            it[AuditLogsTable.userAgent] = userAgent
        }

        logger.debug(
            "Audit log: tenant={} action={} entity={}/{}",
            tenantId,
            action.dbValue,
            entityType.dbValue,
            entityId
        )
    }

    /**
     * Log a financial operation (higher priority logging)
     */
    suspend fun logFinancial(
        tenantId: TenantId,
        userId: BusinessUserId? = null,
        action: AuditAction,
        entityType: EntityType,
        entityId: String, // Generic entity ID as string since it could be any entity type
        amount: Money,
        details: Map<String, Any?>,
        ipAddress: String? = null
    ) {
        val enrichedDetails = details + mapOf("amount" to amount.value)

        log(
            tenantId = tenantId,
            userId = userId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            newValues = enrichedDetails,
            ipAddress = ipAddress
        )

        logger.info("FINANCIAL AUDIT: tenant=$tenantId action=${action.dbValue} amount=${amount.value}")
    }
}