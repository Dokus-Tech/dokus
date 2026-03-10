package tech.dokus.features.ai.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.processor.ManualToolCallFixProcessor
import tech.dokus.features.ai.config.asVisionModel
import tech.dokus.features.ai.graph.AcceptDocumentInput
import tech.dokus.features.ai.graph.acceptDocumentGraph
import tech.dokus.features.ai.graph.purposeEnrichmentGraph
import tech.dokus.features.ai.models.DocumentAiProcessingResult
import tech.dokus.features.ai.models.PurposeEnrichmentInput
import tech.dokus.features.ai.models.PurposeEnrichmentResult
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.foundation.backend.config.AIConfig

class DocumentProcessingAgent(
    private val executor: PromptExecutor,
    private val aiConfig: AIConfig,
    private val documentFetcher: DocumentFetcher
) {
    @OptIn(ExperimentalAgentsApi::class)
    suspend fun process(input: AcceptDocumentInput): DocumentAiProcessingResult {
        val strategy = acceptDocumentGraph(
            aiConfig = aiConfig,
            documentFetcher = documentFetcher
        )

        val agent = AIAgent(
            promptExecutor = executor,
            toolRegistry = ToolRegistry.EMPTY,
            strategy = strategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("koog-agents") { system("You are a document processor.") },
                model = aiConfig.mode.asVisionModel,
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

    @OptIn(ExperimentalAgentsApi::class)
    suspend fun enrichPurpose(input: PurposeEnrichmentInput): PurposeEnrichmentResult {
        val strategy = purposeEnrichmentGraph()
        val agent = AIAgent(
            promptExecutor = executor,
            toolRegistry = ToolRegistry.EMPTY,
            strategy = strategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("koog-purpose-enrichment") {
                    system("You render canonical document purpose labels.")
                },
                model = aiConfig.mode.asVisionModel,
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
