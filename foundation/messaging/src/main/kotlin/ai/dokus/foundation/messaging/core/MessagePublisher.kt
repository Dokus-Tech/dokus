package ai.dokus.foundation.messaging.core

/**
 * Interface for publishing messages to the message bus.
 */
interface MessagePublisher<T : Message> {
    /**
     * Publishes a message with the specified routing key.
     * @param message The message to publish
     * @param routingKey The routing key for the message
     * @return Result of the publish operation
     */
    suspend fun publish(message: T, routingKey: RoutingKey): PublishResult

    /**
     * Closes the publisher and releases resources.
     */
    suspend fun close()
}

/**
 * Result of a publish operation.
 */
sealed class PublishResult {
    /**
     * Message was published successfully and confirmed by the broker.
     */
    data object Success : PublishResult()

    /**
     * Message publish failed.
     */
    data class Failure(val error: Throwable, val message: String? = null) : PublishResult()

    /**
     * Message publish was rejected by the broker.
     */
    data class Rejected(val reason: String) : PublishResult()

    val asResult: Result<Unit>
        get() = when (this) {
            is Success -> Result.success(Unit)
            is Failure -> Result.failure(error)
            is Rejected -> Result.failure(IllegalStateException("Message rejected: $reason"))
        }
}
