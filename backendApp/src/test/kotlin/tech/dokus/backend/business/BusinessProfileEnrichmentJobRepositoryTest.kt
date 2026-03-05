@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.backend.business

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.business.BusinessProfileEnrichmentJobRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.business.BusinessProfileEnrichmentJobsTable
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

class BusinessProfileEnrichmentJobRepositoryTest {
    private lateinit var database: Database
    private lateinit var repository: BusinessProfileEnrichmentJobRepository

    private lateinit var tenantUuid: UUID
    private var tenantId: TenantId = TenantId.generate()

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_business_jobs_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(
                TenantTable,
                BusinessProfileEnrichmentJobsTable
            )
        }

        tenantUuid = UUID.randomUUID()
        tenantId = TenantId(tenantUuid.toKotlinUuid())

        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Acme Logistics"
                it[displayName] = "Acme"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE0123456789"
            }
        }

        repository = BusinessProfileEnrichmentJobRepository()
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(
                BusinessProfileEnrichmentJobsTable,
                TenantTable
            )
        }
    }

    @Test
    fun `enqueueOrReset dedupes same subject into one active row`() = runBlocking {
        repository.enqueueOrReset(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Tenant,
            subjectId = tenantId.value,
            triggerReason = "TENANT_CREATED"
        ).getOrThrow()
        repository.enqueueOrReset(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Tenant,
            subjectId = tenantId.value,
            triggerReason = "TENANT_ADDRESS_UPDATED"
        ).getOrThrow()

        val claimed = repository.claimDue(
            now = (Clock.System.now() + 1.minutes).toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()

        assertEquals(1, claimed.size)
        assertEquals("TENANT_ADDRESS_UPDATED", claimed.single().triggerReason)
        assertEquals(0, claimed.single().attemptCount)
    }

    @Test
    fun `retry lifecycle reschedules and claims when next attempt is due`() = runBlocking {
        repository.enqueueOrReset(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Tenant,
            subjectId = tenantId.value,
            triggerReason = "TENANT_CREATED"
        ).getOrThrow()

        val now = Clock.System.now()
        val firstClaim = repository.claimDue(
            now = now.toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertEquals(1, firstClaim.size)

        val retryAt = (now + 5.minutes).toLocalDateTime(TimeZone.UTC)
        val scheduled = repository.scheduleRetry(
            jobId = firstClaim.single().id,
            attemptCount = 1,
            nextAttemptAt = retryAt,
            error = "Transient network issue"
        ).getOrThrow()
        assertTrue(scheduled)

        val beforeDue = repository.claimDue(
            now = (now + 2.minutes).toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertTrue(beforeDue.isEmpty())

        val secondClaim = repository.claimDue(
            now = (now + 6.minutes).toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertEquals(1, secondClaim.size)
    }

    @Test
    fun `stale processing jobs are recovered to retry`() = runBlocking {
        repository.enqueueOrReset(
            tenantId = tenantId,
            subjectType = BusinessProfileSubjectType.Tenant,
            subjectId = tenantId.value,
            triggerReason = "TENANT_CREATED"
        ).getOrThrow()

        val now = Clock.System.now()
        val claimed = repository.claimDue(
            now = now.toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertEquals(1, claimed.size)

        val recovered = repository.recoverStaleProcessing(
            staleBefore = (now + 1.minutes).toLocalDateTime(TimeZone.UTC),
            retryAt = (now + 2.minutes).toLocalDateTime(TimeZone.UTC),
            reason = "Recovered stale processing lease"
        ).getOrThrow()
        assertEquals(1, recovered)

        val claimAfterRecovery = repository.claimDue(
            now = (now + 3.minutes).toLocalDateTime(TimeZone.UTC),
            limit = 10
        ).getOrThrow()
        assertEquals(1, claimAfterRecovery.size)
    }
}
