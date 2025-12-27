package tech.dokus.foundation.ktor.config

import com.typesafe.config.Config

/**
 * Configuration for the document processing worker.
 */
data class ProcessorConfig(
    val enabled: Boolean,
    val pollingInterval: Long,
    val maxAttempts: Int,
    val batchSize: Int
) {
    companion object {
        fun fromConfig(config: Config): ProcessorConfig = ProcessorConfig(
            enabled = if (config.hasPath("enabled")) config.getBoolean("enabled") else true,
            pollingInterval = config.getLong("pollingInterval"),
            maxAttempts = config.getInt("maxAttempts"),
            batchSize = config.getInt("batchSize")
        )
    }
}
