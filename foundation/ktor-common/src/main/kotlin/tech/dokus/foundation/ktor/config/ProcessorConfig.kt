package tech.dokus.foundation.ktor.config

import com.typesafe.config.Config

/**
 * Configuration for the document processing worker.
 * Processing is always enabled - documents must be processed.
 */
data class ProcessorConfig(
    val pollingInterval: Long,
    val maxAttempts: Int,
    val batchSize: Int
) {
    companion object {
        fun fromConfig(config: Config): ProcessorConfig = ProcessorConfig(
            pollingInterval = config.getLong("pollingInterval"),
            maxAttempts = config.getInt("maxAttempts"),
            batchSize = config.getInt("batchSize")
        )
    }
}
