@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.database.repository.business

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert
import tech.dokus.database.mapper.toBusinessProfileEnrichmentJob
import tech.dokus.database.tables.business.BusinessProfileEnrichmentJobsTable
import tech.dokus.domain.enums.BusinessProfileEnrichmentJobStatus
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

data class BusinessProfileEnrichmentJob(
    val id: Uuid,
    val tenantId: TenantId,
    val subjectType: BusinessProfileSubjectType,
    val subjectId: Uuid,
    val status: BusinessProfileEnrichmentJobStatus,
    val triggerReason: String,
    val scheduledAt: LocalDateTime,
    val nextAttemptAt: LocalDateTime,
    val attemptCount: Int,
    val lastError: String?,
    val processingStartedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

class BusinessProfileEnrichmentJobRepository {
    private val claimableStatuses = listOf(
        BusinessProfileEnrichmentJobStatus.Pending,
        BusinessProfileEnrichmentJobStatus.Retry
    )

    suspend fun enqueueOrReset(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid,
        triggerReason: String,
    ): Result<Unit> = runSuspendCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            BusinessProfileEnrichmentJobsTable.upsert(
                BusinessProfileEnrichmentJobsTable.tenantId,
                BusinessProfileEnrichmentJobsTable.subjectType,
                BusinessProfileEnrichmentJobsTable.subjectId,
                onUpdate = { stmt ->
                    stmt[BusinessProfileEnrichmentJobsTable.status] = BusinessProfileEnrichmentJobStatus.Pending
                    stmt[BusinessProfileEnrichmentJobsTable.triggerReason] = triggerReason
                    stmt[BusinessProfileEnrichmentJobsTable.scheduledAt] = now
                    stmt[BusinessProfileEnrichmentJobsTable.nextAttemptAt] = now
                    stmt[BusinessProfileEnrichmentJobsTable.attemptCount] = 0
                    stmt[BusinessProfileEnrichmentJobsTable.lastError] = null
                    stmt[BusinessProfileEnrichmentJobsTable.processingStartedAt] = null
                    stmt[BusinessProfileEnrichmentJobsTable.updatedAt] = now
                }
            ) {
                it[id] = UUID.randomUUID()
                it[BusinessProfileEnrichmentJobsTable.tenantId] = tenantId.value.toJavaUuid()
                it[BusinessProfileEnrichmentJobsTable.subjectType] = subjectType
                it[BusinessProfileEnrichmentJobsTable.subjectId] = subjectId.toJavaUuid()
                it[BusinessProfileEnrichmentJobsTable.status] = BusinessProfileEnrichmentJobStatus.Pending
                it[BusinessProfileEnrichmentJobsTable.triggerReason] = triggerReason
                it[BusinessProfileEnrichmentJobsTable.scheduledAt] = now
                it[BusinessProfileEnrichmentJobsTable.nextAttemptAt] = now
                it[BusinessProfileEnrichmentJobsTable.attemptCount] = 0
                it[BusinessProfileEnrichmentJobsTable.lastError] = null
                it[BusinessProfileEnrichmentJobsTable.processingStartedAt] = null
                it[BusinessProfileEnrichmentJobsTable.createdAt] = now
                it[BusinessProfileEnrichmentJobsTable.updatedAt] = now
            }
        }
    }

    suspend fun claimDue(now: LocalDateTime, limit: Int): Result<List<BusinessProfileEnrichmentJob>> = runSuspendCatching {
        dbQuery {
            val candidates = BusinessProfileEnrichmentJobsTable
                .selectAll()
                .where {
                    (BusinessProfileEnrichmentJobsTable.status inList claimableStatuses) and
                        (BusinessProfileEnrichmentJobsTable.nextAttemptAt lessEq now)
                }
                .orderBy(BusinessProfileEnrichmentJobsTable.nextAttemptAt to SortOrder.ASC)
                .limit(limit)
                .map { it.toBusinessProfileEnrichmentJob() }

            val claimed = mutableListOf<BusinessProfileEnrichmentJob>()
            for (candidate in candidates) {
                val updated = BusinessProfileEnrichmentJobsTable.update({
                    (BusinessProfileEnrichmentJobsTable.id eq candidate.id.toJavaUuid()) and
                        (BusinessProfileEnrichmentJobsTable.status inList claimableStatuses) and
                        (BusinessProfileEnrichmentJobsTable.nextAttemptAt lessEq now)
                }) {
                    it[BusinessProfileEnrichmentJobsTable.status] = BusinessProfileEnrichmentJobStatus.Processing
                    it[BusinessProfileEnrichmentJobsTable.processingStartedAt] = now
                    it[BusinessProfileEnrichmentJobsTable.updatedAt] = now
                }
                if (updated > 0) {
                    claimed += candidate.copy(
                        status = BusinessProfileEnrichmentJobStatus.Processing,
                        processingStartedAt = now,
                        updatedAt = now
                    )
                }
            }
            claimed
        }
    }

    suspend fun markCompleted(jobId: Uuid): Result<Boolean> = runSuspendCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            BusinessProfileEnrichmentJobsTable.update({
                (BusinessProfileEnrichmentJobsTable.id eq jobId.toJavaUuid()) and
                    (BusinessProfileEnrichmentJobsTable.status eq BusinessProfileEnrichmentJobStatus.Processing)
            }) {
                it[BusinessProfileEnrichmentJobsTable.status] = BusinessProfileEnrichmentJobStatus.Completed
                it[BusinessProfileEnrichmentJobsTable.processingStartedAt] = null
                it[BusinessProfileEnrichmentJobsTable.updatedAt] = now
            } > 0
        }
    }

    suspend fun markCompletedWithError(jobId: Uuid, error: String): Result<Boolean> = runSuspendCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        dbQuery {
            BusinessProfileEnrichmentJobsTable.update({
                (BusinessProfileEnrichmentJobsTable.id eq jobId.toJavaUuid()) and
                    (BusinessProfileEnrichmentJobsTable.status eq BusinessProfileEnrichmentJobStatus.Processing)
            }) {
                it[BusinessProfileEnrichmentJobsTable.status] = BusinessProfileEnrichmentJobStatus.Completed
                it[BusinessProfileEnrichmentJobsTable.lastError] = error.take(2000)
                it[BusinessProfileEnrichmentJobsTable.processingStartedAt] = null
                it[BusinessProfileEnrichmentJobsTable.updatedAt] = now
            } > 0
        }
    }

    suspend fun scheduleRetry(
        jobId: Uuid,
        attemptCount: Int,
        nextAttemptAt: LocalDateTime,
        error: String
    ): Result<Boolean> = runSuspendCatching {
        dbQuery {
            BusinessProfileEnrichmentJobsTable.update({
                (BusinessProfileEnrichmentJobsTable.id eq jobId.toJavaUuid()) and
                    (BusinessProfileEnrichmentJobsTable.status eq BusinessProfileEnrichmentJobStatus.Processing)
            }) {
                it[BusinessProfileEnrichmentJobsTable.status] = BusinessProfileEnrichmentJobStatus.Retry
                it[BusinessProfileEnrichmentJobsTable.attemptCount] = attemptCount
                it[BusinessProfileEnrichmentJobsTable.nextAttemptAt] = nextAttemptAt
                it[BusinessProfileEnrichmentJobsTable.lastError] = error.take(2000)
                it[BusinessProfileEnrichmentJobsTable.processingStartedAt] = null
                it[BusinessProfileEnrichmentJobsTable.updatedAt] = nextAttemptAt
            } > 0
        }
    }

    suspend fun recoverStaleProcessing(
        staleBefore: LocalDateTime,
        retryAt: LocalDateTime,
        reason: String = "Recovered stale processing lease"
    ): Result<Int> = runSuspendCatching {
        dbQuery {
            BusinessProfileEnrichmentJobsTable.update({
                (BusinessProfileEnrichmentJobsTable.status eq BusinessProfileEnrichmentJobStatus.Processing) and
                    (BusinessProfileEnrichmentJobsTable.updatedAt lessEq staleBefore)
            }) {
                it[BusinessProfileEnrichmentJobsTable.status] = BusinessProfileEnrichmentJobStatus.Retry
                it[BusinessProfileEnrichmentJobsTable.nextAttemptAt] = retryAt
                it[BusinessProfileEnrichmentJobsTable.lastError] = reason
                it[BusinessProfileEnrichmentJobsTable.processingStartedAt] = null
                it[BusinessProfileEnrichmentJobsTable.updatedAt] = retryAt
            }
        }
    }

}
