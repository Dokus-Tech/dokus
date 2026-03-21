package tech.dokus.navigation.destinations

import kotlinx.serialization.json.Json
import tech.dokus.domain.enums.DocumentListFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CashFlowDestinationSerializationTest {

    private val json = Json

    @Test
    fun `document review destination serializes queue source`() {
        val destination = CashFlowDestination.DocumentReview(
            documentId = "doc-101",
            queueSource = CashFlowDestination.DocumentReviewQueueContext.DocumentList(
                filter = DocumentListFilter.NeedsAttention,
            ),
        )

        val encoded = json.encodeToString(
            CashFlowDestination.DocumentReview.serializer(),
            destination,
        )
        val decoded = json.decodeFromString(
            CashFlowDestination.DocumentReview.serializer(),
            encoded,
        )

        assertEquals(destination, decoded)
    }

    @Test
    fun `document review destination defaults to Recent queue source`() {
        val destination = CashFlowDestination.DocumentReview(documentId = "doc-102")

        assertIs<CashFlowDestination.DocumentReviewQueueContext.Recent>(destination.queueSource)
    }

    @Test
    fun `document review destination with contact source serializes`() {
        val destination = CashFlowDestination.DocumentReview(
            documentId = "doc-103",
            queueSource = CashFlowDestination.DocumentReviewQueueContext.Contact(
                contactId = "contact-1",
                contactName = "Acme Corp",
            ),
        )

        val encoded = json.encodeToString(
            CashFlowDestination.DocumentReview.serializer(),
            destination,
        )
        val decoded = json.decodeFromString(
            CashFlowDestination.DocumentReview.serializer(),
            encoded,
        )

        assertEquals(destination, decoded)
    }

    @Test
    fun `document source viewer destination serializes parameters`() {
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
