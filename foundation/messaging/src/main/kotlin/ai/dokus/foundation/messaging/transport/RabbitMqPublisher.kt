package ai.dokus.foundation.messaging.transport

import ai.dokus.foundation.messaging.core.ChannelName
import ai.dokus.foundation.messaging.core.Message
import ai.dokus.foundation.messaging.core.MessagePublisher
import ai.dokus.foundation.messaging.core.PublishResult
import ai.dokus.foundation.messaging.core.RoutingKey
import com.rabbitmq.client.AMQP
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.github.resilience4j.kotlin.retry.executeSuspendFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * RabbitMQ publisher with circuit breaker and retry logic.
 */
class RabbitMqPublisher<T : Message>(
    private val channelName: ChannelName,
    private val serializer: KSerializer<T>,
    private val config: PublisherConfig,
    private val transport: RabbitMqTransport
) : MessagePublisher<T> {

    private val logger = LoggerFactory.getLogger(RabbitMqPublisher::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val circuitBreaker: CircuitBreaker? = if (config.circuitBreaker.enabled) {
        val cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(config.circuitBreaker.failureRateThreshold)
            .slowCallRateThreshold(config.circuitBreaker.slowCallRateThreshold)
            .waitDurationInOpenState(Duration.ofMillis(config.circuitBreaker.waitDurationInOpenState.inWholeMilliseconds))
            .slidingWindowSize(config.circuitBreaker.slidingWindowSize)
            .minimumNumberOfCalls(config.circuitBreaker.minimumNumberOfCalls)
            .build()

        CircuitBreaker.of("rabbitmq-publisher-${channelName.value}", cbConfig)
    } else {
        null
    }

    private val retry: Retry = Retry.of(
        "rabbitmq-publisher-${channelName.value}",
        RetryConfig.custom<Any>()
            .maxAttempts(config.retry.maxAttempts)
            .waitDuration(Duration.ofMillis(config.retry.initialDelay.inWholeMilliseconds))
            .build()
    )

    override suspend fun publish(message: T, routingKey: RoutingKey): PublishResult {
        return try {
            if (circuitBreaker != null) {
                circuitBreaker.executeSuspendFunction {
                    retry.executeSuspendFunction {
                        doPublish(message, routingKey)
                    }
                }
            } else {
                retry.executeSuspendFunction {
                    doPublish(message, routingKey)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to publish message after retries", e)
            PublishResult.Failure(e, "Failed to publish message: ${e.message}")
        }
    }

    private suspend fun doPublish(message: T, routingKey: RoutingKey): PublishResult {
        return withContext(Dispatchers.IO) {
            try {
                val channel = transport.getChannel()
                    ?: return@withContext PublishResult.Failure(
                        IllegalStateException("Channel not available"),
                        "RabbitMQ channel is not available"
                    )

                // Serialize message to JSON
                val messageBody = json.encodeToString(serializer, message)
                val bodyBytes = messageBody.toByteArray(Charsets.UTF_8)

                // Build properties
                val properties = AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .contentEncoding("UTF-8")
                    .deliveryMode(2) // Persistent
                    .build()

                // Publish message
                channel.basicPublish(
                    transport.getExchangeName(),
                    routingKey.value,
                    properties,
                    bodyBytes
                )

                // Wait for confirmation if enabled
                if (config.confirmMode) {
                    val confirmed = channel.waitForConfirms(config.confirmTimeoutMs)
                    if (!confirmed) {
                        logger.warn("Message not confirmed by broker")
                        return@withContext PublishResult.Rejected("Broker did not confirm message")
                    }
                }

                logger.debug("Published message to ${routingKey.value}: ${message.messageId}")
                PublishResult.Success
            } catch (e: Exception) {
                logger.error("Error publishing message", e)
                throw e
            }
        }
    }

    override suspend fun close() {
        logger.info("Closing publisher for channel ${channelName.value}")
    }
}
