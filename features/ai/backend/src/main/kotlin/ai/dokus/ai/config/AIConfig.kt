package ai.dokus.ai.config

import com.typesafe.config.Config

/**
 * Configuration for the AI service.
 * Supports multiple providers (Ollama for local/self-hosted, OpenAI for cloud).
 */
data class AIConfig(
    val defaultProvider: AIProvider,
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
        val suggestions: String
    ) {
        companion object {
            fun fromConfig(config: Config): ModelConfig = ModelConfig(
                classification = config.getString("classification"),
                documentExtraction = config.getString("document-extraction"),
                categorization = config.getString("categorization"),
                suggestions = config.getString("suggestions")
            )
        }
    }

    /**
     * Supported AI providers.
     */
    enum class AIProvider {
        OLLAMA,
        OPENAI;

        companion object {
            fun fromString(value: String): AIProvider = when (value.lowercase()) {
                "ollama" -> OLLAMA
                "openai" -> OPENAI
                else -> throw IllegalArgumentException("Unknown AI provider: $value")
            }
        }
    }

    companion object {
        /**
         * Load AI config from HOCON configuration.
         * Returns null if the 'ai' section is not present.
         */
        fun fromConfigOrNull(config: Config): AIConfig? {
            if (!config.hasPath("ai")) return null
            return fromConfig(config.getConfig("ai"))
        }

        /**
         * Load AI config from HOCON configuration.
         */
        fun fromConfig(config: Config): AIConfig = AIConfig(
            defaultProvider = AIProvider.fromString(config.getString("default-provider")),
            ollama = OllamaConfig.fromConfig(config.getConfig("ollama")),
            openai = OpenAIConfig.fromConfig(config.getConfig("openai")),
            models = ModelConfig.fromConfig(config.getConfig("models"))
        )

        /**
         * Create a default configuration for local development with Ollama.
         */
        fun localDefault(): AIConfig = AIConfig(
            defaultProvider = AIProvider.OLLAMA,
            ollama = OllamaConfig(
                enabled = true,
                baseUrl = "http://localhost:11434",
                defaultModel = "mistral:7b"
            ),
            openai = OpenAIConfig(
                enabled = false,
                apiKey = "",
                defaultModel = "gpt-4o-mini"
            ),
            models = ModelConfig(
                classification = "mistral:7b",
                documentExtraction = "mistral:7b",
                categorization = "llama3.1:8b",
                suggestions = "mistral:7b"
            )
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
    }
}

/**
 * Purpose of the AI model usage, used to select the appropriate model.
 */
enum class ModelPurpose {
    CLASSIFICATION,
    DOCUMENT_EXTRACTION,
    CATEGORIZATION,
    SUGGESTIONS
}
