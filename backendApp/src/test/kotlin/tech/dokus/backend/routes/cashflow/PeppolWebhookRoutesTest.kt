package tech.dokus.backend.routes.cashflow

import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.serialization.kotlinx.json.json
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import tech.dokus.backend.worker.PeppolPollingWorker
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolSettingsId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.utils.json
import tech.dokus.peppol.config.InboxConfig
import tech.dokus.peppol.config.MasterCredentialsConfig
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.config.WebhookConfig
import tech.dokus.peppol.service.PeppolService
import kotlin.test.assertEquals

class PeppolWebhookRoutesTest {

    @Test
    fun `valid company and token triggers reconciliation and poll`() = runBlocking {
        val settingsRepository = mockk<PeppolSettingsRepository>()
        val peppolPollingWorker = mockk<PeppolPollingWorker>()
        val peppolService = mockk<PeppolService>()
        val settings = testSettings()

        coEvery { settingsRepository.getEnabledSettingsByCompanyId("company-1") } returns Result.success(settings)
        coEvery { settingsRepository.tryAcquireWebhookPollSlot(settings.tenantId, any(), 60) } returns Result.success(true)
        coEvery { peppolService.reconcileOutboundByExternalDocumentId(settings.tenantId, "doc-1") } returns Result.success(true)
        coEvery { peppolPollingWorker.pollNow(settings.tenantId) } returns true

        webhookTestApplication(settingsRepository, peppolPollingWorker, peppolService) {
            val response = client.post("/api/v1/peppol/webhook?token=token-1") {
                contentType(ContentType.Application.Json)
                setBody("""{"companyId":"company-1","documentId":"doc-1","eventType":"document.updated"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) { settingsRepository.getEnabledSettingsByCompanyId("company-1") }
        coVerify(exactly = 1) { peppolService.reconcileOutboundByExternalDocumentId(settings.tenantId, "doc-1") }
        coVerify(exactly = 1) { settingsRepository.tryAcquireWebhookPollSlot(settings.tenantId, any(), 60) }
        coVerify(exactly = 1) { peppolPollingWorker.pollNow(settings.tenantId) }
    }

    @Test
    fun `wrong token returns unauthorized and does nothing`() = runBlocking {
        val settingsRepository = mockk<PeppolSettingsRepository>()
        val peppolPollingWorker = mockk<PeppolPollingWorker>(relaxed = true)
        val peppolService = mockk<PeppolService>(relaxed = true)
        val settings = testSettings()

        coEvery { settingsRepository.getEnabledSettingsByCompanyId("company-1") } returns Result.success(settings)

        webhookTestApplication(settingsRepository, peppolPollingWorker, peppolService) {
            val response = client.post("/api/v1/peppol/webhook?token=wrong-token") {
                contentType(ContentType.Application.Json)
                setBody("""{"companyId":"company-1","documentId":"doc-1","eventType":"document.updated"}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        coVerify(exactly = 1) { settingsRepository.getEnabledSettingsByCompanyId("company-1") }
        coVerify(exactly = 0) { settingsRepository.tryAcquireWebhookPollSlot(any(), any(), any()) }
        coVerify(exactly = 0) { peppolService.reconcileOutboundByExternalDocumentId(any(), any()) }
        coVerify(exactly = 0) { peppolPollingWorker.pollNow(any()) }
    }

    @Test
    fun `missing token returns unauthorized`() = runBlocking {
        val settingsRepository = mockk<PeppolSettingsRepository>(relaxed = true)
        val peppolPollingWorker = mockk<PeppolPollingWorker>(relaxed = true)
        val peppolService = mockk<PeppolService>(relaxed = true)

        webhookTestApplication(settingsRepository, peppolPollingWorker, peppolService) {
            val response = client.post("/api/v1/peppol/webhook") {
                contentType(ContentType.Application.Json)
                setBody("""{"companyId":"company-1"}""")
            }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

        coVerify(exactly = 0) { settingsRepository.getEnabledSettingsByCompanyId(any()) }
        coVerify(exactly = 0) { peppolService.reconcileOutboundByExternalDocumentId(any(), any()) }
        coVerify(exactly = 0) { peppolPollingWorker.pollNow(any()) }
    }

    @Test
    fun `unknown companyId is acknowledged and ignored`() = runBlocking {
        val settingsRepository = mockk<PeppolSettingsRepository>()
        val peppolPollingWorker = mockk<PeppolPollingWorker>(relaxed = true)
        val peppolService = mockk<PeppolService>(relaxed = true)

        coEvery { settingsRepository.getEnabledSettingsByCompanyId("unknown-company") } returns Result.success(null)

        webhookTestApplication(settingsRepository, peppolPollingWorker, peppolService) {
            val response = client.post("/api/v1/peppol/webhook?token=any-token") {
                contentType(ContentType.Application.Json)
                setBody("""{"companyId":"unknown-company","documentId":"doc-1"}""")
            }

            assertEquals(HttpStatusCode.OK, response.status)
        }

        coVerify(exactly = 1) { settingsRepository.getEnabledSettingsByCompanyId("unknown-company") }
        coVerify(exactly = 0) { settingsRepository.tryAcquireWebhookPollSlot(any(), any(), any()) }
        coVerify(exactly = 0) { peppolService.reconcileOutboundByExternalDocumentId(any(), any()) }
        coVerify(exactly = 0) { peppolPollingWorker.pollNow(any()) }
    }

    private fun webhookTestApplication(
        settingsRepository: PeppolSettingsRepository,
        peppolPollingWorker: PeppolPollingWorker,
        peppolService: PeppolService,
        testBlock: suspend ApplicationTestBuilder.() -> Unit
    ) = testApplication {
        application {
            configureWebhookRouteTestApp(
                settingsRepository = settingsRepository,
                peppolPollingWorker = peppolPollingWorker,
                peppolService = peppolService
            )
        }
        testBlock()
    }

    private fun Application.configureWebhookRouteTestApp(
        settingsRepository: PeppolSettingsRepository,
        peppolPollingWorker: PeppolPollingWorker,
        peppolService: PeppolService
    ) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Koin) {
            modules(
                module {
                    single { settingsRepository }
                    single { peppolPollingWorker }
                    single { peppolService }
                    single { testModuleConfig() }
                }
            )
        }
        routing {
            peppolWebhookRoutes()
        }
    }

    private fun testModuleConfig() = PeppolModuleConfig(
        defaultProvider = "recommand",
        inbox = InboxConfig(
            pollingEnabled = true,
            pollingIntervalSeconds = 600
        ),
        globalTestMode = false,
        masterCredentials = MasterCredentialsConfig(
            apiKey = "api-key",
            apiSecret = "api-secret"
        ),
        webhook = WebhookConfig(
            publicBaseUrl = "https://dokus.invoid.vision",
            callbackPath = "/api/v1/peppol/webhook",
            pollDebounceSeconds = 60
        )
    )

    private fun testSettings(): PeppolSettingsDto {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return PeppolSettingsDto(
            id = PeppolSettingsId.generate(),
            tenantId = TenantId.generate(),
            companyId = "company-1",
            peppolId = PeppolId("0208:BE0123456789"),
            isEnabled = true,
            testMode = false,
            webhookToken = "token-1",
            lastFullSyncAt = null,
            createdAt = now,
            updatedAt = now
        )
    }
}
