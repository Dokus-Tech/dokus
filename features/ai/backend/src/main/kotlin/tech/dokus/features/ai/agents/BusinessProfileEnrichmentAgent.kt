package tech.dokus.features.ai.agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.processor.ManualToolCallFixProcessor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import tech.dokus.features.ai.config.asOrchestratorModel
import tech.dokus.features.ai.graph.businessProfileEnrichmentGraph
import tech.dokus.features.ai.models.BusinessProfileDiscoveryResult
import tech.dokus.features.ai.models.BusinessProfileEnrichmentInput
import tech.dokus.features.ai.tools.BusinessDiscoveryRegistry
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig

class BusinessProfileEnrichmentAgent(
    private val executor: PromptExecutor,
    private val aiConfig: AIConfig,
    private val enrichmentConfig: BusinessProfileEnrichmentConfig,
) : KoinComponent {
    @OptIn(ExperimentalAgentsApi::class)
    suspend fun enrich(input: BusinessProfileEnrichmentInput): BusinessProfileDiscoveryResult {
        val registry by inject<ToolRegistry>(named<BusinessDiscoveryRegistry>()) {
            parametersOf(
                BusinessDiscoveryRegistry.Args(
                    maxPages = input.maxPages,
                    ignoreRobots = enrichmentConfig.ignoreRobots
                )
            )
        }

        val strategy = businessProfileEnrichmentGraph(aiConfig, registry)
        val agent = AIAgent(
            promptExecutor = executor,
            toolRegistry = registry,
            strategy = strategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("koog-business-profile-enrichment") {
                    system("You discover official company websites and return strict structured output.")
                },
                model = aiConfig.mode.asOrchestratorModel,
                maxAgentIterations = aiConfig.mode.maxIterations,
                responseProcessor = ManualToolCallFixProcessor(registry)
            )
        )

        return try {
            agent.run(input)
        } finally {
            runCatching { agent.close() }
        }
    }
}
