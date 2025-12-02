package ai.dokus.peppol.backend.config

import com.typesafe.config.Config

/**
 * Peppol service configuration.
 */
data class PeppolConfig(
    val encryptionKey: String,
    val recommand: RecommandConfig,
    val cashflowService: ServiceConfig,
    val authService: ServiceConfig,
    val inbox: InboxConfig
) {
    companion object {
        fun fromConfig(config: Config): PeppolConfig {
            val peppolConfig = config.getConfig("peppol")
            return PeppolConfig(
                encryptionKey = peppolConfig.getString("encryptionKey"),
                recommand = RecommandConfig.fromConfig(peppolConfig.getConfig("recommand")),
                cashflowService = ServiceConfig.fromConfig(peppolConfig.getConfig("cashflowService")),
                authService = ServiceConfig.fromConfig(peppolConfig.getConfig("authService")),
                inbox = InboxConfig.fromConfig(peppolConfig.getConfig("inbox"))
            )
        }
    }
}

/**
 * Recommand API configuration.
 */
data class RecommandConfig(
    val baseUrl: String,
    val testUrl: String
) {
    companion object {
        fun fromConfig(config: Config): RecommandConfig = RecommandConfig(
            baseUrl = config.getString("baseUrl"),
            testUrl = config.getString("testUrl")
        )
    }
}

/**
 * Inter-service communication configuration.
 */
data class ServiceConfig(
    val baseUrl: String,
    val timeout: Long
) {
    companion object {
        fun fromConfig(config: Config): ServiceConfig = ServiceConfig(
            baseUrl = config.getString("baseUrl"),
            timeout = config.getLong("timeout")
        )
    }
}

/**
 * Inbox polling configuration.
 */
data class InboxConfig(
    val pollingEnabled: Boolean,
    val pollingIntervalSeconds: Int
) {
    companion object {
        fun fromConfig(config: Config): InboxConfig = InboxConfig(
            pollingEnabled = config.getBoolean("pollingEnabled"),
            pollingIntervalSeconds = config.getInt("pollingIntervalSeconds")
        )
    }
}
