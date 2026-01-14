package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

/**
 * AI Mode determines model selection and feature availability.
 *
 * - LIGHT: Resource-constrained (<16GB RAM). Uses smallest models.
 * - MEDIUM: Mid-range (32-48GB RAM). Sequential ensemble with medium models.
 * - NORMAL: Full power (64GB+ RAM). Parallel ensemble with largest models.
 * - CLOUD: Dokus-managed. Same as NORMAL + provenance tracking via Claude API.
 *
 * Document processing uses vision models (qwen3-vl) for direct image analysis.
 */
enum class AIMode(val configValue: String) {
    LIGHT("light"),
    MEDIUM("medium"),
    NORMAL("normal"),
    CLOUD("cloud");

    companion object {
        fun fromValue(value: String): AIMode =
            entries.find { it.configValue == value.lowercase() }
                ?: throw IllegalArgumentException(
                    "Unknown AI mode: '$value'. Valid modes: light, medium, normal, cloud"
                )
    }
}

/**
 * AI configuration for mode-based model selection.
 *
 * Mode determines which models are used (defined in AIModels):
 * - light: Smaller models for resource-constrained environments
 * - normal/cloud: Larger models for quality
 *
 * Document processing (classification, extraction) uses vision models that
 * analyze document images directly, eliminating the need for OCR.
 */
data class AIConfig(
    val mode: AIMode,
    val ollamaHost: String,
    val anthropicApiKey: String?
) {
    /**
     * Check if provenance tracking should be enabled.
     * Only available in cloud mode with valid Anthropic API key.
     */
    fun isProvenanceEnabled(): Boolean =
        mode == AIMode.CLOUD && !anthropicApiKey.isNullOrBlank()

    companion object {
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
