package ai.dokus.foundation.messaging.messages

import ai.dokus.foundation.domain.enums.AuditAction
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.messaging.core.Message
import ai.dokus.foundation.messaging.core.MessageId
import ai.dokus.foundation.messaging.core.RoutingKey
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Audit message for publishing audit events to RabbitMQ.
 */
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
        fun from(
            event: AuditEventData,
            sourceService: String = "dokus"
        ): AuditMessage {
            return AuditMessage(
                messageId = MessageId(UUID.randomUUID().toString()),
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
@Serializable
data class AuditEventData(
    @Serializable(with = UUIDSerializer::class)
    val tenantId: UUID,
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID?,
    val action: AuditAction,
    val entityType: EntityType,
    val entityId: String,
    val oldValues: Map<String, String>? = null,
    val newValues: Map<String, String>? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val performedAt: Instant = Clock.System.now()
)

/**
 * Custom serializer for UUID to support kotlinx.serialization.
 */
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}
