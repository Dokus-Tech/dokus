package ai.dokus.foundation.messaging.transport

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for RabbitMQ transport.
 */
data class RabbitMqTransportConfig(
    val connection: ConnectionConfig,
    val exchange: ExchangeConfig,
    val queue: QueueConfig,
    val publisher: PublisherConfig,
    val consumer: ConsumerConfig,
    val deadLetter: DeadLetterConfig
)

/**
 * Connection configuration.
 */
data class ConnectionConfig(
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val virtualHost: String,
    val connectionName: String,
    val connectionTimeoutMs: Int = 30000,
    val requestedHeartbeatMs: Int = 60000,
    val networkRecoveryIntervalMs: Int = 5000,
    val automaticRecoveryEnabled: Boolean = true,
    val topologyRecoveryEnabled: Boolean = true
)

/**
 * Exchange configuration.
 */
data class ExchangeConfig(
    val name: String,
    val type: String = "topic",
    val durable: Boolean = true,
    val autoDelete: Boolean = false
)

/**
 * Queue configuration.
 */
data class QueueConfig(
    val name: String,
    val durable: Boolean = true,
    val exclusive: Boolean = false,
    val autoDelete: Boolean = false,
    val messageTtlMs: Long? = 86400000 // 24 hours
)

/**
 * Publisher configuration.
 */
data class PublisherConfig(
    val confirmMode: Boolean = true,
    val confirmTimeoutMs: Long = 5000,
    val compression: Boolean = false,
    val circuitBreaker: CircuitBreakerConfig = CircuitBreakerConfig(),
    val retry: RetryConfig = RetryConfig()
)

/**
 * Circuit breaker configuration.
 */
data class CircuitBreakerConfig(
    val enabled: Boolean = true,
    val failureRateThreshold: Float = 50f,
    val slowCallRateThreshold: Float = 50f,
    val waitDurationInOpenState: Duration = 10.seconds,
    val slidingWindowSize: Int = 100,
    val minimumNumberOfCalls: Int = 10
)

/**
 * Retry configuration.
 */
data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 100.milliseconds,
    val maxDelay: Duration = 10.seconds,
    val multiplier: Double = 2.0
)

/**
 * Consumer configuration.
 */
data class ConsumerConfig(
    val prefetchCount: Int = 100,
    val autoAck: Boolean = false,
    val maxRetries: Int = 3,
    val concurrency: Int = 1
)

/**
 * Dead letter configuration.
 */
data class DeadLetterConfig(
    val enabled: Boolean = true,
    val exchangeName: String = "dokus.dlx",
    val queueName: String = "dokus.dlq",
    val routingKey: String = "dead"
)
