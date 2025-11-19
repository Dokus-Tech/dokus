package ai.dokus.foundation.ktor.config

import com.typesafe.config.Config

data class RabbitMQConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val virtualHost: String
) {
    companion object {
        fun fromConfig(config: Config): RabbitMQConfig {
            return RabbitMQConfig(
                host = config.getString("host"),
                port = config.getInt("port"),
                username = config.getString("username"),
                password = config.getString("password"),
                virtualHost = config.getString("virtualHost")
            )
        }
    }
}