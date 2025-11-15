package ai.dokus.foundation.messaging.core

/**
 * Interface for consuming messages from the message bus.
 */
interface MessageConsumer<T : Message> {
    /**
     * Starts consuming messages and processing them with the provided handler.
     * @param handler Function to process each message envelope
     */
    suspend fun start(handler: suspend (MessageEnvelope<T>) -> MessageResult)

    /**
     * Stops consuming messages and releases resources.
     */
    suspend fun stop()
}
