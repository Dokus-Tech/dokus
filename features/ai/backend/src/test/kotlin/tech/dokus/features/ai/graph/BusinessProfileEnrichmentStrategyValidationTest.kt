package tech.dokus.features.ai.graph

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import tech.dokus.features.ai.models.BusinessDiscoveryStatus
import tech.dokus.features.ai.models.BusinessProfileDiscoveryResult

class BusinessProfileEnrichmentStrategyValidationTest {
    @Test
    fun `valid discovery payload passes validation`() {
        val error = validateDiscoveryForRetry(
            BusinessProfileDiscoveryResult(
                status = BusinessDiscoveryStatus.Found,
                candidateWebsiteUrl = "https://example.com",
                logoUrl = "https://example.com/logo.png",
                activities = listOf("consulting"),
                confidence = 0.9
            )
        )

        assertNull(error)
    }

    @Test
    fun `invalid website URL is rejected`() {
        val error = validateDiscoveryForRetry(
            BusinessProfileDiscoveryResult(
                status = BusinessDiscoveryStatus.Found,
                candidateWebsiteUrl = "example.com",
                confidence = 0.7
            )
        )

        assertEquals("candidateWebsiteUrl must be an absolute http(s) URL.", error)
    }
}
