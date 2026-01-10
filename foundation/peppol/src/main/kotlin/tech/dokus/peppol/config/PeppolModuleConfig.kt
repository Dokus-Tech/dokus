package tech.dokus.peppol.config

import com.typesafe.config.Config

/**
 * Configuration for the Peppol module.
 *
 * Loaded from HOCON configuration files following application patterns.
 * Expected configuration path: peppol.*
 *
 * Note: Hosting mode is determined by DeploymentConfig, not this config.
 * Provider URLs are defined in [PeppolProviderConfig] sealed class,
 * not in configuration files (URLs are fixed, only API keys vary).
 */
data class PeppolModuleConfig(
    /** Default provider ID (e.g., "recommand") */
    val defaultProvider: String,

    /** Inbox polling configuration */
    val inbox: InboxConfig,

    /** Global test mode override (if true, all tenants use test mode) */
    val globalTestMode: Boolean,

    /** Master credentials for cloud-hosted deployments (null if self-hosted) */
    val masterCredentials: MasterCredentialsConfig?
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
                globalTestMode = peppolConfig.getBoolean("globalTestMode"),
                masterCredentials = if (peppolConfig.hasPath("master")) {
                    MasterCredentialsConfig.fromConfig(peppolConfig.getConfig("master"))
                } else {
                    null
                }
            )
        }
    }
}

/**
 * Master Peppol credentials for cloud-hosted deployments.
 * These are Dokus's own Recommand API credentials used for all cloud tenants.
 */
data class MasterCredentialsConfig(
    /** Dokus master API key for Recommand */
    val apiKey: String,
    /** Dokus master API secret for Recommand */
    val apiSecret: String
) {
    companion object {
        fun fromConfig(config: Config): MasterCredentialsConfig = MasterCredentialsConfig(
            apiKey = config.getString("apiKey"),
            apiSecret = config.getString("apiSecret")
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
