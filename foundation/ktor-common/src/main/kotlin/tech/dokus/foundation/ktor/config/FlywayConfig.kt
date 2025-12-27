package tech.dokus.foundation.ktor.config

import com.typesafe.config.Config

data class FlywayConfig(
    val enabled: Boolean,
    val baselineOnMigrate: Boolean,
    val baselineVersion: String,
    val locations: List<String>,
    val schemas: List<String>
) {
    companion object {
        fun fromConfig(config: Config): FlywayConfig {
            return FlywayConfig(
                enabled = config.getBoolean("enabled"),
                baselineOnMigrate = config.getBoolean("baselineOnMigrate"),
                baselineVersion = config.getString("baselineVersion"),
                locations = config.getStringList("locations"),
                schemas = config.getStringList("schemas")
            )
        }
    }
}