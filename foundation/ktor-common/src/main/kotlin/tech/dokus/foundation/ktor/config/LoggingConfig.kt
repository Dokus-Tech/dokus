package tech.dokus.foundation.ktor.config

import com.typesafe.config.Config

data class LoggingConfig(
    val level: String,
    val consoleJson: Boolean
) {
    companion object {
        fun fromConfig(config: Config): LoggingConfig {
            return LoggingConfig(
                level = config.getString("level"),
                consoleJson = config.getBoolean("consoleJson")
            )
        }
    }
}