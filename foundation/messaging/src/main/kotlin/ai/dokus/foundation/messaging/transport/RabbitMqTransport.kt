package ai.dokus.foundation.messaging.transport

import ai.dokus.foundation.messaging.core.ChannelName
import ai.dokus.foundation.messaging.core.Message
import ai.dokus.foundation.messaging.core.MessageConsumer
import ai.dokus.foundation.messaging.core.MessagePublisher
import com.rabbitmq.client.*
import kotlinx.serialization.KSerializer
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeoutException

/**
 * RabbitMQ transport implementation.
 * Manages connection, channel, and topology declaration.
 */
class RabbitMqTransport(
    private val config: RabbitMqTransportConfig
) {
    private val logger = LoggerFactory.getLogger(RabbitMqTransport::class.java)
    private var connection: Connection? = null
    private var channel: Channel? = null

    private val connectionFactory = ConnectionFactory().apply {
        host = config.connection.host
        port = config.connection.port
        username = config.connection.username
        password = config.connection.password
        virtualHost = config.connection.virtualHost
        connectionTimeout = config.connection.connectionTimeoutMs
        requestedHeartbeat = config.connection.requestedHeartbeatMs / 1000
        networkRecoveryInterval = config.connection.networkRecoveryIntervalMs.toLong()
        isAutomaticRecoveryEnabled = config.connection.automaticRecoveryEnabled
        isTopologyRecoveryEnabled = config.connection.topologyRecoveryEnabled
    }

    /**
     * Connects to RabbitMQ and declares topology.
     */
    @Throws(IOException::class, TimeoutException::class)
    fun connect() {
        try {
            logger.info("Connecting to RabbitMQ at ${config.connection.host}:${config.connection.port}")
            connection = connectionFactory.newConnection(config.connection.connectionName)
            channel = connection?.createChannel()

            // Enable publisher confirms if configured
            if (config.publisher.confirmMode) {
                channel?.confirmSelect()
            }

            declareTopology()
            logger.info("Successfully connected to RabbitMQ")
        } catch (e: Exception) {
            logger.error("Failed to connect to RabbitMQ", e)
            throw e
        }
    }

    /**
     * Declares RabbitMQ topology (exchanges, queues, bindings).
     */
    private fun declareTopology() {
        channel?.let { ch ->
            logger.info("Declaring RabbitMQ topology")

            // Declare main exchange
            ch.exchangeDeclare(
                config.exchange.name,
                config.exchange.type,
                config.exchange.durable,
                config.exchange.autoDelete,
                emptyMap()
            )
            logger.debug("Declared exchange: ${config.exchange.name}")

            // Build queue arguments
            val queueArgs = mutableMapOf<String, Any>()
            config.queue.messageTtlMs?.let { queueArgs["x-message-ttl"] = it }

            if (config.deadLetter.enabled) {
                queueArgs["x-dead-letter-exchange"] = config.deadLetter.exchangeName
                queueArgs["x-dead-letter-routing-key"] = config.deadLetter.routingKey
            }

            // Declare main queue
            ch.queueDeclare(
                config.queue.name,
                config.queue.durable,
                config.queue.exclusive,
                config.queue.autoDelete,
                queueArgs
            )
            logger.debug("Declared queue: ${config.queue.name}")

            // Bind queue to exchange (catch-all pattern)
            ch.queueBind(config.queue.name, config.exchange.name, "#")
            logger.debug("Bound queue ${config.queue.name} to exchange ${config.exchange.name}")

            // Declare dead letter exchange and queue
            if (config.deadLetter.enabled) {
                ch.exchangeDeclare(
                    config.deadLetter.exchangeName,
                    "topic",
                    true,
                    false,
                    emptyMap()
                )
                logger.debug("Declared dead letter exchange: ${config.deadLetter.exchangeName}")

                ch.queueDeclare(
                    config.deadLetter.queueName,
                    true,
                    false,
                    false,
                    emptyMap()
                )
                logger.debug("Declared dead letter queue: ${config.deadLetter.queueName}")

                ch.queueBind(
                    config.deadLetter.queueName,
                    config.deadLetter.exchangeName,
                    config.deadLetter.routingKey
                )
                logger.debug("Bound dead letter queue to exchange")
            }

            logger.info("Topology declaration completed")
        }
    }

    /**
     * Creates a typed message publisher.
     */
    fun <T : Message> createPublisher(
        channelName: ChannelName,
        serializer: KSerializer<T>,
        publisherConfig: PublisherConfig = config.publisher
    ): MessagePublisher<T> {
        require(channel != null) { "Transport not connected. Call connect() first." }
        return RabbitMqPublisher(
            channelName = channelName,
            serializer = serializer,
            config = publisherConfig,
            transport = this
        )
    }

    /**
     * Creates a typed message consumer.
     */
    fun <T : Message> createConsumer(
        channelName: ChannelName,
        serializer: KSerializer<T>,
        consumerConfig: ConsumerConfig = config.consumer
    ): MessageConsumer<T> {
        require(channel != null) { "Transport not connected. Call connect() first." }
        return RabbitMqConsumer(
            channelName = channelName,
            serializer = serializer,
            consumerConfig = consumerConfig,
            transport = this
        )
    }

    /**
     * Starts consuming from a queue with the provided callback.
     */
    internal fun startConsuming(
        channelName: ChannelName,
        deliverCallback: DeliverCallback,
        cancelCallback: CancelCallback
    ) {
        channel?.basicConsume(
            config.queue.name,
            config.consumer.autoAck,
            deliverCallback,
            cancelCallback
        )
    }

    /**
     * Gets the underlying RabbitMQ channel.
     */
    internal fun getChannel(): Channel? = channel

    /**
     * Gets the exchange name.
     */
    internal fun getExchangeName(): String = config.exchange.name

    /**
     * Acknowledges a message.
     */
    internal fun ack(deliveryTag: Long) {
        channel?.basicAck(deliveryTag, false)
    }

    /**
     * Rejects a message with optional requeue.
     */
    internal fun reject(deliveryTag: Long, requeue: Boolean = false) {
        channel?.basicReject(deliveryTag, requeue)
    }

    /**
     * Closes the connection and releases resources.
     */
    fun close() {
        try {
            logger.info("Closing RabbitMQ connection")
            channel?.close()
            connection?.close()
            logger.info("RabbitMQ connection closed")
        } catch (e: Exception) {
            logger.error("Error closing RabbitMQ connection", e)
        }
    }
}
