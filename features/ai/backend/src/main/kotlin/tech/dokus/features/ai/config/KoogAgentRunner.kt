package tech.dokus.features.ai.config

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentGraphStrategy
import ai.koog.agents.core.annotation.ExperimentalAgentsApi
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.executor.model.PromptExecutor
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.ServerInfoConfig

class KoogAgentRunner(
    @PublishedApi internal val executor: PromptExecutor,
    @PublishedApi internal val aiConfig: AIConfig,
    @PublishedApi internal val serverInfo: ServerInfoConfig,
) {
    @OptIn(ExperimentalAgentsApi::class)
    suspend inline fun <reified Input, reified Output> run(
        input: Input,
        strategy: AIAgentGraphStrategy<Input, Output>,
        agentName: String,
        systemPrompt: String,
        model: LLModel = aiConfig.mode.asVisionModel,
        toolRegistry: ToolRegistry = ToolRegistry.EMPTY,
        traceContext: LangfuseTraceContext = LangfuseTraceContext(),
    ): Output {
        val agent = AIAgent(
            promptExecutor = executor,
            toolRegistry = toolRegistry,
            strategy = strategy,
            agentConfig = AIAgentConfig(
                prompt = prompt("koog-$agentName") { system(systemPrompt) },
                model = model,
                maxAgentIterations = aiConfig.mode.maxIterations,
            ),
            installFeatures = {
                installKoogEventLogging(
                    agentName = agentName,
                    enabled = aiConfig.koogEventLoggingEnabled
                )
                installLangfuseTracing(
                    aiConfig.langfuse,
                    traceContext,
                    serviceName = serverInfo.name,
                    serviceVersion = serverInfo.version,
                )
            }
        )
        return try {
            agent.run(input)
        } finally {
            runCatching { agent.close() }
        }
    }
}
