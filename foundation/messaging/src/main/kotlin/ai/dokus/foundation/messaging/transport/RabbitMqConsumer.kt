package ai.dokus.foundation.messaging.transport

import ai.dokus.foundation.messaging.core.*
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.DeliverCallback
import kotlinx.coroutines.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * RabbitMQ consumer with Flow-based message processing.
 */
class RabbitMqConsumer<T : Message>(
    private val channelName: ChannelName,
    private val serializer: KSerializer<T>,
    private val consumerConfig: ConsumerConfig,
    private val transport: RabbitMqTransport
) : MessageConsumer<T> {

    private val logger = LoggerFactory.getLogger(RabbitMqConsumer::class.java)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val processingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var consumerTag: String? = null

    override suspend fun start(handler: suspend (MessageEnvelope<T>) -> MessageResult) {
        logger.info("Starting consumer for channel ${channelName.value}")

        val channel = transport.getChannel()
            ?: throw IllegalStateException("RabbitMQ channel is not available")

        // Set prefetch count
        channel.basicQos(consumerConfig.prefetchCount)

        val deliverCallback = DeliverCallback { _, delivery ->
            processingScope.launch {
                try {
                    // Deserialize message
                    val messageBody = String(delivery.body, Charsets.UTF_8)
                    val message = json.decodeFromString(serializer, messageBody)

                    // Create envelope
                    val envelope = MessageEnvelope(
                        message = message,
                        deliveryTag = delivery.envelope.deliveryTag,
                        redelivered = delivery.envelope.isRedeliver,
                        routingKey = RoutingKey(delivery.envelope.routingKey)
                    )

                    logger.debug("Received message: ${message.messageId} (routing key: ${envelope.routingKey.value})")

                    // Process message
                    val result = handler(envelope)
                    handleMessageResult(result, envelope.deliveryTag)
                } catch (e: Exception) {
                    logger.error("Error processing message", e)
                    // Reject and don't requeue (send to DLQ)
                    transport.reject(delivery.envelope.deliveryTag, requeue = false)
                }
            }
        }

        val cancelCallback = CancelCallback { consumerTag ->
            logger.warn("Consumer $consumerTag was cancelled")
        }

        transport.startConsuming(channelName, deliverCallback, cancelCallback)
        logger.info("Consumer started for channel ${channelName.value}")
    }

    private fun handleMessageResult(result: MessageResult, deliveryTag: Long) {
        when (result) {
            is MessageResult.Ack -> {
                logger.debug("Acknowledging message $deliveryTag")
                transport.ack(deliveryTag)
            }

            is MessageResult.Reject -> {
                logger.warn("Rejecting message $deliveryTag: ${result.reason}")
                transport.reject(deliveryTag, requeue = false)
            }

            is MessageResult.Retry -> {
                logger.debug("Retrying message $deliveryTag")
                transport.reject(deliveryTag, requeue = true)
            }

            is MessageResult.DeadLetter -> {
                logger.warn("Sending message $deliveryTag to DLQ: ${result.reason}")
                transport.reject(deliveryTag, requeue = false)
            }
        }
    }

    override suspend fun stop() {
        logger.info("Stopping consumer for channel ${channelName.value}")
        processingScope.cancel()
        consumerTag?.let { tag ->
            transport.getChannel()?.basicCancel(tag)
        }
    }
}
