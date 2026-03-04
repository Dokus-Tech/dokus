package tech.dokus.features.ai.tools.enrichment

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

/**
 * Tool that scrapes a website and returns clean text content.
 * Strips HTML tags and truncates to a manageable size for LLM processing.
 */
class WebScraperTool(
    private val httpClient: HttpClient
) : ToolSet {

    @Tool
    @LLMDescription(
        """
        Scrapes a website URL and returns the text content (HTML tags stripped).

        Use this to:
        - Read a company's homepage to extract a business summary and activities
        - Verify that a website belongs to the expected company
        - Check if the website mentions a specific VAT number or company name

        The content is truncated to ~4000 characters to fit in context.

        Input: A full URL (e.g., 'https://www.example.com')
        Returns: The clean text content of the page, or an error message.
    """
    )
    suspend fun scrapeWebsite(
        @LLMDescription("The full URL to scrape. Must include https:// or http://. Example: 'https://www.invoid.vision'")
        url: String
    ): String {
        return try {
            val response = httpClient.get(url) {
                header("User-Agent", "Mozilla/5.0 (compatible; DokusBot/1.0)")
                header("Accept", "text/html")
            }

            val html = response.bodyAsText()
            val cleanText = stripHtml(html)

            if (cleanText.isBlank()) {
                "EMPTY: The page returned no readable text content."
            } else {
                cleanText.take(4000)
            }
        } catch (e: Exception) {
            "ERROR: Failed to scrape $url - ${e.message}"
        }
    }

    private fun stripHtml(html: String): String {
        return html
            // Remove script and style blocks
            .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
            // Remove HTML comments
            .replace(Regex("<!--[\\s\\S]*?-->"), " ")
            // Remove all HTML tags
            .replace(Regex("<[^>]+>"), " ")
            // Decode common HTML entities
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&nbsp;", " ")
            .replace("&#39;", "'")
            // Collapse whitespace
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
