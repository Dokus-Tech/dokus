package ai.dokus.foundation.messaging.core

import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Base interface for all messages that can be published to the message bus.
 */
@OptIn(ExperimentalTime::class)
interface Message {
    val messageId: MessageId
    val timestamp: Instant
    val sourceService: String
    val messageType: String
}

/**
 * Type-safe wrapper for message IDs.
 */
@JvmInline
@Serializable
value class MessageId(val value: String)

/**
 * Type-safe wrapper for routing keys.
 */
@JvmInline
@Serializable
value class RoutingKey(val value: String)

/**
 * Type-safe wrapper for channel names.
 */
@JvmInline
@Serializable
value class ChannelName(val value: String)

/**
 * Envelope containing a message with metadata from the message broker.
 */
data class MessageEnvelope<T : Message>(
    val message: T,
    val deliveryTag: Long,
    val redelivered: Boolean,
    val routingKey: RoutingKey
)

/**
 * Result of message processing.
 */
sealed class MessageResult {
    /**
     * Message processed successfully - acknowledge it.
     */
    data object Ack : MessageResult()

    /**
     * Message processing failed permanently - reject and send to DLQ.
     */
    data class Reject(val reason: String? = null) : MessageResult()

    /**
     * Message processing failed temporarily - retry later.
     */
    data class Retry(val delay: kotlin.time.Duration? = null) : MessageResult()

    /**
     * Message should be sent to dead letter queue.
     */
    data class DeadLetter(val reason: String? = null) : MessageResult()
}
