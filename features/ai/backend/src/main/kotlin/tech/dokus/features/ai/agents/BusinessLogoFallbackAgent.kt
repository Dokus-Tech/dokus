package tech.dokus.features.ai.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.processor.ManualToolCallFixProcessor
import tech.dokus.features.ai.config.asOrchestratorModel
import tech.dokus.features.ai.graph.businessLogoFallbackGraph
import tech.dokus.features.ai.models.BusinessLogoFallbackInput
import tech.dokus.features.ai.models.BusinessLogoFallbackResult
import tech.dokus.foundation.backend.config.AIConfig

class BusinessLogoFallbackAgent(
    private val executor: PromptExecutor,
    private val aiConfig: AIConfig,
) {
    @OptIn(ExperimentalAgentsApi::class)
    suspend fun findLogoCandidates(input: BusinessLogoFallbackInput): BusinessLogoFallbackResult {
        val strategy = businessLogoFallbackGraph(aiConfig)
        val agent = AIAgent(
            promptExecutor = executor,
            toolRegistry = ToolRegistry.EMPTY,
            strategy = strategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("koog-business-logo-fallback") {
                    system("You identify company logo asset URLs from provided HTML snippets only.")
                },
                model = aiConfig.mode.asOrchestratorModel,
                maxAgentIterations = aiConfig.mode.maxIterations,
                responseProcessor = ManualToolCallFixProcessor(ToolRegistry.EMPTY)
            )
        )

        return try {
            agent.run(input)
        } finally {
            runCatching { agent.close() }
        }
    }
}
