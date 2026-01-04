package tech.dokus.peppol.config

import com.typesafe.config.Config

/**
 * Configuration for the Peppol module.
 *
 * Loaded from HOCON configuration files following application patterns.
 * Expected configuration path: peppol.*
 */
data class PeppolModuleConfig(
    /** Default provider ID (e.g., "recommand") */
    val defaultProvider: String,

    /** Recommand provider configuration */
    val recommand: RecommandConfig,

    /** Inbox polling configuration */
    val inbox: InboxConfig,

    /** Global test mode override (if true, all tenants use test mode) */
    val globalTestMode: Boolean
) {
    companion object {
        /**
         * Create config from HOCON configuration.
         */
        fun fromConfig(config: Config): PeppolModuleConfig {
            val peppolConfig = config.getConfig("peppol")
            return PeppolModuleConfig(
                defaultProvider = peppolConfig.getString("defaultProvider"),
                recommand = RecommandConfig.fromConfig(peppolConfig.getConfig("recommand")),
                inbox = InboxConfig.fromConfig(peppolConfig.getConfig("inbox")),
                globalTestMode = peppolConfig.getBoolean("globalTestMode")
            )
        }
    }
}

/**
 * Recommand provider configuration.
 */
data class RecommandConfig(
    /** Production API base URL */
    val baseUrl: String,
    /** Test/sandbox API base URL */
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
 * Inbox polling configuration.
 */
data class InboxConfig(
    /** Whether inbox polling is enabled */
    val pollingEnabled: Boolean,
    /** Polling interval in seconds */
    val pollingIntervalSeconds: Int
) {
    companion object {
        fun fromConfig(config: Config): InboxConfig = InboxConfig(
            pollingEnabled = config.getBoolean("pollingEnabled"),
            pollingIntervalSeconds = config.getInt("pollingIntervalSeconds")
        )
    }
}
