package tech.dokus.backend.services.business

import io.ktor.client.HttpClient
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig

class BusinessWebsiteProbeTest {
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
    fun `canonicalize website url strips tracking params and fragment`() {
        val canonical = probe.canonicalizeWebsiteUrl(
            "https://www.coolblue.be/nl?srsltid=abc&utm_source=search&foo=bar#top"
        )

        assertEquals("https://www.coolblue.be/nl?foo=bar", canonical)
    }

    @Test
    fun `collect logo candidates keeps reliable sources and ignores noisy metadata`() {
        val html = """
            <html>
              <head>
                <link rel="icon" href="/favicon.ico" />
                <meta property="og:image" content="/images/banner-home.jpg" />
                <meta property="og:image" content="/assets/logo-og.png" />
                <meta name="twitter:image" content="/images/twitter-card.jpg" />
                <meta itemprop="image" content="/images/itemprop.jpg" />
              </head>
              <body>
                <img src="/media/company-logo.svg" />
              </body>
            </html>
        """.trimIndent()

        val structuredData = listOf(
            """{"@context":"https://schema.org","@type":"Organization","logo":"https://cdn.example.com/brand/official-logo.png"}"""
        )
        val manifestIcons = listOf("https://example.com/manifest-icon-192.png")

        val candidates = probe.collectLogoCandidates(
            html = html,
            baseUrl = "https://example.com/about",
            structuredData = structuredData,
            manifestIcons = manifestIcons
        )

        assertTrue(candidates.contains("https://example.com/favicon.ico"))
        assertTrue(candidates.contains("https://example.com/assets/logo-og.png"))
        assertTrue(candidates.contains("https://example.com/media/company-logo.svg"))
        assertTrue(candidates.contains("https://cdn.example.com/brand/official-logo.png"))
        assertTrue(candidates.contains("https://example.com/manifest-icon-192.png"))
        assertFalse(candidates.any { it.contains("twitter-card", ignoreCase = true) })
        assertFalse(candidates.any { it.contains("itemprop", ignoreCase = true) })
        assertFalse(candidates.any { it.contains("banner-home", ignoreCase = true) })
    }
}
