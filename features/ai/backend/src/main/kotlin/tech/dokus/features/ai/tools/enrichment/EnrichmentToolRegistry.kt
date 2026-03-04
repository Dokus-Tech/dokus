package tech.dokus.features.ai.tools.enrichment

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.tools
import io.ktor.client.HttpClient

/**
 * Creates a ToolRegistry with all enrichment tools.
 */
object EnrichmentToolRegistry {
    operator fun invoke(
        httpClient: HttpClient,
        serpApiKey: String
    ) = ToolRegistry {
        tools(WebSearchTool(httpClient, serpApiKey))
        tools(WebScraperTool(httpClient))
        tools(LogoExtractorTool(httpClient))
    }
}
