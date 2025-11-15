package ai.dokus.foundation.messaging.messages

import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.messaging.core.Message
import ai.dokus.foundation.messaging.core.MessageId
import ai.dokus.foundation.messaging.core.RoutingKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Audit message for publishing audit events to RabbitMQ.
 */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Serializable
data class AuditMessage(
    override val messageId: MessageId,
    override val timestamp: Instant,
    override val sourceService: String,
    val event: AuditEventData
) : Message {
    override val messageType: String = "audit"

    companion object {
        /**
         * Creates an audit message from event data.
         */
        @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
        fun from(
            event: AuditEventData,
            sourceService: String = "dokus"
        ): AuditMessage {
            return AuditMessage(
                messageId = MessageId(Uuid.random().toString()),
                timestamp = Clock.System.now(),
                sourceService = sourceService,
                event = event
            )
        }

        /**
         * Generates a routing key for an audit event.
         * Pattern: "audit.{action}.{entityType}"
         */
        fun routingKey(event: AuditEventData): RoutingKey {
            return RoutingKey("audit.${event.action.dbValue}.${event.entityType.dbValue}")
        }
    }
}

/**
 * Audit event data containing all information about the event.
 */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Serializable
data class AuditEventData(
    val tenantId: Uuid,
    val userId: Uuid?,
    val action: AuditAction,
    val entityType: EntityType,
    val entityId: String,
    val oldValues: Map<String, String>? = null,
    val newValues: Map<String, String>? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val performedAt: Instant = Clock.System.now()
)
