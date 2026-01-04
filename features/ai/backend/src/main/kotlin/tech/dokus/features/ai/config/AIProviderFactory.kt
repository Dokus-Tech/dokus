package tech.dokus.features.ai.config

import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.ModelPurpose
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Factory for creating AI prompt executors.
 *
 * All inference runs through Ollama. Model selection is handled by [AIModels].
 */
object AIProviderFactory {
    private val logger = loggerFor()

    /**
     * Create a prompt executor for the configured Ollama instance.
     */
    fun createExecutor(config: AIConfig): PromptExecutor {
        logger.info("Creating Ollama executor: ${config.ollamaHost}")
        return simpleOllamaAIExecutor(config.ollamaHost)
    }

    /**
     * Get the model for a specific purpose based on mode.
     * Delegates to [AIModels] for type-safe model selection.
     */
    fun getModel(config: AIConfig, purpose: ModelPurpose): LLModel {
        val model = AIModels.forPurpose(config.mode, purpose)
        logger.debug("Selected model for $purpose: ${model.id} (mode=${config.mode})")
        return model
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

    /**
     * Get the chat model for RAG-powered Q&A.
     */
    fun getChatModel(config: AIConfig): LLModel {
        return AIModels.chatModel(config.mode)
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
