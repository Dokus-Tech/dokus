package tech.dokus.backend.services.business

import io.ktor.client.HttpClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig

class BusinessWebsiteProbeSearchTest {
    private val probe = BusinessWebsiteProbe(
        httpClient = mockk<HttpClient>(),
        config = BusinessProfileEnrichmentConfig(
            enabled = true,
            pollingIntervalMs = 5_000,
            batchSize = 5,
            maxAttempts = 5,
            staleLeaseMinutes = 15,
            maxPages = 5,
            serperApiKey = "test",
            serperBaseUrl = "https://google.serper.dev/search",
            ignoreRobots = false
        )
    )

    @Test
    fun `builds strict query as company plus country when country is present`() {
        assertEquals("Invoid Vision BE", probe.buildStrictSearchQuery("Invoid Vision", "BE"))
    }

    @Test
    fun `builds strict query as company only when country is missing`() {
        assertEquals("Invoid Vision", probe.buildStrictSearchQuery("Invoid Vision", null))
        assertEquals("Invoid Vision", probe.buildStrictSearchQuery("Invoid Vision", "   "))
    }

    @Test
    fun `strips legal suffixes from company name before building query`() {
        assertEquals("KBC Bank BE", probe.buildStrictSearchQuery("KBC Bank NV", "BE"))
        assertEquals("Coolblue België BE", probe.buildStrictSearchQuery("Coolblue België N.V.", "BE"))
    }
}
