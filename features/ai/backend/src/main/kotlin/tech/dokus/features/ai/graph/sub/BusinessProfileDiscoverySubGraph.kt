package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.params.LLMParams
import kotlinx.serialization.Serializable
import tech.dokus.features.ai.config.asOrchestratorModel
import tech.dokus.features.ai.config.assistantResponseRepeatMax
import tech.dokus.features.ai.config.documentProcessing
import tech.dokus.features.ai.models.BusinessDiscoveryStatus
import tech.dokus.features.ai.models.BusinessProfileDiscoveryResult
import tech.dokus.features.ai.models.BusinessProfileEnrichmentInput
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.businessProfileDiscoverySubGraph(
    aiConfig: AIConfig,
    tools: List<Tool<*, *>>
): AIAgentSubgraphDelegate<BusinessProfileEnrichmentInput, BusinessProfileDiscoveryResult> {
    return subgraphWithTask(
        name = "Discover business profile",
        llmModel = aiConfig.mode.asOrchestratorModel,
        tools = tools,
        llmParams = LLMParams.documentProcessing,
        assistantResponseRepeatMax = assistantResponseRepeatMax,
        finishTool = BusinessProfileDiscoveryFinishTool()
    ) { it.prompt }
}

@Serializable
private data class BusinessProfileDiscoveryToolInput(
    @property:LLMDescription("Discovery status: FOUND or NOT_FOUND.")
    val status: BusinessDiscoveryStatus,
    @property:LLMDescription("Canonical company website candidate (absolute URL) when found.")
    val candidateWebsiteUrl: String? = null,
    @property:LLMDescription("Short summary of business activity in the requested language.")
    val businessSummary: String? = null,
    @property:LLMDescription("Business activities as short phrases.")
    val activities: List<String> = emptyList(),
    @property:LLMDescription("Candidate company logo URL.")
    val logoUrl: String? = null,
    @property:LLMDescription("Confidence score from 0.0 to 1.0.")
    val confidence: Double = 0.0,
    @property:LLMDescription("Reasons for candidate selection.")
    val candidateReasons: List<String> = emptyList(),
    @property:LLMDescription("Raw website URLs considered from search results (top 5).")
    val searchResultUrls: List<String> = emptyList(),
)

private class BusinessProfileDiscoveryFinishTool :
    Tool<BusinessProfileDiscoveryToolInput, BusinessProfileDiscoveryResult>(
        argsSerializer = BusinessProfileDiscoveryToolInput.serializer(),
        resultSerializer = BusinessProfileDiscoveryResult.serializer(),
        name = "submit_business_profile_discovery",
        description = "Submit final discovered website/profile/logo candidates in structured form."
    ) {
    override suspend fun execute(args: BusinessProfileDiscoveryToolInput): BusinessProfileDiscoveryResult {
        return BusinessProfileDiscoveryResult(
            status = args.status,
            candidateWebsiteUrl = args.candidateWebsiteUrl,
            businessSummary = args.businessSummary,
            activities = args.activities,
            logoUrl = args.logoUrl,
            confidence = args.confidence.coerceIn(0.0, 1.0),
            candidateReasons = args.candidateReasons,
            searchResultUrls = args.searchResultUrls
        )
    }
}

private val BusinessProfileEnrichmentInput.prompt
    get() = """
    Find the official website and business profile for this company.
    Use tools for all external data access.

    Company name: $companyName
    VAT: ${companyVatNumber ?: "unknown"}
    Country: ${companyCountry ?: "unknown"}
    City: ${companyCity ?: "unknown"}
    Postal code: ${companyPostalCode ?: "unknown"}
    Email: ${companyEmail ?: "unknown"}
    Phone: ${companyPhone ?: "unknown"}
    Output language: ${outputLanguage.dbValue}

    Constraints:
    - Search with `search_google_serper`.
    - Crawl at most $maxPages pages in total using `fetch_web_page`.
    - Respect robots policy as provided by tooling.
    - Filter out aggregators/social/network pages.
    - Return only one best candidate website, if any.
    - If no reliable candidate exists, return status NOT_FOUND.
    - Use `inspect_image_url` before returning logoUrl.

    Output:
    - Always finish using tool `submit_business_profile_discovery`.
    - activities must be concise phrases (max 8).
    - confidence must be 0.0..1.0.
    """.trimIndent()
