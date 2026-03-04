package tech.dokus.features.ai.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.processor.ManualToolCallFixProcessor
import io.ktor.client.HttpClient
import tech.dokus.features.ai.config.AIModels
import tech.dokus.features.ai.graph.enrichBusinessGraph
import tech.dokus.features.ai.models.EnrichBusinessInput
import tech.dokus.features.ai.models.EnrichBusinessResult
import tech.dokus.features.ai.tools.enrichment.EnrichmentToolRegistry
import tech.dokus.foundation.backend.config.AIConfig

/**
 * AI Agent that enriches business information by searching the web,
 * scraping websites, and extracting logos.
 */
class BusinessEnrichmentAgent(
    private val executor: PromptExecutor,
    private val aiConfig: AIConfig,
    private val httpClient: HttpClient
) {
    suspend fun enrich(input: EnrichBusinessInput): EnrichBusinessResult {
        val toolRegistry = EnrichmentToolRegistry(
            httpClient = httpClient,
            serpApiKey = aiConfig.serpApiKey
        )

        val strategy = enrichBusinessGraph(
            aiConfig = aiConfig,
            tools = toolRegistry.tools
        )

        val agent = AIAgent(
            promptExecutor = executor,
            toolRegistry = toolRegistry,
            strategy = strategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("business-enrichment") {
                    system("You are a business researcher that finds and verifies company information online.")
                },
                model = AIModels.forMode(aiConfig.mode).orchestrator,
                maxAgentIterations = aiConfig.mode.maxIterations,
                responseProcessor = ManualToolCallFixProcessor(toolRegistry)
            )
        )

        return try {
            agent.run(input)
        } finally {
            runCatching { agent.close() }
        }
    }
}
