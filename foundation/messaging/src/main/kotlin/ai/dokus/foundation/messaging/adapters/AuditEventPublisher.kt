package ai.dokus.foundation.messaging.adapters

import ai.dokus.foundation.messaging.messages.AuditEventData

/**
 * Interface for publishing audit events.
 * This is the domain-level interface that services use.
 */
interface AuditEventPublisher {
    /**
     * Publishes an audit event.
     * @param event The audit event data to publish
     * @return Result indicating success or failure
     */
    suspend fun publish(event: AuditEventData): Result<Unit>
}
