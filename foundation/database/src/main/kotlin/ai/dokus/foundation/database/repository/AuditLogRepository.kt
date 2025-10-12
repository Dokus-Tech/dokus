package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.enums.AuditAction
import ai.dokus.foundation.database.enums.EntityType
import ai.dokus.foundation.database.tables.AuditLogsTable
import ai.dokus.foundation.database.utils.dbQuery
import kotlinx.serialization.encodeToString
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
        tenantId: Uuid,
        userId: Uuid? = null,
        action: AuditAction,
        entityType: EntityType,
        entityId: Uuid,
        oldValues: Map<String, Any?>? = null,
        newValues: Map<String, Any?>? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ): Unit = dbQuery {
        val tenantJavaUuid = tenantId.toJavaUuid()
        val userJavaUuid = userId?.toJavaUuid()
        val entityJavaUuid = entityId.toJavaUuid()

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

        logger.debug("Audit log: tenant=$tenantId action=${action.dbValue} entity=${entityType.dbValue}/$entityId")
    }

    /**
     * Log a financial operation (higher priority logging)
     */
    suspend fun logFinancial(
        tenantId: Uuid,
        userId: Uuid? = null,
        action: AuditAction,
        entityType: EntityType,
        entityId: Uuid,
        amount: String,
        details: Map<String, Any?>,
        ipAddress: String? = null
    ) {
        val enrichedDetails = details + mapOf("amount" to amount)

        log(
            tenantId = tenantId,
            userId = userId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            newValues = enrichedDetails,
            ipAddress = ipAddress
        )

        logger.info("FINANCIAL AUDIT: tenant=$tenantId action=${action.dbValue} amount=$amount")
    }
}