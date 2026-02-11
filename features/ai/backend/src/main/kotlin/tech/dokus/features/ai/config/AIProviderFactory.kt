package tech.dokus.features.ai.config

import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Factory for creating AI prompt executors.
 *
 * All inference runs through Ollama. Model selection is handled by [AIModels].
 */
object AIProviderFactory {
    private val logger = loggerFor()

    /**
     * Create a throttled executor that respects concurrency limits.
     *
     * CRITICAL: Without throttling, parallel agents can overwhelm Ollama
     * causing OOM, timeouts, or model unload/reload thrashing.
     */
    fun createOllamaExecutor(config: AIConfig): PromptExecutor {
        return simpleOllamaAIExecutor(config.ollamaHost).also {
            logger.info(
                "Creating Ollama executor: {} (mode: {})",
                config.ollamaHost,
                config.mode.name
            )
        }
    }

    /**
     * Create an OpenAI-compatible executor for LM Studio.
     *
     * LM Studio exposes an OpenAI-compatible API at http://localhost:1234/v1 by default.
     */
    fun createOpenAiExecutor(config: AIConfig): PromptExecutor {
        logger.info(
            "Creating LM Studio executor: {} (mode: {})",
            config.lmStudioHost,
            config.mode.name
        )

        val client = OpenAILLMClient(
            apiKey = "",
            settings = OpenAIClientSettings(baseUrl = config.lmStudioHost)
        )
        return SingleLLMPromptExecutor(client)
    }
}
