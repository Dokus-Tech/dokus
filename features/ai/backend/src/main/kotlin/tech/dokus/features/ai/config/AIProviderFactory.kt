package tech.dokus.features.ai.config

import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.ModelPurpose
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Factory for creating AI prompt executors and models.
 *
 * All inference runs through Ollama. Model selection is determined by [AIConfig.mode].
 *
 * ## Embedding
 * Always uses `nomic-embed-text` (768 dimensions) via Ollama.
 */
object AIProviderFactory {
    private val logger = loggerFor()

    // Embedding constants (hardcoded, not configurable)
    const val EMBEDDING_MODEL = "nomic-embed-text"
    const val EMBEDDING_DIMENSIONS = 768

    /**
     * Create a prompt executor for the configured Ollama instance.
     */
    fun createExecutor(config: AIConfig): PromptExecutor {
        logger.info("Creating Ollama executor: ${config.ollamaHost}")
        return simpleOllamaAIExecutor(config.ollamaHost)
    }

    /**
     * Get the model for a specific purpose based on mode.
     */
    fun getModel(config: AIConfig, purpose: ModelPurpose): LLModel {
        val modelName = config.getModel(purpose)
        logger.debug("Selected model for $purpose: $modelName (mode=${config.mode})")
        return createOllamaModel(modelName)
    }

    /**
     * Create an Ollama model reference.
     */
    private fun createOllamaModel(modelName: String): LLModel {
        // Estimate context length based on model name
        val contextLength = when {
            modelName.contains("4b", ignoreCase = true) -> 32768L
            modelName.contains("7b", ignoreCase = true) -> 32768L
            modelName.contains("8b", ignoreCase = true) -> 32768L
            modelName.contains("30b", ignoreCase = true) -> 131072L
            modelName.contains("32b", ignoreCase = true) -> 131072L
            else -> 32768L
        }

        return LLModel(
            provider = LLMProvider.Ollama,
            id = modelName,
            capabilities = emptyList(),
            contextLength = contextLength,
            maxOutputTokens = null
        )
    }

    /**
     * Get embedding configuration.
     * Always uses nomic-embed-text (768 dimensions) via Ollama.
     */
    fun getEmbeddingConfig(config: AIConfig): EmbeddingConfig {
        return EmbeddingConfig(
            modelName = EMBEDDING_MODEL,
            dimensions = EMBEDDING_DIMENSIONS,
            baseUrl = config.ollamaHost
        )
    }

    /**
     * Get the chat model for RAG-powered Q&A.
     */
    fun getChatModel(config: AIConfig): LLModel {
        return getModel(config, ModelPurpose.CHAT)
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
