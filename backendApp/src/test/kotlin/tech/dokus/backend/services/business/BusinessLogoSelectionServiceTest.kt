package tech.dokus.backend.services.business

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.dokus.features.ai.models.BusinessLogoFallbackCandidate
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class BusinessLogoSelectionServiceTest {
    private val websiteProbe = mockk<BusinessWebsiteProbe>()
    private val service = BusinessLogoSelectionService(websiteProbe)

    @Test
    fun `fallback icons are attempted when initial candidates exist but fail`() = runBlocking {
        val faviconBytes = pngBytes(96, 96, Color.BLUE)

        coEvery { websiteProbe.downloadImageDetailed(any(), any(), any()) } returns ImageDownloadResult(
            image = null,
            failureKind = ImageDownloadFailureKind.HttpStatus,
            statusCode = 404
        )
        coEvery { websiteProbe.downloadImageDetailed("https://acme.example/favicon.ico", any(), any()) } returns ImageDownloadResult(
            image = DownloadedBusinessImage(bytes = faviconBytes, contentType = "image/png"),
            normalizedUrl = "https://acme.example/favicon.ico",
            statusCode = 200,
            contentType = "image/png"
        )

        val result = service.selectPreferredLogo(
            websiteUrl = "https://acme.example",
            logoCandidates = listOf("https://acme.example/assets/broken-logo.svg")
        )

        assertNotNull(result.image)
        assertTrue(result.trace.fallbackCandidatesAppended > 0)
        assertEquals("https://acme.example/favicon.ico", result.trace.selectedSourceUrl)
        assertEquals("Png", result.trace.selectedFormat)
    }

    @Test
    fun `bot block status triggers single browser user-agent retry`() = runBlocking {
        val logoBytes = pngBytes(120, 120, Color.GREEN)
        val candidateUrl = "https://www.kbc.com/logo.png"

        coEvery { websiteProbe.downloadImageDetailed(any(), any(), any()) } returns ImageDownloadResult(
            image = null,
            failureKind = ImageDownloadFailureKind.HttpStatus,
            statusCode = 404
        )
        coEvery { websiteProbe.downloadImageDetailed(candidateUrl, any(), false) } returns ImageDownloadResult(
            image = null,
            failureKind = ImageDownloadFailureKind.HttpStatus,
            statusCode = 403
        )
        coEvery { websiteProbe.downloadImageDetailed(candidateUrl, any(), true) } returns ImageDownloadResult(
            image = DownloadedBusinessImage(bytes = logoBytes, contentType = "image/png"),
            normalizedUrl = candidateUrl,
            statusCode = 200,
            contentType = "image/png",
            usedBrowserUserAgent = true
        )

        val result = service.selectPreferredLogo(
            websiteUrl = "https://www.kbc.com",
            logoCandidates = listOf(candidateUrl)
        )

        assertNotNull(result.image)
        assertTrue(result.trace.attempts >= 2)
        coVerify(exactly = 1) { websiteProbe.downloadImageDetailed(candidateUrl, any(), false) }
        coVerify(exactly = 1) { websiteProbe.downloadImageDetailed(candidateUrl, any(), true) }
    }

    @Test
    fun `logo discovery stops with budget exhausted when attempts are too slow`() = runBlocking {
        coEvery { websiteProbe.downloadImageDetailed(any(), any(), any()) } coAnswers {
            delay(1_400)
            ImageDownloadResult(
                image = null,
                failureKind = ImageDownloadFailureKind.HttpStatus,
                statusCode = 404
            )
        }

        val result = service.selectPreferredLogo(
            websiteUrl = "https://example.com",
            logoCandidates = listOf(
                "https://example.com/a.png",
                "https://example.com/b.png",
                "https://example.com/c.png",
                "https://example.com/d.png",
                "https://example.com/e.png"
            ),
            budgetMs = 3_500L,
        )

        assertEquals(LogoSelectionTerminalReason.BUDGET_EXHAUSTED, result.trace.terminalReason)
        assertTrue(result.trace.attempts in 2..3)
        assertTrue(result.trace.elapsedMs >= 3_500L)
    }

    @Test
    fun `ai candidate validation keeps same-domain http urls and rejects noisy off-domain`() {
        val validated = service.validateAiFallbackCandidates(
            selectedWebsiteUrl = "https://www.tesla.com/nl_be",
            knownAssetHosts = setOf("static-assets.tesla.com"),
            aiCandidates = listOf(
                BusinessLogoFallbackCandidate(
                    url = "https://www.tesla.com/themes/custom/logo.svg",
                    confidence = 0.92
                ),
                BusinessLogoFallbackCandidate(
                    url = "data:image/png;base64,abc",
                    confidence = 0.4
                ),
                BusinessLogoFallbackCandidate(
                    url = "https://cdn.example.com/share/social-banner.png",
                    confidence = 0.7
                ),
                BusinessLogoFallbackCandidate(
                    url = "https://static-assets.tesla.com/favicon.ico",
                    confidence = 0.6
                )
            )
        )

        assertEquals(
            listOf(
                "https://www.tesla.com/themes/custom/logo.svg",
                "https://static-assets.tesla.com/favicon.ico"
            ),
            validated.acceptedUrls
        )
        assertTrue(
            (validated.rejectReasons["non_http_scheme"] ?: 0) >= 1 ||
                (validated.rejectReasons["invalid_url"] ?: 0) >= 1
        )
        assertFalse(validated.rejectReasons.isEmpty())
    }

    private fun pngBytes(width: Int, height: Int, color: Color): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        graphics.color = color
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, "png", output)
            output.toByteArray()
        }
    }
}
