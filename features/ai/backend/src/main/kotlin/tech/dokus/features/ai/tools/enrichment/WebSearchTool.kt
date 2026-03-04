package tech.dokus.features.ai.tools.enrichment

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Tool that searches the web for company websites using SerpAPI.
 * Returns top organic search results for the LLM to evaluate.
 */
class WebSearchTool(
    private val httpClient: HttpClient,
    private val serpApiKey: String
) : ToolSet {

    private val json = Json { ignoreUnknownKeys = true }

    @Tool
    @LLMDescription(
        """
        Searches Google for a company's official website using SerpAPI.

        Use this to find the official website of a company by name and optionally country.
        Returns up to 5 organic search results with title, URL, and snippet.

        You should evaluate the results carefully:
        - Does the domain name match or relate to the company name?
        - Does the snippet describe the company's business?
        - If multiple results look plausible, you can scrape them to verify.

        Returns: A numbered list of search results, or "NO_RESULTS" if nothing found.
        Returns "NO_API_KEY" if the SerpAPI key is not configured.
    """
    )
    suspend fun searchCompanyWebsite(
        @LLMDescription("The company name to search for. Example: 'Invoid Vision'")
        companyName: String,
        @LLMDescription("Optional country to narrow results. Example: 'Belgium', 'Netherlands'. Leave empty if unknown.")
        country: String = ""
    ): String {
        if (serpApiKey.isBlank()) return "NO_API_KEY"

        val query = buildString {
            append(companyName)
            append(" official website")
            if (country.isNotBlank()) append(" $country")
        }

        return try {
            val response = httpClient.get("https://serpapi.com/search.json") {
                parameter("q", query)
                parameter("api_key", serpApiKey)
                parameter("num", 5)
            }

            val body = response.bodyAsText()
            val jsonObj = json.decodeFromString<JsonObject>(body)

            val organicResults = jsonObj["organic_results"]?.jsonArray ?: return "NO_RESULTS"
            if (organicResults.isEmpty()) return "NO_RESULTS"

            buildString {
                appendLine("Search results for '$companyName':")
                organicResults.forEachIndexed { index, result ->
                    val obj = result.jsonObject
                    val title = obj["title"]?.jsonPrimitive?.content ?: "No title"
                    val link = obj["link"]?.jsonPrimitive?.content ?: "No URL"
                    val snippet = obj["snippet"]?.jsonPrimitive?.content ?: "No description"
                    appendLine("[${index + 1}] $title")
                    appendLine("    URL: $link")
                    appendLine("    $snippet")
                }
            }
        } catch (e: Exception) {
            "ERROR: Failed to search - ${e.message}"
        }
    }
}
