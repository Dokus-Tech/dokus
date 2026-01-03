package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

data class MetricsConfig(
    val enabled: Boolean,
    val prometheusPath: String
) {
    companion object {
        fun fromConfig(config: Config): MetricsConfig {
            return MetricsConfig(
                enabled = config.getBoolean("enabled"),
                prometheusPath = config.getString("prometheusPath")
            )
        }
    }
}
