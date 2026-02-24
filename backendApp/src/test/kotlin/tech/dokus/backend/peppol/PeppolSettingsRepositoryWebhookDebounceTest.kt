package tech.dokus.backend.peppol

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.database.tables.peppol.PeppolSettingsTable
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
class PeppolSettingsRepositoryWebhookDebounceTest {

    private lateinit var database: Database
    private lateinit var repository: PeppolSettingsRepository
    private lateinit var tenantUuid: UUID
    private val tenantId: TenantId get() = TenantId(tenantUuid.toKotlinUuid())

    @BeforeEach
    fun setup() {
        database = Database.connect(
            url = "jdbc:h2:mem:test_${System.currentTimeMillis()};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
            user = "sa",
            password = ""
        )

        transaction(database) {
            SchemaUtils.create(TenantTable, PeppolSettingsTable)
        }

        tenantUuid = UUID.randomUUID()
        transaction(database) {
            TenantTable.insert {
                it[id] = tenantUuid
                it[type] = TenantType.Company
                it[legalName] = "Webhook Tenant"
                it[displayName] = "Webhook Tenant"
                it[plan] = SubscriptionTier.Core
                it[status] = TenantStatus.Active
                it[language] = Language.En
                it[vatNumber] = "BE0123456789"
            }
        }

        repository = PeppolSettingsRepository()

        runBlocking {
            repository.saveSettings(
                tenantId = tenantId,
                companyId = "company-1",
                peppolId = "0208:BE0123456789",
                isEnabled = true,
                testMode = true
            ).getOrThrow()
        }
    }

    @AfterEach
    fun teardown() {
        transaction(database) {
            SchemaUtils.drop(PeppolSettingsTable, TenantTable)
        }
    }

    @Test
    fun `tryAcquireWebhookPollSlot debounces within configured window`() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val first = repository.tryAcquireWebhookPollSlot(
            tenantId = tenantId,
            now = now,
            debounceSeconds = 60
        ).getOrThrow()

        val second = repository.tryAcquireWebhookPollSlot(
            tenantId = tenantId,
            now = now,
            debounceSeconds = 60
        ).getOrThrow()

        val later = (now.toInstant(TimeZone.UTC) + 61.seconds).toLocalDateTime(TimeZone.UTC)
        val third = repository.tryAcquireWebhookPollSlot(
            tenantId = tenantId,
            now = later,
            debounceSeconds = 60
        ).getOrThrow()

        assertTrue(first)
        assertFalse(second)
        assertTrue(third)
    }

    @Test
    fun `tryAcquireWebhookPollSlot only allows one concurrent winner`() = runBlocking {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val results = coroutineScope {
            listOf(
                async {
                    repository.tryAcquireWebhookPollSlot(
                        tenantId = tenantId,
                        now = now,
                        debounceSeconds = 60
                    ).getOrThrow()
                },
                async {
                    repository.tryAcquireWebhookPollSlot(
                        tenantId = tenantId,
                        now = now,
                        debounceSeconds = 60
                    ).getOrThrow()
                }
            ).awaitAll()
        }

        assertEquals(1, results.count { it })
        assertEquals(1, results.count { !it })
    }

    @Test
    fun `getEnabledSettingsByCompanyId only returns enabled settings`() = runBlocking {
        val enabled = repository.getEnabledSettingsByCompanyId("company-1").getOrThrow()
        assertNotNull(enabled)

        repository.saveSettings(
            tenantId = tenantId,
            companyId = "company-1",
            peppolId = "0208:BE0123456789",
            isEnabled = false,
            testMode = true
        ).getOrThrow()

        val disabled = repository.getEnabledSettingsByCompanyId("company-1").getOrThrow()
        assertTrue(disabled == null)
    }
}
