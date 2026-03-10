package tech.dokus.backend.services.contacts.sse

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactChangedEventDto
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

private const val DefaultBufferCapacity = 32

private data class ContactEventKey(
    val tenantId: TenantId,
    val contactId: ContactId,
)

/**
 * Per-contact SSE event hub with auto-eviction of idle flows.
 */
internal class ContactEventHub {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val streams = ConcurrentHashMap<ContactEventKey, MutableSharedFlow<ContactChangedEventDto>>()

    fun eventsFor(
        tenantId: TenantId,
        contactId: ContactId,
    ): SharedFlow<ContactChangedEventDto> {
        val key = ContactEventKey(tenantId, contactId)
        return streams.computeIfAbsent(key) { k ->
            createFlow().also { flow -> scheduleEviction(k, flow) }
        }.asSharedFlow()
    }

    fun publish(
        tenantId: TenantId,
        contactId: ContactId,
        event: ContactChangedEventDto,
    ) {
        val key = ContactEventKey(tenantId, contactId)
        streams[key]?.tryEmit(event)
    }

    private fun createFlow(): MutableSharedFlow<ContactChangedEventDto> {
        return MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = DefaultBufferCapacity,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }

    private fun scheduleEviction(key: ContactEventKey, flow: MutableSharedFlow<ContactChangedEventDto>) {
        scope.launch {
            while (true) {
                val subscribed = withTimeoutOrNull(EvictionDelay) {
                    flow.subscriptionCount.first { it > 0 }
                }
                if (subscribed == null) {
                    streams.remove(key, flow)
                    break
                }
                flow.subscriptionCount.first { it == 0 }
                delay(EvictionDelay)
                if (flow.subscriptionCount.value == 0) {
                    streams.remove(key, flow)
                    break
                }
            }
        }
    }

    companion object {
        private val EvictionDelay = 30.seconds
    }
}

/**
 * Publishes contact change events to connected SSE clients.
 */
internal class ContactSsePublisher(
    private val contactEventHub: ContactEventHub,
) {
    fun publishContactChanged(
        tenantId: TenantId,
        contactId: ContactId,
        reason: String? = null,
    ) {
        contactEventHub.publish(
            tenantId = tenantId,
            contactId = contactId,
            event = ContactChangedEventDto(contactId = contactId, reason = reason),
        )
    }
}
