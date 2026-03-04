package tech.dokus.features.ai.tools.enrichment

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import java.net.URI

/**
 * Tool that extracts logo candidates from a website's HTML.
 * Returns a prioritized list of image URLs the LLM can choose from.
 */
class LogoExtractorTool(
    private val httpClient: HttpClient
) : ToolSet {

    @Tool
    @LLMDescription(
        """
        Extracts logo image candidates from a website URL.

        Fetches the HTML of the given URL and looks for logo images in priority order:
        1. Open Graph image (og:image meta tag) - usually high quality PNG/JPEG
        2. Apple touch icon - always PNG, good quality
        3. PNG favicon links
        4. IMG elements with "logo" in class/id/alt/src
        5. Generic favicon (last resort, often ICO format)

        Returns a numbered list of candidates with format info.
        You should pick the best candidate: prefer PNG/JPEG over SVG/ICO.

        Input: A full URL (e.g., 'https://www.example.com')
        Returns: Numbered list of logo candidates, or "NO_LOGOS_FOUND".
    """
    )
    suspend fun extractLogo(
        @LLMDescription("The full URL of the website to extract logo from. Example: 'https://www.invoid.vision'")
        url: String
    ): String {
        return try {
            val response = httpClient.get(url) {
                header("User-Agent", "Mozilla/5.0 (compatible; DokusBot/1.0)")
                header("Accept", "text/html")
            }

            val html = response.bodyAsText()
            val baseUrl = resolveBaseUrl(url)
            val candidates = mutableListOf<LogoCandidate>()

            // 1. og:image
            findOgImage(html)?.let { src ->
                candidates.add(LogoCandidate(resolveUrl(baseUrl, src), "og:image", guessFormat(src)))
            }

            // 2. apple-touch-icon
            findAppleTouchIcon(html)?.let { src ->
                candidates.add(LogoCandidate(resolveUrl(baseUrl, src), "apple-touch-icon", "PNG"))
            }

            // 3. PNG favicon
            findPngFavicon(html)?.let { src ->
                candidates.add(LogoCandidate(resolveUrl(baseUrl, src), "png-favicon", "PNG"))
            }

            // 4. IMG elements with "logo" in attributes
            findLogoImages(html).forEach { src ->
                candidates.add(LogoCandidate(resolveUrl(baseUrl, src), "img-logo", guessFormat(src)))
            }

            // 5. Generic favicon
            findGenericFavicon(html)?.let { src ->
                candidates.add(LogoCandidate(resolveUrl(baseUrl, src), "favicon", guessFormat(src)))
            }

            // Always add /favicon.ico as fallback
            candidates.add(LogoCandidate("$baseUrl/favicon.ico", "default-favicon", "ICO"))

            // Deduplicate by URL
            val unique = candidates.distinctBy { it.url }

            if (unique.isEmpty()) {
                "NO_LOGOS_FOUND"
            } else {
                buildString {
                    appendLine("Logo candidates found on $url:")
                    unique.forEachIndexed { index, candidate ->
                        appendLine("[${index + 1}] ${candidate.format}: ${candidate.url} (${candidate.source})")
                    }
                    appendLine()
                    appendLine("Pick the best candidate. Prefer PNG/JPEG from og:image or apple-touch-icon over SVG/ICO.")
                }
            }
        } catch (e: Exception) {
            "ERROR: Failed to extract logos from $url - ${e.message}"
        }
    }

    private data class LogoCandidate(val url: String, val source: String, val format: String)

    private fun findOgImage(html: String): String? {
        val regex = Regex("""<meta\s+[^>]*property\s*=\s*["']og:image["'][^>]*content\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val altRegex = Regex("""<meta\s+[^>]*content\s*=\s*["']([^"']+)["'][^>]*property\s*=\s*["']og:image["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.get(1)
            ?: altRegex.find(html)?.groupValues?.get(1)
    }

    private fun findAppleTouchIcon(html: String): String? {
        val regex = Regex("""<link\s+[^>]*rel\s*=\s*["']apple-touch-icon["'][^>]*href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val altRegex = Regex("""<link\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["']apple-touch-icon["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.get(1)
            ?: altRegex.find(html)?.groupValues?.get(1)
    }

    private fun findPngFavicon(html: String): String? {
        val regex = Regex("""<link\s+[^>]*type\s*=\s*["']image/png["'][^>]*href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val altRegex = Regex("""<link\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*type\s*=\s*["']image/png["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.get(1)
            ?: altRegex.find(html)?.groupValues?.get(1)
    }

    private fun findLogoImages(html: String): List<String> {
        val regex = Regex("""<img\s+[^>]*(?:class|id|alt|src)\s*=\s*["'][^"']*logo[^"']*["'][^>]*src\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val altRegex = Regex("""<img\s+[^>]*src\s*=\s*["']([^"']*logo[^"']*?)["']""", RegexOption.IGNORE_CASE)
        val results = mutableListOf<String>()
        regex.findAll(html).take(3).forEach { results.add(it.groupValues[1]) }
        if (results.isEmpty()) {
            altRegex.findAll(html).take(3).forEach { results.add(it.groupValues[1]) }
        }
        return results
    }

    private fun findGenericFavicon(html: String): String? {
        val regex = Regex("""<link\s+[^>]*rel\s*=\s*["'](?:shortcut )?icon["'][^>]*href\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val altRegex = Regex("""<link\s+[^>]*href\s*=\s*["']([^"']+)["'][^>]*rel\s*=\s*["'](?:shortcut )?icon["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.get(1)
            ?: altRegex.find(html)?.groupValues?.get(1)
    }

    private fun guessFormat(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".png") -> "PNG"
            lower.contains(".jpg") || lower.contains(".jpeg") -> "JPEG"
            lower.contains(".webp") -> "WebP"
            lower.contains(".svg") -> "SVG"
            lower.contains(".ico") -> "ICO"
            lower.contains(".gif") -> "GIF"
            else -> "Unknown"
        }
    }

    private fun resolveBaseUrl(url: String): String {
        return try {
            val uri = URI(url)
            "${uri.scheme}://${uri.host}${if (uri.port > 0 && uri.port != 443 && uri.port != 80) ":${uri.port}" else ""}"
        } catch (e: Exception) {
            url.substringBefore("/", url)
        }
    }

    private fun resolveUrl(baseUrl: String, href: String): String {
        return when {
            href.startsWith("http://") || href.startsWith("https://") -> href
            href.startsWith("//") -> "https:$href"
            href.startsWith("/") -> "$baseUrl$href"
            else -> "$baseUrl/$href"
        }
    }
}
