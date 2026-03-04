package tech.dokus.database.repository.enrichment

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.enrichment.BusinessDescriptionsTable
import tech.dokus.domain.enums.EnrichmentEntityType
import tech.dokus.domain.enums.EnrichmentStatus
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

/**
 * Data class representing a business description row.
 */
data class BusinessDescriptionRow(
    val id: UUID,
    val tenantId: UUID,
    val entityType: EnrichmentEntityType,
    val entityId: UUID,
    val websiteUrl: String?,
    val summary: String?,
    val activities: String?,
    val enrichmentStatus: EnrichmentStatus
)

/**
 * Repository for managing business description enrichment records.
 *
 * CRITICAL: All queries MUST filter by tenant_id for multi-tenant isolation.
 */
@OptIn(ExperimentalUuidApi::class)
class BusinessDescriptionRepository {
    private val logger = loggerFor()

    /**
     * Create a PENDING enrichment record. Skips if one already exists for the entity.
     * @return the record ID, or null if it already existed
     */
    suspend fun createPending(
        tenantId: TenantId,
        entityType: EnrichmentEntityType,
        entityId: UUID
    ): UUID? = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()

        // Check if already exists
        val existing = BusinessDescriptionsTable.selectAll().where {
            (BusinessDescriptionsTable.entityType eq entityType) and
                (BusinessDescriptionsTable.entityId eq entityId)
        }.singleOrNull()

        if (existing != null) {
            logger.info("Enrichment record already exists for $entityType:$entityId")
            return@dbQuery null
        }

        val id = UUID.randomUUID()
        BusinessDescriptionsTable.insert {
            it[BusinessDescriptionsTable.id] = id
            it[BusinessDescriptionsTable.tenantId] = tenantUuid
            it[BusinessDescriptionsTable.entityType] = entityType
            it[BusinessDescriptionsTable.entityId] = entityId
            it[enrichmentStatus] = EnrichmentStatus.Pending
        }

        logger.info("Created PENDING enrichment for $entityType:$entityId (tenant=$tenantId)")
        id
    }

    /**
     * Find an enrichment record by entity.
     * CRITICAL: Filters by tenant_id.
     */
    suspend fun findByEntity(
        entityType: EnrichmentEntityType,
        entityId: UUID,
        tenantId: TenantId
    ): BusinessDescriptionRow? = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        BusinessDescriptionsTable.selectAll().where {
            (BusinessDescriptionsTable.tenantId eq tenantUuid) and
                (BusinessDescriptionsTable.entityType eq entityType) and
                (BusinessDescriptionsTable.entityId eq entityId)
        }.singleOrNull()?.let { row ->
            mapRow(row)
        }
    }

    /**
     * Update the enrichment status of a record.
     * CRITICAL: Filters by tenant_id.
     */
    suspend fun updateStatus(
        id: UUID,
        tenantId: TenantId,
        status: EnrichmentStatus
    ): Unit = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        BusinessDescriptionsTable.update({
            (BusinessDescriptionsTable.id eq id) and
                (BusinessDescriptionsTable.tenantId eq tenantUuid)
        }) {
            it[enrichmentStatus] = status
        }
    }

    /**
     * Mark enrichment as completed with results.
     * CRITICAL: Filters by tenant_id.
     */
    suspend fun markCompleted(
        id: UUID,
        tenantId: TenantId,
        websiteUrl: String?,
        summary: String?,
        activities: String?
    ): Unit = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        BusinessDescriptionsTable.update({
            (BusinessDescriptionsTable.id eq id) and
                (BusinessDescriptionsTable.tenantId eq tenantUuid)
        }) {
            it[BusinessDescriptionsTable.websiteUrl] = websiteUrl
            it[BusinessDescriptionsTable.summary] = summary
            it[BusinessDescriptionsTable.activities] = activities
            it[enrichmentStatus] = EnrichmentStatus.Completed
        }
        logger.info("Marked enrichment $id as COMPLETED (tenant=$tenantId)")
    }

    /**
     * Atomically select PENDING records and mark them as IN_PROGRESS.
     * Returns the records that were claimed for processing.
     */
    suspend fun findPendingForProcessing(limit: Int = 5): List<BusinessDescriptionRow> = dbQuery {
        val pending = BusinessDescriptionsTable.selectAll().where {
            BusinessDescriptionsTable.enrichmentStatus eq EnrichmentStatus.Pending
        }.limit(limit).map { row -> mapRow(row) }

        // Mark them as IN_PROGRESS
        pending.forEach { record ->
            BusinessDescriptionsTable.update({
                (BusinessDescriptionsTable.id eq record.id) and
                    (BusinessDescriptionsTable.enrichmentStatus eq EnrichmentStatus.Pending)
            }) {
                it[enrichmentStatus] = EnrichmentStatus.InProgress
            }
        }

        pending
    }

    /**
     * Mark enrichment as failed.
     * CRITICAL: Filters by tenant_id.
     */
    suspend fun markFailed(id: UUID, tenantId: TenantId): Unit = dbQuery {
        val tenantUuid = tenantId.value.toJavaUuid()
        BusinessDescriptionsTable.update({
            (BusinessDescriptionsTable.id eq id) and
                (BusinessDescriptionsTable.tenantId eq tenantUuid)
        }) {
            it[enrichmentStatus] = EnrichmentStatus.Failed
        }
        logger.info("Marked enrichment $id as FAILED (tenant=$tenantId)")
    }

    private fun mapRow(row: org.jetbrains.exposed.v1.core.ResultRow): BusinessDescriptionRow {
        return BusinessDescriptionRow(
            id = row[BusinessDescriptionsTable.id].value,
            tenantId = row[BusinessDescriptionsTable.tenantId],
            entityType = row[BusinessDescriptionsTable.entityType],
            entityId = row[BusinessDescriptionsTable.entityId],
            websiteUrl = row[BusinessDescriptionsTable.websiteUrl],
            summary = row[BusinessDescriptionsTable.summary],
            activities = row[BusinessDescriptionsTable.activities],
            enrichmentStatus = row[BusinessDescriptionsTable.enrichmentStatus]
        )
    }
}
