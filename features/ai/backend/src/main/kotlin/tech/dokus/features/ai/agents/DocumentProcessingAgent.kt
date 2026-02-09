package tech.dokus.features.ai.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.graph.acceptDocumentGraph
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.foundation.backend.config.AIConfig

class DocumentProcessingAgent(
    private val executor: PromptExecutor,
    private val aiConfig: AIConfig,
    private val documentFetcher: DocumentFetcher
) {
    @OptIn(ExperimentalAgentsApi::class)
    suspend fun process(input: AcceptDocumentInput): DocumentAiProcessingResult {
        val toolRegistry = ToolRegistry.EMPTY
        val strategy = acceptDocumentGraph(
            aiConfig = aiConfig,
            _registries = emptyList(),
            documentFetcher = documentFetcher
        )

        val agent = AIAgent(
            promptExecutor = executor,
            toolRegistry = toolRegistry,
            strategy = strategy,
            agentConfig = AIAgentConfig.withSystemPrompt(
                prompt = "You are a document processor.",
                llm = aiConfig.mode.asVisionModel,
                maxAgentIterations = aiConfig.mode.maxIterations
            )
        )

        return try {
            agent.run(input)
        } finally {
            runCatching { agent.close() }
        }
    }
}
