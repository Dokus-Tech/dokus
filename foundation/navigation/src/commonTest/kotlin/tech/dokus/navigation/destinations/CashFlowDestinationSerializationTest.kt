package tech.dokus.navigation.destinations

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CashFlowDestinationSerializationTest {

    private val json = Json

    @Test
    fun `document review destination serializes route context`() {
        val destination = CashFlowDestination.DocumentReview(
            documentId = "doc-101",
            sourceFilter = CashFlowDestination.DocumentReviewSourceFilter.NeedsAttention.token,
            sourceSearch = "acme",
            sourceSort = CashFlowDestination.DocumentReviewSourceSort.NewestFirst.token,
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
    fun `document review destination keeps backward compatible defaults`() {
        val destination = CashFlowDestination.DocumentReview(documentId = "doc-102")

        assertNull(destination.sourceFilter)
        assertNull(destination.sourceSearch)
        assertEquals(
            CashFlowDestination.DocumentReviewSourceSort.NewestFirst.token,
            destination.sourceSort,
        )
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
