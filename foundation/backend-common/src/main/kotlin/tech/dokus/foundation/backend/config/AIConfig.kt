package tech.dokus.foundation.backend.config

import com.typesafe.config.Config
import tech.dokus.domain.model.ai.AiProvider

/**
 * Configuration for the AI service.
 * Supports multiple providers (Ollama for local/self-hosted, OpenAI for cloud).
 */
data class AIConfig(
    val defaultProvider: AiProvider,
    val ollama: OllamaConfig,
    val openai: OpenAIConfig,
    val models: ModelConfig
) {
    /**
     * Ollama configuration for local/self-hosted LLM inference.
     */
    data class OllamaConfig(
        val enabled: Boolean,
        val baseUrl: String,
        val defaultModel: String
    ) {
        companion object {
            fun fromConfig(config: Config): OllamaConfig = OllamaConfig(
                enabled = config.getBoolean("enabled"),
                baseUrl = config.getString("base-url"),
                defaultModel = config.getString("default-model")
            )
        }
    }

    /**
     * OpenAI configuration for cloud-based inference.
     */
    data class OpenAIConfig(
        val enabled: Boolean,
        val apiKey: String,
        val defaultModel: String
    ) {
        companion object {
            fun fromConfig(config: Config): OpenAIConfig = OpenAIConfig(
                enabled = config.getBoolean("enabled"),
                apiKey = config.getString("api-key"),
                defaultModel = config.getString("default-model")
            )
        }
    }

    /**
     * Model configuration for different AI tasks.
     */
    data class ModelConfig(
        val classification: String,
        val documentExtraction: String,
        val categorization: String,
        val suggestions: String,
        val chat: String,
        val embedding: String
    ) {
        companion object {
            fun fromConfig(config: Config): ModelConfig = ModelConfig(
                classification = config.getString("classification"),
                documentExtraction = config.getString("document-extraction"),
                categorization = config.getString("categorization"),
                suggestions = config.getString("suggestions"),
                chat = if (config.hasPath("chat")) {
                    config.getString("chat")
                } else {
                    config.getString("document-extraction")
                },
                embedding = if (config.hasPath("embedding")) config.getString("embedding") else "nomic-embed-text"
            )
        }
    }

    companion object {
        /**
         * Load AI config from HOCON configuration.
         */
        fun fromConfig(config: Config): AIConfig = AIConfig(
            defaultProvider = AiProvider.fromDbValue(config.getString("default-provider")),
            ollama = OllamaConfig.fromConfig(config.getConfig("ollama")),
            openai = OpenAIConfig.fromConfig(config.getConfig("openai")),
            models = ModelConfig.fromConfig(config.getConfig("models"))
        )
    }

    /**
     * Get the model name for a specific purpose.
     */
    fun getModel(purpose: ModelPurpose): String = when (purpose) {
        ModelPurpose.CLASSIFICATION -> models.classification
        ModelPurpose.DOCUMENT_EXTRACTION -> models.documentExtraction
        ModelPurpose.CATEGORIZATION -> models.categorization
        ModelPurpose.SUGGESTIONS -> models.suggestions
        ModelPurpose.CHAT -> models.chat
        ModelPurpose.EMBEDDING -> models.embedding
    }

    /**
     * Get the embedding model name for the configured provider.
     * Returns provider-appropriate embedding model.
     */
    fun getEmbeddingModel(): String = when (defaultProvider) {
        AiProvider.Ollama -> models.embedding
        AiProvider.OpenAi -> if (models.embedding == "nomic-embed-text") "text-embedding-3-small" else models.embedding
    }
}

/**
 * Purpose of the AI model usage, used to select the appropriate model.
 */
enum class ModelPurpose {
    /** Document type classification (invoice, receipt, etc.) */
    CLASSIFICATION,

    /** Structured data extraction from documents */
    DOCUMENT_EXTRACTION,

    /** Transaction/expense categorization */
    CATEGORIZATION,

    /** Smart suggestions and recommendations */
    SUGGESTIONS,

    /** RAG-powered chat/Q&A with documents */
    CHAT,

    /** Text embedding generation for vector search */
    EMBEDDING
}
