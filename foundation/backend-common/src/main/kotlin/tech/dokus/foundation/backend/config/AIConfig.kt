package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

/**
 * AI Mode determines model selection and feature availability.
 *
 * - LIGHT: Resource-constrained (Raspberry Pi). Uses qwen3:4b for all tasks.
 * - NORMAL: Self-hosted/local dev. Fast models for chat, quality models for extraction.
 * - CLOUD: Dokus-managed. Same as normal + provenance tracking via Claude API.
 */
enum class AIMode(val configValue: String) {
    LIGHT("light"),
    NORMAL("normal"),
    CLOUD("cloud");

    companion object {
        fun fromValue(value: String): AIMode =
            entries.find { it.configValue == value.lowercase() }
                ?: throw IllegalArgumentException(
                    "Unknown AI mode: '$value'. Valid modes: light, normal, cloud"
                )
    }
}

/**
 * Simplified AI configuration using mode-based model selection.
 *
 * Mode determines which models are used for each purpose:
 * - light: qwen3:4b for everything (optimized for low resources)
 * - normal: qwen3:4b for fast tasks, qwen3:30b-a3b for extraction
 * - cloud: Same as normal + provenance via Claude (if ANTHROPIC_API_KEY set)
 */
data class AIConfig(
    val mode: AIMode,
    val ollamaHost: String,
    val anthropicApiKey: String?
) {
    /**
     * Get the model name for a specific purpose based on current mode.
     */
    fun getModel(purpose: ModelPurpose): String = when (mode) {
        AIMode.LIGHT -> FAST_MODEL // All tasks use same small model
        AIMode.NORMAL, AIMode.CLOUD -> when (purpose) {
            ModelPurpose.CLASSIFICATION -> FAST_MODEL
            ModelPurpose.CATEGORIZATION -> FAST_MODEL
            ModelPurpose.SUGGESTIONS -> FAST_MODEL
            ModelPurpose.CHAT -> FAST_MODEL
            ModelPurpose.DOCUMENT_EXTRACTION -> QUALITY_MODEL
            ModelPurpose.EMBEDDING -> EMBEDDING_MODEL
        }
    }

    /**
     * Check if provenance tracking should be enabled.
     * Only available in cloud mode with valid Anthropic API key.
     */
    fun isProvenanceEnabled(): Boolean =
        mode == AIMode.CLOUD && !anthropicApiKey.isNullOrBlank()

    companion object {
        // Model constants
        const val FAST_MODEL = "qwen3:4b"
        const val QUALITY_MODEL = "qwen3:30b-a3b"
        const val EMBEDDING_MODEL = "nomic-embed-text"

        // Default Ollama host
        const val DEFAULT_OLLAMA_HOST = "http://localhost:11434"

        /**
         * Load AI config from HOCON configuration.
         */
        fun fromConfig(config: Config): AIConfig {
            val mode = AIMode.fromValue(config.getString("mode"))
            val ollamaHost = config.getString("ollama-host")
            val anthropicApiKey = if (config.hasPath("anthropic-api-key")) {
                config.getString("anthropic-api-key").takeIf { it.isNotBlank() }
            } else {
                null
            }

            return AIConfig(
                mode = mode,
                ollamaHost = ollamaHost,
                anthropicApiKey = anthropicApiKey
            )
        }
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
