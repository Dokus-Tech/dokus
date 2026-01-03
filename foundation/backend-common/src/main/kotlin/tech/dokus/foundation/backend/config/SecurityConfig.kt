package tech.dokus.foundation.backend.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigValueType

data class SecurityConfig(
    val cors: Cors,
) {
    data class Cors(
        val allowedHosts: List<String>
    )

    companion object {
        fun fromConfig(config: Config): SecurityConfig {
            val corsConfig = config.getConfig("cors")

            // Handle both list format and comma-separated string format
            // This allows setting CORS_ALLOWED_HOSTS as either:
            //   - ["host1:port", "host2:port"] (HOCON list)
            //   - "host1:port,host2:port" or "*" (string, e.g., from env var)
            val allowedHosts = if (corsConfig.getValue("allowedHosts").valueType() == ConfigValueType.LIST) {
                corsConfig.getStringList("allowedHosts")
            } else {
                val rawValue = corsConfig.getString("allowedHosts")
                if (rawValue == "*") {
                    listOf("*")
                } else {
                    rawValue.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }
            }

            return SecurityConfig(
                cors = Cors(
                    allowedHosts = allowedHosts
                )
            )
        }
    }
}
