package tech.dokus.database.repository.auth
import kotlin.uuid.Uuid

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
import tech.dokus.database.tables.auth.WelcomeEmailJobsTable
import tech.dokus.database.tables.auth.WelcomeEmailJobsTable.JobStatus
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.foundation.backend.database.dbQuery

data class WelcomeEmailJob(
    val id: Uuid,
    val userId: UserId,
    val tenantId: TenantId,
    val status: JobStatus,
    val scheduledAt: LocalDateTime,
    val nextAttemptAt: LocalDateTime,
    val attemptCount: Int,
    val lastError: String?,
    val sentAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

class WelcomeEmailJobRepository {
    private val claimableStatuses = listOf(JobStatus.Pending, JobStatus.Retry)

    suspend fun enqueueIfAbsent(
        userId: UserId,
        tenantId: TenantId,
        scheduledAt: LocalDateTime
    ): Result<Unit> = runCatching {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val userUuid = Uuid.parse(userId.toString())
        val tenantUuid = Uuid.parse(tenantId.toString())

        dbQuery {
            WelcomeEmailJobsTable.upsert(
                WelcomeEmailJobsTable.userId,
                onUpdate = { stmt ->
                    stmt[WelcomeEmailJobsTable.updatedAt] = now
                }
            ) {
                it[WelcomeEmailJobsTable.userId] = userUuid
                it[WelcomeEmailJobsTable.tenantId] = tenantUuid
                it[WelcomeEmailJobsTable.status] = JobStatus.Pending
                it[WelcomeEmailJobsTable.scheduledAt] = scheduledAt
                it[WelcomeEmailJobsTable.nextAttemptAt] = scheduledAt
                it[WelcomeEmailJobsTable.attemptCount] = 0
                it[WelcomeEmailJobsTable.lastError] = null
                it[WelcomeEmailJobsTable.sentAt] = null
                it[WelcomeEmailJobsTable.updatedAt] = now
                it[WelcomeEmailJobsTable.createdAt] = now
            }
        }
    }

    suspend fun findByUserId(userId: UserId): Result<WelcomeEmailJob?> = runCatching {
        val userUuid = Uuid.parse(userId.toString())
        dbQuery {
            WelcomeEmailJobsTable.selectAll()
                .where { WelcomeEmailJobsTable.userId eq userUuid }
                .singleOrNull()
                ?.toModel()
        }
    }

    suspend fun claimDue(
        now: LocalDateTime,
        limit: Int
    ): Result<List<WelcomeEmailJob>> = runCatching {
        dbQuery {
            val candidates = WelcomeEmailJobsTable.selectAll()
                .where {
                    (WelcomeEmailJobsTable.status inList claimableStatuses) and
                        (WelcomeEmailJobsTable.nextAttemptAt lessEq now)
                }
                .orderBy(WelcomeEmailJobsTable.nextAttemptAt to SortOrder.ASC)
                .limit(limit)
                .map { it.toModel() }

            val claimed = mutableListOf<WelcomeEmailJob>()
            for (candidate in candidates) {
                val updated = WelcomeEmailJobsTable.update({
                    (WelcomeEmailJobsTable.id eq candidate.id) and
                        (WelcomeEmailJobsTable.status inList claimableStatuses) and
                        (WelcomeEmailJobsTable.nextAttemptAt lessEq now)
                }) {
                    it[status] = JobStatus.Processing
                    it[updatedAt] = now
                }
                if (updated > 0) {
                    claimed += candidate.copy(
                        status = JobStatus.Processing,
                        updatedAt = now
                    )
                }
            }

            claimed
        }
    }

    suspend fun markSent(
        jobId: Uuid,
        sentAt: LocalDateTime
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = WelcomeEmailJobsTable.update({
                (WelcomeEmailJobsTable.id eq jobId) and
                    (WelcomeEmailJobsTable.status eq JobStatus.Processing)
            }) {
                it[status] = JobStatus.Sent
                it[WelcomeEmailJobsTable.sentAt] = sentAt
                it[lastError] = null
                it[updatedAt] = sentAt
            }
            updated > 0
        }
    }

    suspend fun scheduleRetry(
        jobId: Uuid,
        attemptCount: Int,
        nextAttemptAt: LocalDateTime,
        errorMessage: String
    ): Result<Boolean> = runCatching {
        dbQuery {
            val updated = WelcomeEmailJobsTable.update({
                (WelcomeEmailJobsTable.id eq jobId) and
                    (WelcomeEmailJobsTable.status eq JobStatus.Processing)
            }) {
                it[status] = JobStatus.Retry
                it[WelcomeEmailJobsTable.attemptCount] = attemptCount
                it[WelcomeEmailJobsTable.nextAttemptAt] = nextAttemptAt
                it[lastError] = errorMessage.take(2000)
                it[updatedAt] = nextAttemptAt
            }
            updated > 0
        }
    }

    suspend fun recoverStaleProcessing(
        staleBefore: LocalDateTime,
        retryAt: LocalDateTime
    ): Result<Int> = runCatching {
        dbQuery {
            WelcomeEmailJobsTable.update({
                (WelcomeEmailJobsTable.status eq JobStatus.Processing) and
                    (WelcomeEmailJobsTable.updatedAt lessEq staleBefore)
            }) {
                it[status] = JobStatus.Retry
                it[nextAttemptAt] = retryAt
                it[lastError] = "Recovered stale processing lease"
                it[updatedAt] = retryAt
            }
        }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toModel(): WelcomeEmailJob = WelcomeEmailJob(
        id = this[WelcomeEmailJobsTable.id].value,
        userId = UserId(this[WelcomeEmailJobsTable.userId]),
        tenantId = TenantId(this[WelcomeEmailJobsTable.tenantId]),
        status = this[WelcomeEmailJobsTable.status],
        scheduledAt = this[WelcomeEmailJobsTable.scheduledAt],
        nextAttemptAt = this[WelcomeEmailJobsTable.nextAttemptAt],
        attemptCount = this[WelcomeEmailJobsTable.attemptCount],
        lastError = this[WelcomeEmailJobsTable.lastError],
        sentAt = this[WelcomeEmailJobsTable.sentAt],
        createdAt = this[WelcomeEmailJobsTable.createdAt],
        updatedAt = this[WelcomeEmailJobsTable.updatedAt]
    )
}
