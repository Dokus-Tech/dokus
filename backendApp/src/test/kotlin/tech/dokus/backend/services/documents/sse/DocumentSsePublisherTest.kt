package tech.dokus.backend.services.documents.sse

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentCollectionChangedEventDto
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentSsePublisherTest {

    @Test
    fun `publishDocumentChanged emits detail and collection invalidation`() = runTest {
        val tenantId = TenantId.generate()
        val documentId = DocumentId.generate()
        val collectionHub = DocumentCollectionEventHub()
        val snapshotHub = DocumentSnapshotEventHub()
        val publisher = DocumentSsePublisher(snapshotHub, collectionHub)

        val detailSignal = async(start = CoroutineStart.UNDISPATCHED) {
            snapshotHub.eventsFor(tenantId, documentId).first()
        }
        val collectionEvent = async(start = CoroutineStart.UNDISPATCHED) {
            collectionHub.eventsFor(tenantId).first()
        }

        publisher.publishDocumentChanged(tenantId, documentId)

        assertEquals(DocumentSnapshotSignal.Changed, withTimeout(1_000) { detailSignal.await() })
        assertEquals(
            DocumentCollectionChangedEventDto(documentId = documentId),
            withTimeout(1_000) { collectionEvent.await() }
        )
    }

    @Test
    fun `publishDocumentDeleted emits deleted detail and collection invalidation`() = runTest {
        val tenantId = TenantId.generate()
        val documentId = DocumentId.generate()
        val collectionHub = DocumentCollectionEventHub()
        val snapshotHub = DocumentSnapshotEventHub()
        val publisher = DocumentSsePublisher(snapshotHub, collectionHub)

        val detailSignal = async(start = CoroutineStart.UNDISPATCHED) {
            snapshotHub.eventsFor(tenantId, documentId).first()
        }
        val collectionEvent = async(start = CoroutineStart.UNDISPATCHED) {
            collectionHub.eventsFor(tenantId).first()
        }

        publisher.publishDocumentDeleted(tenantId, documentId)

        assertEquals(DocumentSnapshotSignal.Deleted, withTimeout(1_000) { detailSignal.await() })
        assertEquals(
            DocumentCollectionChangedEventDto(documentId = documentId),
            withTimeout(1_000) { collectionEvent.await() }
        )
    }
}
