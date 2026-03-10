package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.params.LLMParams
import kotlinx.serialization.Serializable
import tech.dokus.features.ai.config.asOrchestratorModel
import tech.dokus.features.ai.config.finishToolOnly
import tech.dokus.features.ai.config.finishToolTextAssistantResponseRepeatMax
import tech.dokus.features.ai.models.BusinessLogoFallbackCandidate
import tech.dokus.features.ai.models.BusinessLogoFallbackInput
import tech.dokus.features.ai.models.BusinessLogoFallbackResult
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.businessLogoFallbackSubGraph(
    aiConfig: AIConfig
): AIAgentSubgraphDelegate<BusinessLogoFallbackInput, BusinessLogoFallbackResult> {
    return subgraphWithTask(
        name = "Recover logo candidates from HTML snippets",
        llmModel = aiConfig.mode.asOrchestratorModel,
        tools = emptyList<Tool<*, *>>(),
        llmParams = LLMParams.finishToolOnly("submit_business_logo_fallback"),
        assistantResponseRepeatMax = finishToolTextAssistantResponseRepeatMax,
        finishTool = BusinessLogoFallbackFinishTool()
    ) { it.prompt }
}

@Serializable
private data class BusinessLogoFallbackToolInput(
    @property:LLMDescription("Ordered logo candidate URLs (max 8).")
    val candidates: List<BusinessLogoFallbackCandidateInput> = emptyList(),
)

@Serializable
private data class BusinessLogoFallbackCandidateInput(
    @property:LLMDescription("Absolute http(s) URL for a likely logo or icon asset.")
    val url: String,
    @property:LLMDescription("Confidence from 0.0 to 1.0.")
    val confidence: Double = 0.0,
    @property:LLMDescription("Short reason why this URL is likely a logo.")
    val reason: String? = null,
)

private class BusinessLogoFallbackFinishTool :
    Tool<BusinessLogoFallbackToolInput, BusinessLogoFallbackResult>(
        argsSerializer = BusinessLogoFallbackToolInput.serializer(),
        resultSerializer = BusinessLogoFallbackResult.serializer(),
        name = "submit_business_logo_fallback",
        description = "Submit candidate logo URLs inferred from provided HTML snippets only."
    ) {
    override suspend fun execute(args: BusinessLogoFallbackToolInput): BusinessLogoFallbackResult {
        return BusinessLogoFallbackResult(
            candidates = args.candidates
                .map {
                    BusinessLogoFallbackCandidate(
                        url = it.url,
                        confidence = it.confidence.coerceIn(0.0, 1.0),
                        reason = it.reason
                    )
                }
                .take(8)
        )
    }
}

private val BusinessLogoFallbackInput.prompt
    get() = """
    Identify likely company logo assets from the provided HTML snippets only.
    Do not browse the web and do not invent domains.

    Selected website: $selectedWebsiteUrl
    Failed candidate URLs:
    ${failedCandidateUrls.joinToString("\n") { "- $it" }.ifBlank { "- none" }}

    Rules:
    - Return only absolute http(s) URLs.
    - Prefer explicit logo/icon/manifest assets.
    - Avoid social/share/banner images.
    - Use only URLs explicitly present in snippets or in the known asset lists below.
    - Never derive new URLs from hostnames, guessed paths, favicon conventions, or common logo filenames.
    - Return at most 8 candidates ordered best-first.

    Provided pages:
    ${pages.joinToString("\n\n") { page ->
        buildString {
            append("URL: ")
            append(page.url)
            append('\n')
            append("Title: ")
            append(page.title ?: "-")
            append('\n')
            append("Description: ")
            append(page.description ?: "-")
            append('\n')
            append("Head snippet: ")
            append(page.headHtmlSnippet ?: "-")
            append('\n')
            append("Logo snippet: ")
            append(page.logoRelevantHtmlSnippet ?: "-")
            append('\n')
            if (page.structuredDataSnippets.isNotEmpty()) {
                append("Structured: ")
                append(page.structuredDataSnippets.joinToString(" | "))
                append('\n')
            }
            if (page.assetUrls.isNotEmpty()) {
                append("Known assets: ")
                append(page.assetUrls.joinToString(" | "))
                append('\n')
            }
        }
    }}

    Always finish using submit_business_logo_fallback.
    """.trimIndent()
