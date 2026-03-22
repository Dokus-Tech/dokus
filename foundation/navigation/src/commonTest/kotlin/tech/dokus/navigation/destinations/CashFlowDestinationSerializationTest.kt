package tech.dokus.navigation.destinations

import kotlinx.serialization.json.Json
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class CashFlowDestinationSerializationTest {

    private val json = Json

    @Test
    fun `document review with filter serializes and round-trips`() {
        val destination = CashFlowDestination.DocumentDetail(
            documentId = "doc-101",
            filter = DocumentListFilter.NeedsAttention.name,
        )

        val encoded = json.encodeToString(
            CashFlowDestination.DocumentDetail.serializer(),
            destination,
        )
        val decoded = json.decodeFromString(
            CashFlowDestination.DocumentDetail.serializer(),
            encoded,
        )

        assertEquals(destination, decoded)
        assertIs<CashFlowDestination.DocumentDetailQueueContext.DocumentList>(decoded.queueSource)
        assertEquals(
            DocumentListFilter.NeedsAttention,
            (decoded.queueSource as CashFlowDestination.DocumentDetailQueueContext.DocumentList).filter
        )
    }

    @Test
    fun `document review defaults to Recent queue source`() {
        val destination = CashFlowDestination.DocumentDetail(documentId = "doc-102")

        assertIs<CashFlowDestination.DocumentDetailQueueContext.Recent>(destination.queueSource)
    }

    @Test
    fun `document review with contactId reconstructs Contact source`() {
        val destination = CashFlowDestination.DocumentDetail(
            documentId = "doc-103",
            contactId = "550e8400-e29b-41d4-a716-446655440000",
        )

        val source = destination.queueSource
        assertIs<CashFlowDestination.DocumentDetailQueueContext.Contact>(source)
        assertEquals(
            ContactId.parse("550e8400-e29b-41d4-a716-446655440000"),
            source.contactId
        )
    }

    @Test
    fun `document review factory from DocumentList context`() {
        val docId = DocumentId.parse("550e8400-e29b-41d4-a716-446655440001")
        val context = CashFlowDestination.DocumentDetailQueueContext.DocumentList(DocumentListFilter.Confirmed)

        val destination = CashFlowDestination.DocumentDetail.from(docId, context)

        assertEquals(docId.value.toString(), destination.documentId)
        assertEquals("Confirmed", destination.filter)
        assertIs<CashFlowDestination.DocumentDetailQueueContext.DocumentList>(destination.queueSource)
    }

    @Test
    fun `document review factory from Contact context`() {
        val docId = DocumentId.parse("550e8400-e29b-41d4-a716-446655440002")
        val contactId = ContactId.parse("550e8400-e29b-41d4-a716-446655440003")
        val context = CashFlowDestination.DocumentDetailQueueContext.Contact(contactId)

        val destination = CashFlowDestination.DocumentDetail.from(docId, context)

        assertEquals(docId.value.toString(), destination.documentId)
        assertEquals(contactId.value.toString(), destination.contactId)
        assertIs<CashFlowDestination.DocumentDetailQueueContext.Contact>(destination.queueSource)
    }

    @Test
    fun `document review factory from Search context`() {
        val docId = DocumentId.parse("550e8400-e29b-41d4-a716-446655440004")
        val context = CashFlowDestination.DocumentDetailQueueContext.Search("invoice 2024")

        val destination = CashFlowDestination.DocumentDetail.from(docId, context)

        assertEquals("invoice 2024", destination.query)
        assertIs<CashFlowDestination.DocumentDetailQueueContext.Search>(destination.queueSource)
    }

    @Test
    fun `document source viewer serializes parameters`() {
        val destination = CashFlowDestination.DocumentSourceViewer(
            documentId = "doc-201",
            sourceId = "src-301",
        )

        val encoded = json.encodeToString(
            CashFlowDestination.DocumentSourceViewer.serializer(),
            destination,
        )
        val decoded = json.decodeFromString(
            CashFlowDestination.DocumentSourceViewer.serializer(),
            encoded,
        )

        assertEquals(destination, decoded)
    }
}
