package tech.dokus.backend.peppol

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.AddressId
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolSettingsId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.PeppolConnectStatus
import tech.dokus.domain.model.Tenant
import tech.dokus.peppol.config.InboxConfig
import tech.dokus.peppol.config.MasterCredentialsConfig
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.config.WebhookConfig
import tech.dokus.peppol.provider.client.RecommandCompaniesClient
import tech.dokus.peppol.provider.client.recommand.model.RecommandCompany
import tech.dokus.peppol.service.PeppolConnectionService
import tech.dokus.peppol.service.PeppolWebhookSyncService
import tech.dokus.database.repository.peppol.PeppolSettingsRepository
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PeppolConnectionServiceWebhookSyncTest {

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
    fun `connect ensures webhook convergence before returning connected`() = runBlocking {
        val settingsRepository = mockk<PeppolSettingsRepository>()
        val companiesClient = mockk<RecommandCompaniesClient>()
        val webhookSyncService = mockk<PeppolWebhookSyncService>()
        val service = PeppolConnectionService(
            settingsRepository = settingsRepository,
            recommandCompaniesClient = companiesClient,
            moduleConfig = moduleConfig,
            webhookSyncService = webhookSyncService
        )

        val tenant = testTenant()
        val address = testAddress(tenant.id)
        val company = testCompany()
        val savedSettings = testSettings(tenant.id, company.id)

        coEvery {
            companiesClient.listCompanies("api-key", "api-secret", tenant.vatNumber.normalized)
        } returns Result.success(listOf(company))
        coEvery {
            settingsRepository.saveSettings(
                tenantId = tenant.id,
                companyId = company.id,
                peppolId = "0208:${tenant.vatNumber.normalized}",
                isEnabled = true,
                testMode = false
            )
        } returns Result.success(savedSettings)
        coEvery { webhookSyncService.ensureSingleWebhookForSettings(savedSettings) } returns Result.success(mockk())

        val result = service.connect(tenant, address).getOrThrow()

        assertEquals(PeppolConnectStatus.Connected, result.status)
        coVerify(exactly = 1) { webhookSyncService.ensureSingleWebhookForSettings(savedSettings) }
    }

    @Test
    fun `connect fails when webhook convergence fails`() = runBlocking {
        val settingsRepository = mockk<PeppolSettingsRepository>()
        val companiesClient = mockk<RecommandCompaniesClient>()
        val webhookSyncService = mockk<PeppolWebhookSyncService>()
        val service = PeppolConnectionService(
            settingsRepository = settingsRepository,
            recommandCompaniesClient = companiesClient,
            moduleConfig = moduleConfig,
            webhookSyncService = webhookSyncService
        )

        val tenant = testTenant()
        val address = testAddress(tenant.id)
        val company = testCompany()
        val savedSettings = testSettings(tenant.id, company.id)

        coEvery {
            companiesClient.listCompanies("api-key", "api-secret", tenant.vatNumber.normalized)
        } returns Result.success(listOf(company))
        coEvery {
            settingsRepository.saveSettings(any(), any(), any(), any(), any())
        } returns Result.success(savedSettings)
        coEvery {
            webhookSyncService.ensureSingleWebhookForSettings(savedSettings)
        } returns Result.failure(IllegalStateException("webhook sync failed"))

        val result = service.connect(tenant, address)
        assertTrue(result.isFailure)
    }

    private fun testTenant(): Tenant {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return Tenant(
            id = TenantId.generate(),
            type = TenantType.Company,
            legalName = LegalName("Tenant BV"),
            displayName = DisplayName("Tenant BV"),
            subscription = SubscriptionTier.Core,
            status = TenantStatus.Active,
            language = Language.En,
            vatNumber = VatNumber("BE0123456789"),
            createdAt = now,
            updatedAt = now
        )
    }

    private fun testAddress(tenantId: TenantId): Address {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return Address(
            id = AddressId.generate(),
            tenantId = tenantId,
            streetLine1 = "Main Street 1",
            city = "Brussels",
            postalCode = "1000",
            country = "BE",
            createdAt = now,
            updatedAt = now
        )
    }

    private fun testSettings(tenantId: TenantId, companyId: String): PeppolSettingsDto {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        return PeppolSettingsDto(
            id = PeppolSettingsId.generate(),
            tenantId = tenantId,
            companyId = companyId,
            peppolId = PeppolId("0208:BE0123456789"),
            isEnabled = true,
            testMode = false,
            webhookToken = "token-1",
            lastFullSyncAt = null,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun testCompany(): RecommandCompany = RecommandCompany(
        id = "company-1",
        teamId = "team-1",
        name = "Tenant BV",
        address = "Main Street 1",
        postalCode = "1000",
        city = "Brussels",
        country = "BE",
        enterpriseNumber = "0123456789",
        vatNumber = "BE0123456789",
        isSmpRecipient = true,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )
}
