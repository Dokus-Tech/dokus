package ai.dokus.foundation.messaging.integration

import ai.dokus.foundation.messaging.adapters.AuditEventPublisher
import ai.dokus.foundation.messaging.adapters.AuditPublisherAdapter
import ai.dokus.foundation.messaging.core.ChannelName
import ai.dokus.foundation.messaging.messages.AuditMessage
import ai.dokus.foundation.messaging.transport.*
import kotlinx.serialization.serializer
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Creates a Koin module for RabbitMQ messaging with the specified configuration.
 *
 * @param config RabbitMQ transport configuration
 * @param sourceService Name of the service using the messaging system
 * @return Koin module with messaging dependencies
 */
fun messagingModule(
    config: RabbitMqTransportConfig,
    sourceService: String
): Module = module {
    // RabbitMQ Transport (Singleton)
    single {
        RabbitMqTransport(config).apply {
            connect()
        }
    }

    // Audit Message Publisher
    single {
        val transport: RabbitMqTransport = get()
        transport.createPublisher(
            channelName = ChannelName("audit.events"),
            serializer = serializer<AuditMessage>(),
            publisherConfig = config.publisher
        )
    }

    // Audit Event Publisher Adapter (Domain Interface)
    single<AuditEventPublisher> {
        AuditPublisherAdapter(
            publisher = get(),
            sourceService = sourceService
        )
    }
}

/**
 * Helper function to create default RabbitMQ configuration.
 */
fun createDefaultRabbitMqConfig(
    host: String,
    port: Int,
    username: String,
    password: String,
    virtualHost: String,
    serviceName: String
): RabbitMqTransportConfig {
    return RabbitMqTransportConfig(
        connection = ConnectionConfig(
            host = host,
            port = port,
            username = username,
            password = password,
            virtualHost = virtualHost,
            connectionName = serviceName,
            connectionTimeoutMs = 30000,
            requestedHeartbeatMs = 60000,
            networkRecoveryIntervalMs = 5000,
            automaticRecoveryEnabled = true,
            topologyRecoveryEnabled = true
        ),
        exchange = ExchangeConfig(
            name = "dokus.events",
            type = "topic",
            durable = true,
            autoDelete = false
        ),
        queue = QueueConfig(
            name = "$serviceName.queue",
            durable = true,
            exclusive = false,
            autoDelete = false,
            messageTtlMs = 86400000 // 24 hours
        ),
        publisher = PublisherConfig(
            confirmMode = true,
            confirmTimeoutMs = 5000,
            compression = false,
            circuitBreaker = CircuitBreakerConfig(
                enabled = true,
                failureRateThreshold = 50f,
                slowCallRateThreshold = 50f
            ),
            retry = RetryConfig(
                maxAttempts = 3
            )
        ),
        consumer = ConsumerConfig(
            prefetchCount = 100,
            autoAck = false,
            maxRetries = 3,
            concurrency = 1
        ),
        deadLetter = DeadLetterConfig(
            enabled = true,
            exchangeName = "dokus.dlx",
            queueName = "dokus.dlq",
            routingKey = "dead"
        )
    )
}
