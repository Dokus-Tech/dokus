package ai.dokus.ai.config

import tech.dokus.domain.model.ai.AIProvider
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaModels
import tech.dokus.foundation.ktor.config.AIConfig
import tech.dokus.foundation.ktor.config.ModelPurpose
import tech.dokus.foundation.ktor.utils.loggerFor

/**
 * Factory for creating AI prompt executors and models based on configuration.
 *
 * Supports both LLM models (for text generation) and embedding models (for vector search/RAG).
 *
 * ## Embedding Models
 *
 * The factory provides configuration for embedding models used in RAG (Retrieval Augmented Generation):
 * - **Ollama**: Uses `nomic-embed-text` (768 dimensions) by default
 * - **OpenAI**: Uses `text-embedding-3-small` (1536 dimensions) by default
 *
 * Note: Embedding generation is handled by [ai.dokus.ai.services.EmbeddingService] using direct
 * HTTP calls to provider APIs, as Koog doesn't have built-in embedding support for Ollama.
 */
object AIProviderFactory {
    private val logger = loggerFor()
    // =========================================================================
    // Embedding Model Constants
    // =========================================================================

    /** Default embedding model for Ollama (768 dimensions) */
    const val OLLAMA_EMBEDDING_MODEL = "nomic-embed-text"

    /** Default embedding model for OpenAI (1536 dimensions) */
    const val OPENAI_EMBEDDING_MODEL = "text-embedding-3-small"

    /** Embedding dimensions for Ollama nomic-embed-text model */
    const val OLLAMA_EMBEDDING_DIMENSIONS = 768

    /** Embedding dimensions for OpenAI text-embedding-3-small model */
    const val OPENAI_EMBEDDING_DIMENSIONS = 1536

    /**
     * Create a prompt executor based on the configured provider.
     */
    fun createExecutor(config: AIConfig): PromptExecutor {
        return when (config.defaultProvider) {
            AIProvider.OLLAMA -> {
                logger.info("Creating Ollama executor: ${config.ollama.baseUrl}")
                simpleOllamaAIExecutor(config.ollama.baseUrl)
            }

            AIProvider.OPENAI -> {
                logger.info("Creating OpenAI executor")
                simpleOpenAIExecutor(config.openai.apiKey)
            }
        }
    }

    /**
     * Get the model for a specific purpose based on the configured provider.
     */
    fun getModel(config: AIConfig, purpose: ModelPurpose): LLModel {
        val modelName = config.getModel(purpose)
        return when (config.defaultProvider) {
            AIProvider.OLLAMA -> createOllamaModel(modelName)
            AIProvider.OPENAI -> createOpenAIModel(modelName)
        }
    }

    /**
     * Create an Ollama model reference.
     * For models not predefined in Koog, creates a custom LLModel.
     */
    private fun createOllamaModel(modelName: String): LLModel {
        return when {
            // Meta Llama models
            modelName.startsWith("llama3.2") || modelName == "llama3:latest" -> OllamaModels.Meta.LLAMA_3_2
            modelName.startsWith("llama3.2:3b") -> OllamaModels.Meta.LLAMA_3_2_3B

            // Alibaba Qwen models
            modelName.startsWith("qwq") -> OllamaModels.Alibaba.QWQ
            modelName.startsWith("qwen2.5:0.5") -> OllamaModels.Alibaba.QWEN_2_5_05B
            modelName.startsWith("qwen3:0.6") -> OllamaModels.Alibaba.QWEN_3_06B

            // For all other models (including Mistral), create custom model
            else -> {
                logger.info("Creating custom Ollama model: $modelName")
                createCustomOllamaModel(modelName)
            }
        }
    }

    /**
     * Create a custom Ollama model for models not predefined in Koog.
     */
    private fun createCustomOllamaModel(modelName: String): LLModel {
        // Estimate context length based on model name
        val contextLength = when {
            modelName.contains("7b", ignoreCase = true) -> 32768L
            modelName.contains("8b", ignoreCase = true) -> 32768L
            modelName.contains("3b", ignoreCase = true) -> 8192L
            modelName.contains("13b", ignoreCase = true) -> 4096L
            else -> 32768L // Default
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
     * Create an OpenAI model reference.
     */
    private fun createOpenAIModel(modelName: String): LLModel {
        return when (modelName) {
            "gpt-4o", "gpt4o" -> OpenAIModels.Chat.GPT4o
            "gpt-4o-mini", "gpt4o-mini" -> OpenAIModels.CostOptimized.GPT4oMini
            "gpt-4.1", "gpt-4-1" -> OpenAIModels.Chat.GPT4_1
            "gpt-4.1-mini" -> OpenAIModels.CostOptimized.GPT4_1Mini
            "gpt-4.1-nano" -> OpenAIModels.CostOptimized.GPT4_1Nano
            "gpt-5" -> OpenAIModels.Chat.GPT5
            "gpt-5-mini" -> OpenAIModels.Chat.GPT5Mini
            "o3-mini" -> OpenAIModels.CostOptimized.O3Mini
            "o4-mini" -> OpenAIModels.CostOptimized.O4Mini
            else -> {
                logger.warn("Unknown OpenAI model: $modelName, defaulting to GPT-4o-mini")
                OpenAIModels.CostOptimized.GPT4oMini
            }
        }
    }

    // =========================================================================
    // Embedding Configuration Methods
    // =========================================================================

    /**
     * Get embedding configuration for the specified provider.
     *
     * @param config The AI configuration
     * @return EmbeddingConfig with model name, dimensions, and provider info
     */
    fun getEmbeddingConfig(config: AIConfig): EmbeddingConfig {
        return when (config.defaultProvider) {
            AIProvider.OLLAMA -> EmbeddingConfig(
                modelName = config.getEmbeddingModel(),
                dimensions = getEmbeddingDimensions(config.getEmbeddingModel()),
                provider = config.defaultProvider,
                baseUrl = config.ollama.baseUrl
            )

            AIProvider.OPENAI -> EmbeddingConfig(
                modelName = config.getEmbeddingModel(),
                dimensions = getEmbeddingDimensions(config.getEmbeddingModel()),
                provider = config.defaultProvider,
                baseUrl = "https://api.openai.com/v1"
            )
        }
    }

    /**
     * Get the embedding dimensions for a specific model.
     *
     * @param modelName The embedding model name
     * @return Number of dimensions in the embedding vector
     */
    fun getEmbeddingDimensions(modelName: String): Int {
        return when (modelName) {
            // Ollama models
            "nomic-embed-text" -> OLLAMA_EMBEDDING_DIMENSIONS
            "mxbai-embed-large" -> 1024
            "all-minilm" -> 384
            "bge-base-en" -> 768
            "bge-large-en" -> 1024

            // OpenAI models
            "text-embedding-3-small" -> OPENAI_EMBEDDING_DIMENSIONS
            "text-embedding-3-large" -> 3072
            "text-embedding-ada-002" -> 1536

            // Default to Ollama dimensions for unknown models
            else -> {
                logger.warn("Unknown embedding model: $modelName, assuming $OLLAMA_EMBEDDING_DIMENSIONS dimensions")
                OLLAMA_EMBEDDING_DIMENSIONS
            }
        }
    }

    /**
     * Get the chat model for RAG-powered Q&A.
     *
     * @param config The AI configuration
     * @return LLModel for chat/Q&A operations
     */
    fun getChatModel(config: AIConfig): LLModel {
        return getModel(config, ModelPurpose.CHAT)
    }

    /**
     * Check if the configured provider supports embeddings.
     *
     * @param config The AI configuration
     * @return true if embeddings are supported
     */
    fun supportsEmbeddings(config: AIConfig): Boolean {
        return when (config.defaultProvider) {
            AIProvider.OLLAMA -> config.ollama.enabled
            AIProvider.OPENAI -> config.openai.enabled && config.openai.apiKey.isNotBlank()
        }
    }
}

/**
 * Configuration for embedding model.
 *
 * @property modelName The name of the embedding model
 * @property dimensions The number of dimensions in the embedding vector
 * @property provider The AI provider (OLLAMA or OPENAI)
 * @property baseUrl The base URL for the embedding API
 */
data class EmbeddingConfig(
    val modelName: String,
    val dimensions: Int,
    val provider: AIProvider,
    val baseUrl: String
)
