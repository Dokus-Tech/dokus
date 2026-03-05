package tech.dokus.backend.peppol

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.VatNumber
import tech.dokus.peppol.config.InboxConfig
import tech.dokus.peppol.config.MasterCredentialsConfig
import tech.dokus.peppol.config.PeppolModuleConfig
import tech.dokus.peppol.config.WebhookConfig
import tech.dokus.peppol.provider.client.RecommandApiException
import tech.dokus.peppol.provider.client.RecommandProvider
import tech.dokus.peppol.service.PeppolVerificationService
import java.net.ConnectException
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PeppolVerificationServiceErrorMappingTest {

    @Test
    fun `recommand api 5xx error maps to recoverable peppol directory unavailable`() = runTest {
        val provider = mockk<RecommandProvider>()
        every { provider.configure(any()) } returns Unit
        coEvery { provider.searchDirectory(any()) } returns Result.failure(RecommandApiException(503, "down"))

        val service = PeppolVerificationService(provider, testModuleConfig())
        val failure = service.verify(VatNumber("BE0777887045")).exceptionOrNull()

        val error = assertIs<DokusException.PeppolDirectoryUnavailable>(failure)
        assertTrue(error.recoverable)
    }

    @Test
    fun `recommand api 4xx error maps to internal error`() = runTest {
        val provider = mockk<RecommandProvider>()
        every { provider.configure(any()) } returns Unit
        coEvery { provider.searchDirectory(any()) } returns Result.failure(RecommandApiException(401, "unauthorized"))

        val service = PeppolVerificationService(provider, testModuleConfig())
        val failure = service.verify(VatNumber("BE0777887045")).exceptionOrNull()

        assertIs<DokusException.InternalError>(failure)
    }

    @Test
    fun `network error maps to recoverable connection error`() = runTest {
        val provider = mockk<RecommandProvider>()
        every { provider.configure(any()) } returns Unit
        coEvery { provider.searchDirectory(any()) } returns Result.failure(ConnectException("offline"))

        val service = PeppolVerificationService(provider, testModuleConfig())
        val failure = service.verify(VatNumber("BE0777887045")).exceptionOrNull()

        val error = assertIs<DokusException.ConnectionError>(failure)
        assertTrue(error.recoverable)
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
}
