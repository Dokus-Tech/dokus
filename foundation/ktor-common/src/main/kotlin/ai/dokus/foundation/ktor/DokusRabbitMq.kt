package ai.dokus.foundation.ktor

/**
 * RabbitMQ connection configuration using typed config from AppBaseConfig.
 * Configuration flows from environment variables → HOCON config → Kotlin config objects.
 *
 * Usage:
 * ```kotlin
 * val baseConfig = AppBaseConfig.load()
 * val rabbitMq = DokusRabbitMq.from(baseConfig.rabbitmq)
 * val uri = rabbitMq.toAmqpUri()
 * ```
 */
data class DokusRabbitMq(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val virtualHost: String
) {
    companion object {
        /**
         * Create DokusRabbitMq from typed RabbitMQConfig.
         * This follows the pattern: Env → Conf → Kotlin Config
         */
        fun from(config: RabbitMQConfig): DokusRabbitMq {
            return DokusRabbitMq(
                host = config.host,
                port = config.port,
                username = config.username,
                password = config.password,
                virtualHost = config.virtualHost
            )
        }
    }

    /**
     * Constructs the full AMQP URI for connection.
     * Format: amqp://username:password@host:port/virtualHost
     */
    fun toAmqpUri(): String {
        return "amqp://$username:$password@$host:$port$virtualHost"
    }
}
