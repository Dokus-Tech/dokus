package tech.dokus.backend.auth
import kotlin.uuid.Uuid

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.auth.WelcomeEmailJobRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.auth.UsersTable
import tech.dokus.database.tables.auth.WelcomeEmailJobsTable
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class WelcomeEmailJobRepositoryTest {

    private lateinit var database: Database
    private lateinit var repository: WelcomeEmailJobRepository

    private lateinit var tenantUuid: Uuid
    private lateinit var userUuid: Uuid
    private var tenantId: TenantId = TenantId.generate()
    private var userId: UserId = UserId.generate()

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_welcome_jobs_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                UsersTable,
                WelcomeEmailJobsTable
            )
        }

        tenantUuid = Uuid.random()
        userUuid = Uuid.random()
        tenantId = TenantId(tenantUuid)
        userId = UserId(userUuid)

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Welcome Test Tenant"
                it[displayName] = "Welcome Test Tenant"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE0123456789"
            }

            UsersTable.insert {
                it[id] = userUuid
                it[email] = "welcome@test.dokus"
                it[passwordHash] = "hash"
                it[firstName] = "Welcome"
                it[lastName] = "Tester"
                it[isActive] = true
            }
        }

        repository = WelcomeEmailJobRepository()
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                WelcomeEmailJobsTable,
                UsersTable,
                TenantTable
            )
        }
    }

    @Test
    fun `enqueue if absent is concurrency safe`() = runBlocking {
        val scheduledAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        coroutineScope {
            launch {
                repository.enqueueIfAbsent(userId, tenantId, scheduledAt).getOrThrow()
            }
            launch {
                repository.enqueueIfAbsent(userId, tenantId, scheduledAt).getOrThrow()
            }
        }

        val job = repository.findByUserId(userId).getOrThrow()
        assertNotNull(job)
        assertEquals(WelcomeEmailJobsTable.JobStatus.Pending, job.status)
    }

    @Test
    fun `job lifecycle supports claim retry and sent`() = runBlocking {
        val now = Clock.System.now()
        val scheduledAt = (now - 2.minutes).toLocalDateTime(TimeZone.UTC)
        repository.enqueueIfAbsent(userId, tenantId, scheduledAt).getOrThrow()

        val claimed = repository.claimDue(
            now = now.toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertEquals(1, claimed.size)

        val job = claimed.single()
        val retryAt = (now + 5.minutes).toLocalDateTime(TimeZone.UTC)
        val retryScheduled = repository.scheduleRetry(
            jobId = job.id,
            attemptCount = 1,
            nextAttemptAt = retryAt,
            errorMessage = "Transient failure"
        ).getOrThrow()
        assertTrue(retryScheduled)

        val secondClaimBeforeRetry = repository.claimDue(
            now = (now + 1.minutes).toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertTrue(secondClaimBeforeRetry.isEmpty())

        val secondClaim = repository.claimDue(
            now = (now + 6.minutes).toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertEquals(1, secondClaim.size)

        val markedSent = repository.markSent(
            jobId = secondClaim.single().id,
            sentAt = (now + 6.minutes).toLocalDateTime(TimeZone.UTC)
        ).getOrThrow()
        assertTrue(markedSent)

        val afterSent = repository.claimDue(
            now = (now + 1.hours).toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertTrue(afterSent.isEmpty())
    }

    @Test
    fun `stale processing jobs are recovered back to retry`() = runBlocking {
        val now = Clock.System.now()
        val dueAt = (now - 1.minutes).toLocalDateTime(TimeZone.UTC)
        repository.enqueueIfAbsent(userId, tenantId, dueAt).getOrThrow()

        val claimed = repository.claimDue(
            now = now.toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertEquals(1, claimed.size)

        val recovered = repository.recoverStaleProcessing(
            staleBefore = (now + 1.minutes).toLocalDateTime(TimeZone.UTC),
            retryAt = (now + 2.minutes).toLocalDateTime(TimeZone.UTC)
        ).getOrThrow()
        assertEquals(1, recovered)

        val claimAfterRecovery = repository.claimDue(
            now = (now + 3.minutes).toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertEquals(1, claimAfterRecovery.size)

        val markSent = repository.markSent(
            jobId = claimAfterRecovery.single().id,
            sentAt = (now + 4.minutes).toLocalDateTime(TimeZone.UTC)
        ).getOrThrow()
        assertTrue(markSent)

        val finalJob = repository.findByUserId(userId).getOrThrow()
        assertNotNull(finalJob)
        assertEquals(WelcomeEmailJobsTable.JobStatus.Sent, finalJob.status)
        assertNull(
            repository.claimDue(
                now = (now + 5.minutes).toLocalDateTime(TimeZone.UTC),
                limit = 1
            ).getOrThrow().firstOrNull()
        )
    }
}
