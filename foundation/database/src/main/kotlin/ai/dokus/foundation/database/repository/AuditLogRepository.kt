package ai.dokus.foundation.database.repository

import ai.dokus.foundation.database.enums.AuditAction
import ai.dokus.foundation.database.enums.EntityType
import ai.dokus.foundation.database.tables.AuditLogsTable
import ai.dokus.foundation.database.utils.dbQuery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Repository for immutable audit logging
 * Critical for compliance and debugging
 */
class AuditLogRepository {
    private val logger = LoggerFactory.getLogger(AuditLogRepository::class.java)
    private val json = Json { prettyPrint = false }

    suspend fun log(
        tenantId: UUID,
        userId: UUID? = null,
        action: AuditAction,
        entityType: EntityType,
        entityId: UUID,
        oldValues: Map<String, Any?>? = null,
        newValues: Map<String, Any?>? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ): Unit = dbQuery {
        AuditLogsTable.insert {
            it[AuditLogsTable.tenantId] = tenantId
            it[AuditLogsTable.userId] = userId
            it[AuditLogsTable.action] = action
            it[AuditLogsTable.entityType] = entityType
            it[AuditLogsTable.entityId] = entityId
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
        tenantId: UUID,
        userId: UUID? = null,
        action: AuditAction,
        entityType: EntityType,
        entityId: UUID,
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