package tech.dokus.features.cashflow.presentation.review.route

import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.ids.ContactId
import tech.dokus.navigation.destinations.CashFlowDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class DocumentReviewRouteMappingsTest {

    @Test
    fun `route with no params maps to Recent context`() {
        val route = CashFlowDestination.DocumentReview(documentId = "doc-1")

        assertIs<CashFlowDestination.DocumentReviewQueueContext.Recent>(route.queueSource)
    }

    @Test
    fun `route with filter maps to DocumentList context`() {
        val route = CashFlowDestination.DocumentReview(
            documentId = "doc-1",
            filter = DocumentListFilter.NeedsAttention.name,
        )

        val source = assertIs<CashFlowDestination.DocumentReviewQueueContext.DocumentList>(route.queueSource)
        assertEquals(DocumentListFilter.NeedsAttention, source.filter)
    }

    @Test
    fun `route with contactId maps to Contact context`() {
        val contactId = ContactId.parse("550e8400-e29b-41d4-a716-446655440000")
        val route = CashFlowDestination.DocumentReview(
            documentId = "doc-1",
            contactId = contactId.value.toString(),
        )

        val source = assertIs<CashFlowDestination.DocumentReviewQueueContext.Contact>(route.queueSource)
        assertEquals(contactId, source.contactId)
    }

    @Test
    fun `route with query maps to Search context`() {
        val route = CashFlowDestination.DocumentReview(
            documentId = "doc-1",
            query = "invoice 2024",
        )

        val source = assertIs<CashFlowDestination.DocumentReviewQueueContext.Search>(route.queueSource)
        assertEquals("invoice 2024", source.query)
    }
}
