package ai.dokus.foundation.messaging.adapters

import ai.dokus.foundation.messaging.core.ChannelName
import ai.dokus.foundation.messaging.core.MessagePublisher
import ai.dokus.foundation.messaging.messages.AuditEventData
import ai.dokus.foundation.messaging.messages.AuditMessage
import org.slf4j.LoggerFactory

/**
 * Adapter that bridges the domain's AuditEventPublisher interface
 * to the messaging system's MessagePublisher.
 */
class AuditPublisherAdapter(
    private val publisher: MessagePublisher<AuditMessage>,
    private val sourceService: String = "dokus"
) : AuditEventPublisher {

    private val logger = LoggerFactory.getLogger(AuditPublisherAdapter::class.java)

    override suspend fun publish(event: AuditEventData): Result<Unit> {
        return try {
            val message = AuditMessage.from(event, sourceService)
            val routingKey = AuditMessage.routingKey(event)

            logger.debug(
                "Publishing audit event: action=${event.action.dbValue}, " +
                "entityType=${event.entityType.dbValue}, entityId=${event.entityId}"
            )

            val result = publisher.publish(message, routingKey)
            result.asResult
        } catch (e: Exception) {
            logger.error("Failed to publish audit event", e)
            Result.failure(e)
        }
    }
}
