package tech.dokus.backend.services.documents.sse

import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentCollectionChangedEventDto

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

    fun eventsFor(tenantId: TenantId): Flow<DocumentCollectionChangedEventDto> = streamFor(tenantId).asSharedFlow()

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
    private val streams = ConcurrentHashMap<DocumentSnapshotKey, MutableSharedFlow<DocumentSnapshotSignal>>()

    fun eventsFor(
        tenantId: TenantId,
        documentId: DocumentId,
    ): Flow<DocumentSnapshotSignal> = streamFor(DocumentSnapshotKey(tenantId, documentId)).asSharedFlow()

    fun publishChanged(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        streamFor(DocumentSnapshotKey(tenantId, documentId)).tryEmit(DocumentSnapshotSignal.Changed)
    }

    fun publishDeleted(
        tenantId: TenantId,
        documentId: DocumentId,
    ) {
        streamFor(DocumentSnapshotKey(tenantId, documentId)).tryEmit(DocumentSnapshotSignal.Deleted)
    }

    private fun streamFor(key: DocumentSnapshotKey): MutableSharedFlow<DocumentSnapshotSignal> {
        return streams.getOrPut(key) {
            MutableSharedFlow(
                replay = 0,
                extraBufferCapacity = DefaultBufferCapacity,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
            )
        }
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

    fun publishDocumentsChanged(
        tenantId: TenantId,
        documentIds: Iterable<DocumentId>,
    ) {
        val uniqueDocumentIds = documentIds.toSet()
        if (uniqueDocumentIds.isEmpty()) return

        uniqueDocumentIds.forEach { documentId ->
            documentSnapshotEventHub.publishChanged(tenantId, documentId)
        }
        documentCollectionEventHub.publish(
            tenantId = tenantId,
            event = DocumentCollectionChangedEventDto(documentId = uniqueDocumentIds.singleOrNull()),
        )
    }
}
