package ai.dokus.foundation.ktor.config

import com.typesafe.config.Config

data class KtorConfig(
    val deployment: DeploymentConfig
) {
    data class DeploymentConfig(
        val port: Int,
        val host: String,
        val environment: String
    )

    companion object {
        fun fromConfig(config: Config): KtorConfig {
            val deploymentConfig = config.getConfig("deployment")
            return KtorConfig(
                deployment = DeploymentConfig(
                    port = deploymentConfig.getInt("port"),
                    host = deploymentConfig.getString("host"),
                    environment = deploymentConfig.getString("environment")
                )
            )
        }
    }
}