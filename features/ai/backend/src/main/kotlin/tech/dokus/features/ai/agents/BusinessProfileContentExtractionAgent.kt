package tech.dokus.features.ai.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import tech.dokus.features.ai.config.asOrchestratorModel
import tech.dokus.features.ai.config.installKoogEventLogging
import tech.dokus.features.ai.config.installLangfuseTracing
import tech.dokus.features.ai.graph.businessProfileContentExtractionGraph
import tech.dokus.features.ai.models.BusinessProfileContentExtractionInput
import tech.dokus.features.ai.models.BusinessProfileContentExtractionResult
import tech.dokus.foundation.backend.config.AIConfig

class BusinessProfileContentExtractionAgent(
    private val executor: PromptExecutor,
    private val aiConfig: AIConfig,
) {
    @OptIn(ExperimentalAgentsApi::class)
    suspend fun extract(input: BusinessProfileContentExtractionInput): BusinessProfileContentExtractionResult {
        val strategy = businessProfileContentExtractionGraph(aiConfig)
        val agent = AIAgent(
            promptExecutor = executor,
            toolRegistry = ToolRegistry.EMPTY,
            strategy = strategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("koog-business-profile-content-extraction") {
                    system("You extract business summaries from provided website content only.")
                },
                model = aiConfig.mode.asOrchestratorModel,
                maxAgentIterations = aiConfig.mode.maxIterations,
            ),
            installFeatures = {
                installKoogEventLogging(
                    agentName = "business-profile-content-extraction",
                    enabled = aiConfig.koogEventLoggingEnabled
                )
                installLangfuseTracing(aiConfig.langfuse)
            }
        )

        return try {
            agent.run(input)
        } finally {
            runCatching { agent.close() }
        }
    }
}
