package tech.dokus.foundation.backend.config

import com.typesafe.config.Config
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * AI configuration - simplified to just mode and Ollama connection.
 *
 * All model selection and processing strategy is determined by [IntelligenceMode].
 */
data class AIConfig(
    val mode: IntelligenceMode,
    val ollamaHost: String,
    val lmStudioHost: String,
) {
    val llmRequestTimeout: Duration = 15.minutes
    val llmConnectTimeout: Duration = 60.seconds
    val llmSocketTimeout: Duration = 15.minutes
    val llmRetryMaxAttempts: Int = 3
    val llmRetryInitialDelay: Duration = 1.seconds
    val llmRetryMaxDelay: Duration = 30.seconds
    val koogEventLoggingEnabled: Boolean = true

    companion object {
        /**
         * Load AI config from HOCON configuration.
         */
        fun fromConfig(config: Config): AIConfig {
            val mode = IntelligenceMode.fromDbValue(config.getString("mode"))
            val ollamaHost = config.getString("ollama-host")
            val lmStudioHost = config.getString("lm-studio-host")
            return AIConfig(
                mode = mode,
                ollamaHost = ollamaHost,
                lmStudioHost = lmStudioHost,
            )
        }
    }
}
