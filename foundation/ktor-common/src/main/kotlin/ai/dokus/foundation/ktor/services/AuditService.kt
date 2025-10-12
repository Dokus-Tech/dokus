package ai.dokus.foundation.ktor.services

import ai.dokus.foundation.domain.AuditLogId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.model.AuditLog
import kotlinx.datetime.Instant
import kotlinx.rpc.RPC
import kotlin.uuid.Uuid

@RPC
interface AuditService {
    /**
     * Logs an audit event
     * Creates an immutable audit record for compliance and debugging
     *
     * @param tenantId The tenant's unique identifier
     * @param userId The user who performed the action (optional for system actions)
     * @param action The action performed (Created, Updated, Deleted, etc.)
     * @param entityType The type of entity affected
     * @param entityId The unique identifier of the affected entity
     * @param oldValues The previous state as JSON (optional, for updates/deletes)
     * @param newValues The new state as JSON (optional, for creates/updates)
     * @param ipAddress The IP address of the request (optional)
     * @param userAgent The user agent of the request (optional)
     * @return The created audit log
     */
    suspend fun log(
        tenantId: TenantId,
        userId: UserId? = null,
        action: AuditAction,
        entityType: EntityType,
        entityId: Uuid,
        oldValues: String? = null,
        newValues: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null
    ): AuditLog

    /**
     * Lists audit logs for a specific entity
     * Returns the complete history of changes for an entity
     *
     * @param entityType The type of entity
     * @param entityId The unique identifier of the entity
     * @return List of audit logs ordered by timestamp (newest first)
     */
    suspend fun listByEntity(entityType: EntityType, entityId: Uuid): List<AuditLog>

    /**
     * Lists audit logs for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param action Filter by action (optional)
     * @param entityType Filter by entity type (optional)
     * @param userId Filter by user (optional)
     * @param fromDate Filter logs from this timestamp (optional)
     * @param toDate Filter logs until this timestamp (optional)
     * @param limit Maximum number of results (optional)
     * @param offset Pagination offset (optional)
     * @return List of audit logs ordered by timestamp (newest first)
     */
    suspend fun listByTenant(
        tenantId: TenantId,
        action: AuditAction? = null,
        entityType: EntityType? = null,
        userId: UserId? = null,
        fromDate: Instant? = null,
        toDate: Instant? = null,
        limit: Int? = null,
        offset: Int? = null
    ): List<AuditLog>

    /**
     * Lists audit logs for a specific user
     * Returns all actions performed by a user
     *
     * @param userId The user's unique identifier
     * @param fromDate Filter logs from this timestamp (optional)
     * @param toDate Filter logs until this timestamp (optional)
     * @param limit Maximum number of results (optional)
     * @return List of audit logs ordered by timestamp (newest first)
     */
    suspend fun listByUser(
        userId: UserId,
        fromDate: Instant? = null,
        toDate: Instant? = null,
        limit: Int? = null
    ): List<AuditLog>

    /**
     * Lists audit logs for a specific action
     * Useful for tracking specific operations (e.g., all deletions)
     *
     * @param tenantId The tenant's unique identifier
     * @param action The action to filter by
     * @param fromDate Filter logs from this timestamp (optional)
     * @param toDate Filter logs until this timestamp (optional)
     * @param limit Maximum number of results (optional)
     * @return List of audit logs ordered by timestamp (newest first)
     */
    suspend fun listByAction(
        tenantId: TenantId,
        action: AuditAction,
        fromDate: Instant? = null,
        toDate: Instant? = null,
        limit: Int? = null
    ): List<AuditLog>

    /**
     * Finds an audit log by its unique ID
     *
     * @param id The audit log's unique identifier
     * @return The audit log if found, null otherwise
     */
    suspend fun findById(id: AuditLogId): AuditLog?

    /**
     * Gets audit statistics for a tenant
     *
     * @param tenantId The tenant's unique identifier
     * @param fromDate Start date for statistics (optional)
     * @param toDate End date for statistics (optional)
     * @return Map of statistics (totalActions, byAction, byEntityType, byUser, etc.)
     */
    suspend fun getStatistics(
        tenantId: TenantId,
        fromDate: Instant? = null,
        toDate: Instant? = null
    ): Map<String, Any>

    /**
     * Exports audit logs for compliance reporting
     * Generates a report for a specific time period
     *
     * @param tenantId The tenant's unique identifier
     * @param fromDate Start date for export
     * @param toDate End date for export
     * @param format The export format (CSV, JSON, PDF)
     * @return The export content as ByteArray
     */
    suspend fun exportLogs(
        tenantId: TenantId,
        fromDate: Instant,
        toDate: Instant,
        format: String = "CSV"
    ): ByteArray

    /**
     * Searches audit logs by content
     * Searches in old and new values JSON
     *
     * @param tenantId The tenant's unique identifier
     * @param searchQuery The search query
     * @param limit Maximum number of results (optional)
     * @return List of matching audit logs
     */
    suspend fun search(
        tenantId: TenantId,
        searchQuery: String,
        limit: Int? = null
    ): List<AuditLog>

    /**
     * Gets the most recent audit log for an entity
     *
     * @param entityType The type of entity
     * @param entityId The unique identifier of the entity
     * @return The most recent audit log, or null if none exist
     */
    suspend fun getLatestForEntity(entityType: EntityType, entityId: Uuid): AuditLog?

    /**
     * Archives old audit logs
     * Moves logs older than retention period to archive storage
     * Typically 7 years for financial records
     *
     * @param tenantId The tenant's unique identifier
     * @param olderThan Archive logs older than this timestamp
     * @return Number of logs archived
     */
    suspend fun archiveLogs(tenantId: TenantId, olderThan: Instant): Int
}
