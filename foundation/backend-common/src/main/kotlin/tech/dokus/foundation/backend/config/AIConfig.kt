package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

/**
 * AI configuration - simplified to just mode and Ollama connection.
 *
 * All model selection and processing strategy is determined by [IntelligenceMode].
 */
data class AIConfig(
    val mode: IntelligenceMode,
    val ollamaHost: String
) {
    companion object {
        /**
         * Load AI config from HOCON configuration.
         */
        fun fromConfig(config: Config): AIConfig {
            val mode = IntelligenceMode.fromConfigValue(config.getString("mode"))
            val ollamaHost = config.getString("ollama-host")
            return AIConfig(mode = mode, ollamaHost = ollamaHost)
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
