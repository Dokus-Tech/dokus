package tech.dokus.features.ai.graph

import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.ext.agent.ConditionResult
import ai.koog.agents.ext.agent.subgraphWithRetrySimple
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.prompt.params.LLMParams
import tech.dokus.features.ai.config.AIModels
import tech.dokus.features.ai.config.assistantResponseRepeatMax
import tech.dokus.features.ai.models.EnrichBusinessInput
import tech.dokus.features.ai.models.EnrichBusinessResult
import tech.dokus.features.ai.tools.enrichment.EnrichmentToolResult
import tech.dokus.features.ai.tools.enrichment.SubmitEnrichmentTool
import tech.dokus.foundation.backend.config.AIConfig

/**
 * Koog graph strategy for business enrichment.
 *
 * Flow: Search web -> Verify website -> Scrape content -> Extract logo -> Submit results
 * Includes retry logic: approves if website OR summary was found, rejects otherwise.
 */
fun enrichBusinessGraph(
    aiConfig: AIConfig,
    tools: List<Tool<*, *>>
): AIAgentGraphStrategy<EnrichBusinessInput, EnrichBusinessResult> {
    return strategy<EnrichBusinessInput, EnrichBusinessResult>("enrich-business-graph") {

        val processWithRetry by subgraphWithRetrySimple<EnrichBusinessInput, EnrichBusinessResult>(
            name = "enrich-with-retry",
            maxRetries = 1,
            strict = false,
            conditionDescription = buildString {
                appendLine("Auto-confirm if:")
                appendLine("- A website URL was found, OR")
                appendLine("- A business summary was extracted")
                appendLine()
                appendLine("Reject if both website and summary are null/empty.")
                appendLine("On retry, try alternative search terms or check more results.")
            },
            condition = { result ->
                val hasWebsite = !result.websiteUrl.isNullOrBlank()
                val hasSummary = !result.summary.isNullOrBlank()

                if (hasWebsite || hasSummary) {
                    ConditionResult.Approve
                } else {
                    ConditionResult.Reject(
                        "No website or summary found. Try different search terms: " +
                            "include the country, VAT number prefix, or industry keywords."
                    )
                }
            }
        ) {
            val enrichSubGraph by subgraphWithTask(
                name = "Enrich business",
                llmModel = AIModels.forMode(aiConfig.mode).orchestrator,
                tools = tools,
                llmParams = LLMParams(temperature = 0.1, toolChoice = LLMParams.ToolChoice.Required),
                assistantResponseRepeatMax = assistantResponseRepeatMax,
                finishTool = SubmitEnrichmentTool()
            ) { input: EnrichBusinessInput -> input.prompt }

            val mapResult by node<EnrichmentToolResult, EnrichBusinessResult>("map-to-result") { toolResult ->
                EnrichBusinessResult(
                    websiteUrl = toolResult.websiteUrl,
                    summary = toolResult.summary,
                    activities = toolResult.activities,
                    logoUrl = toolResult.logoUrl
                )
            }

            nodeStart then enrichSubGraph then mapResult then nodeFinish
        }

        edge(nodeStart forwardTo processWithRetry)
        edge(processWithRetry forwardTo nodeFinish)
    }
}
