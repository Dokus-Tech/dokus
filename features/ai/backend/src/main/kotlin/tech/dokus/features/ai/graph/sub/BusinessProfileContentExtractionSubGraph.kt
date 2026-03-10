package tech.dokus.features.ai.graph.sub

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.params.LLMParams
import kotlinx.serialization.Serializable
import tech.dokus.features.ai.config.asOrchestratorModel
import tech.dokus.features.ai.config.finishToolOnlyText
import tech.dokus.features.ai.config.finishToolTextAssistantResponseRepeatMax
import tech.dokus.features.ai.models.BusinessProfileContentExtractionInput
import tech.dokus.features.ai.models.BusinessProfileContentExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

fun AIAgentSubgraphBuilderBase<*, *>.businessProfileContentExtractionSubGraph(
    aiConfig: AIConfig
): AIAgentSubgraphDelegate<BusinessProfileContentExtractionInput, BusinessProfileContentExtractionResult> {
    return subgraphWithTask(
        name = "Extract business profile content",
        llmModel = aiConfig.mode.asOrchestratorModel,
        tools = emptyList<Tool<*, *>>(),
        llmParams = LLMParams.finishToolOnlyText("submit_business_profile_content"),
        assistantResponseRepeatMax = finishToolTextAssistantResponseRepeatMax,
        finishTool = BusinessProfileContentFinishTool()
    ) { it.prompt }
}

@Serializable
private data class BusinessProfileContentToolInput(
    @property:LLMDescription("Short summary of company business in requested language.")
    val businessSummary: String? = null,
    @property:LLMDescription("Business activities as short phrases, max 8 entries.")
    val activities: List<String> = emptyList(),
    @property:LLMDescription("Model confidence from 0.0 to 1.0.")
    val confidence: Double = 0.0,
)

private class BusinessProfileContentFinishTool :
    Tool<BusinessProfileContentToolInput, BusinessProfileContentExtractionResult>(
        argsSerializer = BusinessProfileContentToolInput.serializer(),
        resultSerializer = BusinessProfileContentExtractionResult.serializer(),
        name = "submit_business_profile_content",
        description = "Submit business summary and activities extracted from provided crawled pages."
    ) {
    override suspend fun execute(args: BusinessProfileContentToolInput): BusinessProfileContentExtractionResult {
        return BusinessProfileContentExtractionResult(
            businessSummary = args.businessSummary,
            activities = args.activities,
            confidence = args.confidence.coerceIn(0.0, 1.0)
        )
    }
}

private val BusinessProfileContentExtractionInput.prompt
    get() = """
    You receive pages from one already-selected official company website.
    Do not search the web and do not suggest another website.

    Company: $companyName
    VAT: ${companyVatNumber ?: "unknown"}
    Website: $websiteUrl
    Output language: ${outputLanguage.dbValue}

    Extract:
    - businessSummary: one concise sentence about what the company does.
    - activities: short phrases (max 8), deduplicated.

    Rules:
    - Use only provided page content.
    - If content is insufficient, return null summary and empty activities.
    - Never include legal disclaimers, cookie banners, or navigation labels as activities.
    - Keep confidence in range 0.0..1.0.

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
            append("Text: ")
            append(page.textContent)
            append('\n')
            if (page.structuredDataSnippets.isNotEmpty()) {
                append("Structured: ")
                append(page.structuredDataSnippets.joinToString(" | "))
                append('\n')
            }
        }
    }}

    Always finish with tool submit_business_profile_content.
    """.trimIndent()
