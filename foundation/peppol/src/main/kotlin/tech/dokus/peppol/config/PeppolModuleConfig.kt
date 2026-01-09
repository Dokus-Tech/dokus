package tech.dokus.peppol.config

import com.typesafe.config.Config

/**
 * Configuration for the Peppol module.
 *
 * Loaded from HOCON configuration files following application patterns.
 * Expected configuration path: peppol.*
 *
 * Note: Provider URLs are defined in [PeppolProviderConfig] sealed class,
 * not in configuration files (URLs are fixed, only API keys vary).
 */
data class PeppolModuleConfig(
    /** Default provider ID (e.g., "recommand") */
    val defaultProvider: String,

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
                inbox = InboxConfig.fromConfig(peppolConfig.getConfig("inbox")),
                globalTestMode = peppolConfig.getBoolean("globalTestMode")
            )
        }
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
