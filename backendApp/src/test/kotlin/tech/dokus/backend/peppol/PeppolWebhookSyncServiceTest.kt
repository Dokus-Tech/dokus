package tech.dokus.backend.peppol

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolSettingsId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.peppol.config.InboxConfig
import tech.dokus.peppol.config.MasterCredentialsConfig
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.config.WebhookConfig
import tech.dokus.peppol.provider.client.RecommandWebhooksClient
import tech.dokus.peppol.provider.client.recommand.model.RecommandWebhook
import tech.dokus.peppol.service.PeppolWebhookSyncService
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PeppolWebhookSyncServiceTest {

    private val moduleConfig = PeppolModuleConfig(
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

    @Test
    fun `ensureSingleWebhookForSettings creates webhook when missing`() = runBlocking {
        val settingsRepository = mockk<PeppolSettingsRepository>(relaxed = true)
        val webhooksClient = mockk<RecommandWebhooksClient>()
        val service = PeppolWebhookSyncService(settingsRepository, webhooksClient, moduleConfig)

        val settings = testSettings(
            companyId = "company-1",
            webhookToken = "token-1"
        )
        val expectedUrl = "https://dokus.invoid.vision/api/v1/peppol/webhook?token=token-1"
        val createdWebhook = testWebhook(
            id = "wh-1",
            companyId = "company-1",
            url = expectedUrl,
            createdAt = "2025-01-01T00:00:00Z"
        )

        coEvery {
            webhooksClient.listWebhooks("api-key", "api-secret", "company-1")
        } returnsMany listOf(
            Result.success(emptyList()),
            Result.success(listOf(createdWebhook))
        )
        coEvery {
            webhooksClient.createWebhook("api-key", "api-secret", expectedUrl, "company-1")
        } returns Result.success(createdWebhook)

        val result = service.ensureSingleWebhookForSettings(settings).getOrThrow()

        assertEquals("wh-1", result.id)
        assertEquals(expectedUrl, result.url)

        coVerify(exactly = 1) {
            webhooksClient.createWebhook("api-key", "api-secret", expectedUrl, "company-1")
        }
    }

    @Test
    fun `ensureSingleWebhookForSettings updates keeper and deletes stale webhooks`() = runBlocking {
        val settingsRepository = mockk<PeppolSettingsRepository>(relaxed = true)
        val webhooksClient = mockk<RecommandWebhooksClient>()
        val service = PeppolWebhookSyncService(settingsRepository, webhooksClient, moduleConfig)

        val settings = testSettings(
            companyId = "company-1",
            webhookToken = "token-1"
        )
        val expectedUrl = "https://dokus.invoid.vision/api/v1/peppol/webhook?token=token-1"

        val staleKeeper = testWebhook(
            id = "wh-keep",
            companyId = "company-1",
            url = "https://old.example/webhook",
            createdAt = "2024-01-01T00:00:00Z"
        )
        val staleExtra = testWebhook(
            id = "wh-delete",
            companyId = "company-1",
            url = "https://another.example/webhook",
            createdAt = "2024-06-01T00:00:00Z"
        )
        val normalizedWebhook = staleKeeper.copy(url = expectedUrl)

        coEvery {
            webhooksClient.listWebhooks("api-key", "api-secret", "company-1")
        } returnsMany listOf(
            Result.success(listOf(staleKeeper, staleExtra)),
            Result.success(listOf(normalizedWebhook))
        )
        coEvery {
            webhooksClient.updateWebhook("api-key", "api-secret", "wh-keep", expectedUrl, "company-1")
        } returns Result.success(normalizedWebhook)
        coEvery {
            webhooksClient.deleteWebhook("api-key", "api-secret", "wh-delete")
        } returns Result.success(true)

        val result = service.ensureSingleWebhookForSettings(settings).getOrThrow()

        assertEquals("wh-keep", result.id)
        assertEquals(expectedUrl, result.url)
        assertTrue(result.companyId == "company-1")

        coVerify(exactly = 1) {
            webhooksClient.updateWebhook("api-key", "api-secret", "wh-keep", expectedUrl, "company-1")
        }
        coVerify(exactly = 1) {
            webhooksClient.deleteWebhook("api-key", "api-secret", "wh-delete")
        }
    }

    private fun testSettings(
        companyId: String,
        webhookToken: String
    ): PeppolSettingsDto {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return PeppolSettingsDto(
            id = PeppolSettingsId.generate(),
            tenantId = TenantId.generate(),
            companyId = companyId,
            peppolId = PeppolId("0208:BE0123456789"),
            isEnabled = true,
            testMode = false,
            webhookToken = webhookToken,
            lastFullSyncAt = null,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun testWebhook(
        id: String,
        companyId: String,
        url: String,
        createdAt: String
    ) = RecommandWebhook(
        id = id,
        teamId = "team-1",
        companyId = companyId,
        url = url,
        createdAt = createdAt,
        updatedAt = createdAt
    )
}
