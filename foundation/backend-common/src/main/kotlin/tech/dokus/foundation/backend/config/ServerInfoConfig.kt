package tech.dokus.foundation.backend.config

import com.typesafe.config.Config
import tech.dokus.domain.config.appVersion

/**
 * Configuration for the server info endpoint.
 *
 * Used by the /api/v1/server/info endpoint to provide
 * server metadata to clients during connection validation.
 */
data class ServerInfoConfig(
    val name: String,
    val environment: String,
    val bankingEnabled: Boolean,
    val paymentsEnabled: Boolean
) {
    val version: String = appVersion.versionName

    companion object {
        fun fromConfig(config: Config): ServerInfoConfig {
            return ServerInfoConfig(
                name = config.getString("name"),
                environment = config.getString("environment"),
                bankingEnabled = config.getBoolean("bankingEnabled"),
                paymentsEnabled = config.getBoolean("paymentsEnabled")
            )
        }
    }
}
