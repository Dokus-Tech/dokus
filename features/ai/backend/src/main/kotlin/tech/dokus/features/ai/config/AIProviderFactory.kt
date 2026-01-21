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
    fun createExecutor(config: AIConfig): PromptExecutor {
        val baseExecutor = simpleOllamaAIExecutor(config.ollamaHost)
        val maxConcurrent = config.mode.maxConcurrentRequests

        logger.info(
            "Creating Ollama executor: {} (max concurrent: {}, mode: {})",
            config.ollamaHost,
            maxConcurrent,
            config.mode.name
        )

        return wrapWithThrottling(baseExecutor, maxConcurrent)
    }

    /**
     * Create an OpenAI-compatible executor for LM Studio.
     *
     * LM Studio exposes an OpenAI-compatible API at http://localhost:1234/v1 by default.
     */
    fun createOpenAiExecutor(config: AIConfig): PromptExecutor {
        val baseUrl = config.lmStudioHost
        val maxConcurrent = config.mode.maxConcurrentRequests

        logger.info(
            "Creating LM Studio executor: {} (max concurrent: {}, mode: {})",
            baseUrl,
            maxConcurrent,
            config.mode.name
        )

        val client = OpenAILLMClient(
            apiKey = "",
            settings = OpenAIClientSettings(baseUrl = baseUrl)
        )
        val baseExecutor = SingleLLMPromptExecutor(client)

        return wrapWithThrottling(baseExecutor, maxConcurrent)
    }
}
