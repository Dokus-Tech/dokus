package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

/**
 * Configuration for the server info endpoint.
 *
 * Used by the /api/v1/server/info endpoint to provide
 * server metadata to clients during connection validation.
 */
data class ServerInfoConfig(
    val name: String,
    val version: String,
    val environment: String,
    val bankingEnabled: Boolean,
    val paymentsEnabled: Boolean
) {
    companion object {
        fun fromConfig(config: Config): ServerInfoConfig {
            return ServerInfoConfig(
                name = config.getString("name"),
                version = config.getString("version"),
                environment = config.getString("environment"),
                bankingEnabled = config.getBoolean("bankingEnabled"),
                paymentsEnabled = config.getBoolean("paymentsEnabled")
            )
        }
    }
}
