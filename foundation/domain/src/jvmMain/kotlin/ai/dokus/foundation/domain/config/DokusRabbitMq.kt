package ai.dokus.foundation.domain.config

/**
 * RabbitMQ connection configuration using BuildKonfig with environment variable overrides.
 * Provides build-time configuration for RabbitMQ endpoints across all environments,
 * with runtime override capability via environment variables (useful for Docker).
 *
 * Usage:
 * ```kotlin
 * val rabbitMq = DokusRabbitMq.current
 * val config = createDefaultRabbitMqConfig(
 *     host = rabbitMq.host,
 *     port = rabbitMq.port,
 *     username = rabbitMq.username,
 *     password = rabbitMq.password,
 *     virtualHost = rabbitMq.virtualHost,
 *     serviceName = "auth-service"
 * )
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
         * Current RabbitMQ configuration based on build environment.
         * Values are determined at compile-time via BuildKonfig, but can be overridden
         * at runtime using environment variables (useful for Docker deployments).
         */
        val current: DokusRabbitMq
            get() = DokusRabbitMq(
                host = getEnvOrBuildKonfig("RABBITMQ_HOST", BuildConfig.rabbitmqHost),
                port = getEnvOrBuildKonfigInt("RABBITMQ_PORT", BuildConfig.rabbitmqPort),
                username = getEnvOrBuildKonfig("RABBITMQ_USERNAME", BuildConfig.rabbitmqUsername),
                password = getEnvOrBuildKonfig("RABBITMQ_PASSWORD", BuildConfig.rabbitmqPassword),
                virtualHost = getEnvOrBuildKonfig("RABBITMQ_VHOST", BuildConfig.rabbitmqVirtualHost)
            )

        /**
         * Get value from environment variable or fall back to BuildKonfig value.
         */
        private fun getEnvOrBuildKonfig(envKey: String, buildKonfigValue: String): String {
            return try {
                // Platform-specific getenv will be available in JVM target
                System.getenv(envKey) ?: buildKonfigValue
            } catch (e: Exception) {
                // Fallback for non-JVM platforms
                buildKonfigValue
            }
        }

        /**
         * Get int value from environment variable or fall back to BuildKonfig value.
         */
        private fun getEnvOrBuildKonfigInt(envKey: String, buildKonfigValue: Int): Int {
            return try {
                System.getenv(envKey)?.toIntOrNull() ?: buildKonfigValue
            } catch (e: Exception) {
                buildKonfigValue
            }
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
