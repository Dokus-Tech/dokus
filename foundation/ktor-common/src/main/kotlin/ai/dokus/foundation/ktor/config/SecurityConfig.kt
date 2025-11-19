package ai.dokus.foundation.ktor.config

import com.typesafe.config.Config

data class SecurityConfig(
    val cors: Cors,
) {
    data class Cors(
        val allowedHosts: List<String>
    )

    companion object {
        fun fromConfig(config: Config): SecurityConfig {
            val corsConfig = config.getConfig("cors")

            return SecurityConfig(
                cors = Cors(
                    allowedHosts = corsConfig.getStringList("allowedHosts")
                )
            )
        }
    }
}