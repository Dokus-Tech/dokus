@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.audit.backend.database.services

import ai.dokus.foundation.domain.ids.AuditLogId
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.model.AuditLog
import ai.dokus.foundation.ktor.services.AuditService
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class AuditServiceImpl : AuditService {

    override suspend fun log(
        organizationId: OrganizationId,
        userId: UserId?,
        action: AuditAction,
        entityType: EntityType,
        entityId: Uuid,
        oldValues: String?,
        newValues: String?,
        ipAddress: String?,
        userAgent: String?
    ): AuditLog {
        TODO("Not yet implemented")
    }

    override suspend fun listByEntity(entityType: EntityType, entityId: Uuid): List<AuditLog> {
        TODO("Not yet implemented")
    }

    override suspend fun listByTenant(
        organizationId: OrganizationId,
        action: AuditAction?,
        entityType: EntityType?,
        userId: UserId?,
        fromDate: Instant?,
        toDate: Instant?,
        limit: Int?,
        offset: Int?
    ): List<AuditLog> {
        TODO("Not yet implemented")
    }

    override suspend fun listByUser(
        userId: UserId,
        fromDate: Instant?,
        toDate: Instant?,
        limit: Int?
    ): List<AuditLog> {
        TODO("Not yet implemented")
    }

    override suspend fun listByAction(
        organizationId: OrganizationId,
        action: AuditAction,
        fromDate: Instant?,
        toDate: Instant?,
        limit: Int?
    ): List<AuditLog> {
        TODO("Not yet implemented")
    }

    override suspend fun findById(id: AuditLogId): AuditLog? {
        TODO("Not yet implemented")
    }

    override suspend fun getStatistics(
        organizationId: OrganizationId,
        fromDate: Instant?,
        toDate: Instant?
    ): Map<String, Any> {
        TODO("Not yet implemented")
    }

    override suspend fun exportLogs(
        organizationId: OrganizationId,
        fromDate: Instant,
        toDate: Instant,
        format: String
    ): ByteArray {
        TODO("Not yet implemented")
    }

    override suspend fun search(
        organizationId: OrganizationId,
        searchQuery: String,
        limit: Int?
    ): List<AuditLog> {
        TODO("Not yet implemented")
    }

    override suspend fun getLatestForEntity(entityType: EntityType, entityId: Uuid): AuditLog? {
        TODO("Not yet implemented")
    }

    override suspend fun archiveLogs(organizationId: OrganizationId, olderThan: Instant): Int {
        TODO("Not yet implemented")
    }
}
