package tech.dokus.features.ai.tools.enrichment

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Finish tool for the enrichment graph.
 * The LLM calls this to submit structured enrichment results.
 */
class SubmitEnrichmentTool : Tool<EnrichmentToolInput, EnrichmentToolResult>(
    argsSerializer = EnrichmentToolInput.serializer(),
    resultSerializer = EnrichmentToolResult.serializer(),
    name = "submit_enrichment",
    description = "Submit the final business enrichment result after researching the company. " +
        "Call this once you have gathered the website URL, business summary, activities, and best logo URL."
) {
    override suspend fun execute(args: EnrichmentToolInput): EnrichmentToolResult {
        return EnrichmentToolResult(
            websiteUrl = args.websiteUrl,
            summary = args.summary,
            activities = args.activities,
            logoUrl = args.logoUrl
        )
    }
}

@Serializable
data class EnrichmentToolInput(
    @property:LLMDescription("The verified official website URL of the company, or null if not found with confidence")
    val websiteUrl: String? = null,
    @property:LLMDescription("A concise 1-3 sentence summary of what the company does, based on website content")
    val summary: String? = null,
    @property:LLMDescription("Comma-separated list of business activities (e.g., 'Software Development, IT Consulting, Cloud Services')")
    val activities: String? = null,
    @property:LLMDescription("The URL of the best logo image found. Prefer PNG/JPEG from og:image or apple-touch-icon over SVG/ICO. Null if no suitable logo found.")
    val logoUrl: String? = null
)

@Serializable
@SerialName("EnrichmentToolResult")
@LLMDescription("Result of business enrichment research")
data class EnrichmentToolResult(
    val websiteUrl: String?,
    val summary: String?,
    val activities: String?,
    val logoUrl: String?
)
