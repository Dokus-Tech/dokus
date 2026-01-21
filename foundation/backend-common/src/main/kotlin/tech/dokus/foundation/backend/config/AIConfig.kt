package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

/**
 * AI configuration - simplified to just mode and Ollama connection.
 *
 * All model selection and processing strategy is determined by [IntelligenceMode].
 */
data class AIConfig(
    val mode: IntelligenceMode,
    val ollamaHost: String,
    val lmStudioHost: String
) {
    companion object {
        /**
         * Load AI config from HOCON configuration.
         */
        fun fromConfig(config: Config): AIConfig {
            val mode = IntelligenceMode.fromConfigValue(config.getString("mode"))
            val ollamaHost = config.getString("ollama-host")
            val lmStudioHost = config.getString("lm-studio-host")
            return AIConfig(mode = mode, ollamaHost = ollamaHost, lmStudioHost = lmStudioHost)
        }
    }
}