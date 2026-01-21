package tech.dokus.foundation.backend.config

import com.typesafe.config.Config
import tech.dokus.domain.enums.ContactLinkPolicy

/**
 * Configuration for the document processing worker.
 * Processing is always enabled - documents must be processed.
 */
data class ProcessorConfig(
    val pollingInterval: Long,
    val maxAttempts: Int,
    val batchSize: Int,
    val linkingPolicy: ContactLinkPolicy = ContactLinkPolicy.VatOnly
) {
    companion object {
        fun fromConfig(config: Config): ProcessorConfig = ProcessorConfig(
            pollingInterval = config.getLong("pollingInterval"),
            maxAttempts = config.getInt("maxAttempts"),
            batchSize = config.getInt("batchSize"),
            linkingPolicy = ContactLinkPolicy.fromConfig(
                if (config.hasPath("linkingPolicy")) config.getString("linkingPolicy") else null
            )
        )
    }
}
