package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

data class BusinessProfileEnrichmentConfig(
    val enabled: Boolean,
    val pollingIntervalMs: Long,
    val batchSize: Int,
    val maxAttempts: Int,
    val staleLeaseMinutes: Long,
    val maxPages: Int,
    val serperApiKey: String,
    val serperBaseUrl: String,
    val ignoreRobots: Boolean,
) {
    companion object {
        fun fromConfig(config: Config): BusinessProfileEnrichmentConfig = BusinessProfileEnrichmentConfig(
            enabled = config.getBoolean("enabled"),
            pollingIntervalMs = config.getLong("pollingIntervalMs"),
            batchSize = config.getInt("batchSize"),
            maxAttempts = config.getInt("maxAttempts"),
            staleLeaseMinutes = config.getLong("staleLeaseMinutes"),
            maxPages = config.getInt("maxPages"),
            serperApiKey = config.getString("serperApiKey"),
            serperBaseUrl = config.getString("serperBaseUrl"),
            ignoreRobots = config.getBoolean("ignoreRobots"),
        )
    }
}
