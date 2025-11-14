package ai.dokus.foundation.messaging.integration

import ai.dokus.foundation.domain.config.BuildConfig
import ai.dokus.foundation.messaging.transport.*

/**
 * Helper object to load RabbitMQ configuration from environment variables with BuildKonfig fallbacks.
 */
object RabbitMqConfigLoader {
    /**
     * Loads RabbitMQ transport configuration from environment variables, falling back to BuildKonfig defaults.
     * @param serviceName Name of the service (e.g., "auth-service", "audit-service")
     * @return Configured RabbitMqTransportConfig
     */
    fun load(serviceName: String): RabbitMqTransportConfig {
        val host = System.getenv("RABBITMQ_HOST") ?: BuildConfig.rabbitmqHost
        val port = System.getenv("RABBITMQ_PORT")?.toIntOrNull() ?: BuildConfig.rabbitmqPort
        val username = System.getenv("RABBITMQ_USERNAME") ?: BuildConfig.rabbitmqUsername
        val password = System.getenv("RABBITMQ_PASSWORD") ?: BuildConfig.rabbitmqPassword
        val virtualHost = System.getenv("RABBITMQ_VHOST") ?: BuildConfig.rabbitmqVirtualHost

        return createDefaultRabbitMqConfig(
            host = host,
            port = port,
            username = username,
            password = password,
            virtualHost = virtualHost,
            serviceName = serviceName
        )
    }
}
