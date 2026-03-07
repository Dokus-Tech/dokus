package tech.dokus.backend.services.documents.sse

import java.util.concurrent.ConcurrentHashMap
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
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentCollectionChangedEventDto
import kotlin.time.Duration.Companion.seconds

private const val DefaultBufferCapacity = 32

internal sealed interface DocumentSnapshotSignal {
    data object Changed : DocumentSnapshotSignal
    data object Deleted : DocumentSnapshotSignal
}

private data class DocumentSnapshotKey(
    val tenantId: TenantId,
    val documentId: DocumentId,
)

internal class DocumentCollectionEventHub {
    private val streams = ConcurrentHashMap<TenantId, MutableSharedFlow<DocumentCollectionChangedEventDto>>()

    fun eventsFor(tenantId: TenantId): SharedFlow<DocumentCollectionChangedEventDto> = streamFor(tenantId).asSharedFlow()

    fun publish(
        tenantId: TenantId,
        event: DocumentCollectionChangedEventDto,
    ) {
        streamFor(tenantId).tryEmit(event)
    }

    private fun streamFor(tenantId: TenantId): MutableSharedFlow<DocumentCollectionChangedEventDto> {
        return streams.getOrPut(tenantId) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = DefaultBufferCapacity,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
    }
}

internal class DocumentSnapshotEventHub {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val streams = ConcurrentHashMap<DocumentSnapshotKey, MutableSharedFlow<DocumentSnapshotSignal>>()

    fun eventsFor(
        tenantId: TenantId,
        documentId: DocumentId,
    ): SharedFlow<DocumentSnapshotSignal> {
        val key = DocumentSnapshotKey(tenantId, documentId)
        return streams.getOrPut(key) {
            createFlow().also { flow -> scheduleEviction(key, flow) }
        }.asSharedFlow()
    }

    fun publishChanged(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        val key = DocumentSnapshotKey(tenantId, documentId)
        streams[key]?.tryEmit(DocumentSnapshotSignal.Changed)
    }

    fun publishDeleted(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        val key = DocumentSnapshotKey(tenantId, documentId)
        streams[key]?.tryEmit(DocumentSnapshotSignal.Deleted)
    }

    private fun createFlow(): MutableSharedFlow<DocumentSnapshotSignal> {
        return MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = DefaultBufferCapacity,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    }

    private fun scheduleEviction(key: DocumentSnapshotKey, flow: MutableSharedFlow<DocumentSnapshotSignal>) {
        scope.launch {
            while (true) {
                flow.subscriptionCount.first { it > 0 }
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

internal class DocumentSsePublisher(
    private val documentSnapshotEventHub: DocumentSnapshotEventHub,
    private val documentCollectionEventHub: DocumentCollectionEventHub,
) {
    fun publishDocumentChanged(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        documentSnapshotEventHub.publishChanged(tenantId, documentId)
        documentCollectionEventHub.publish(
            tenantId = tenantId,
            event = DocumentCollectionChangedEventDto(documentId = documentId),
        )
    }

    fun publishDocumentDeleted(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        documentSnapshotEventHub.publishDeleted(tenantId, documentId)
        documentCollectionEventHub.publish(
            tenantId = tenantId,
            event = DocumentCollectionChangedEventDto(documentId = documentId),
        )
    }

}
