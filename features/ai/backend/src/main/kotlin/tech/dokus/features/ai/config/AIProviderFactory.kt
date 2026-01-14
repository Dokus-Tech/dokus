package tech.dokus.features.ai.config

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

        return ThrottledPromptExecutor(baseExecutor, maxConcurrent)
    }

    /**
     * Get all models for a given config.
     */
    fun getModels(config: AIConfig): ModelSet {
        return AIModels.forMode(config.mode)
    }

    /**
     * Get embedding configuration.
     * Always uses nomic-embed-text (768 dimensions) via Ollama.
     */
    fun getEmbeddingConfig(config: AIConfig): EmbeddingConfig {
        return EmbeddingConfig(
            modelName = AIModels.EMBEDDING_MODEL_NAME,
            dimensions = AIModels.EMBEDDING_DIMENSIONS,
            baseUrl = config.ollamaHost
        )
    }
}

/**
 * Configuration for embedding model.
 */
data class EmbeddingConfig(
    val modelName: String,
    val dimensions: Int,
    val baseUrl: String
)
