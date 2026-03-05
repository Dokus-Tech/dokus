package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

/**
 * Configuration for the document processing worker.
 * Processing is always enabled - documents must be processed.
 */
data class ProcessorConfig(
    val pollingInterval: Long,
    val maxAttempts: Int,
    val batchSize: Int,
    val maxConcurrentRuns: Int,
) {
    init {
        require(maxConcurrentRuns >= 1) { "maxConcurrentRuns must be >= 1, was $maxConcurrentRuns" }
    }

    companion object {
        fun fromConfig(config: Config): ProcessorConfig = ProcessorConfig(
            pollingInterval = config.getLong("pollingInterval"),
            maxAttempts = config.getInt("maxAttempts"),
            batchSize = config.getInt("batchSize"),
            maxConcurrentRuns = config.getInt("maxConcurrentRuns").coerceAtLeast(1),
        )
    }
}
