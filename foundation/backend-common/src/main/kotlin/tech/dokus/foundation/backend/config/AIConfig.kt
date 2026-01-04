package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

/**
 * AI Mode determines model selection and feature availability.
 *
 * - LIGHT: Resource-constrained (Raspberry Pi). Uses smaller models.
 * - NORMAL: Self-hosted/local dev. Fast models for chat, quality models for extraction.
 * - CLOUD: Dokus-managed. Same as normal + provenance tracking via Claude API.
 *
 * Document processing uses vision models (qwen3-vl) for direct image analysis.
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
 * - light: qwen3-vl:2b for vision tasks, qwen3:8b for chat
 * - normal/cloud: qwen3-vl:32b for vision tasks, qwen3:30b-a3b for chat
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
     * Get the model name for a specific purpose based on current mode.
     */
    fun getModel(purpose: ModelPurpose): String = when (mode) {
        AIMode.LIGHT -> when (purpose) {
            // Vision models for document processing
            ModelPurpose.CLASSIFICATION -> LIGHT_VISION_MODEL
            ModelPurpose.DOCUMENT_EXTRACTION -> LIGHT_VISION_MODEL
            // Text models for chat and other tasks
            ModelPurpose.CATEGORIZATION -> LIGHT_CHAT_MODEL
            ModelPurpose.SUGGESTIONS -> LIGHT_CHAT_MODEL
            ModelPurpose.CHAT -> LIGHT_CHAT_MODEL
            ModelPurpose.EMBEDDING -> EMBEDDING_MODEL
        }
        AIMode.NORMAL, AIMode.CLOUD -> when (purpose) {
            // Vision models for document processing
            ModelPurpose.CLASSIFICATION -> QUALITY_VISION_MODEL
            ModelPurpose.DOCUMENT_EXTRACTION -> QUALITY_VISION_MODEL
            // Text models for chat and other tasks
            ModelPurpose.CATEGORIZATION -> QUALITY_CHAT_MODEL
            ModelPurpose.SUGGESTIONS -> QUALITY_CHAT_MODEL
            ModelPurpose.CHAT -> QUALITY_CHAT_MODEL
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
        // Vision models (for document classification and extraction)
        const val LIGHT_VISION_MODEL = "qwen3-vl:2b"
        const val QUALITY_VISION_MODEL = "qwen3-vl:32b"

        // Chat/text models (for chat, categorization, suggestions)
        const val LIGHT_CHAT_MODEL = "qwen3:8b"
        const val QUALITY_CHAT_MODEL = "qwen3:30b-a3b"

        // Embedding model (always the same)
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
