package tech.dokus.features.cashflow.presentation.review.route

import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.navigation.destinations.CashFlowDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DocumentReviewRouteMappingsTest {

    @Test
    fun `route with default source maps to Recent context`() {
        val route = CashFlowDestination.DocumentReview(documentId = "doc-1")
        val context = route.toQueueContext()

        assertIs<CashFlowDestination.DocumentReviewQueueSource.Recent>(context.source)
    }

    @Test
    fun `route with document list source preserves filter`() {
        val route = CashFlowDestination.DocumentReview(
            documentId = "doc-1",
            queueSource = CashFlowDestination.DocumentReviewQueueSource.DocumentList(
                filter = DocumentListFilter.NeedsAttention,
            ),
        )

        val context = route.toQueueContext()

        val source = assertIs<CashFlowDestination.DocumentReviewQueueSource.DocumentList>(context.source)
        assertEquals(DocumentListFilter.NeedsAttention, source.filter)
    }

    @Test
    fun `route with contact source preserves contact info`() {
        val route = CashFlowDestination.DocumentReview(
            documentId = "doc-1",
            queueSource = CashFlowDestination.DocumentReviewQueueSource.Contact(
                contactId = "contact-42",
                contactName = "Acme Corp",
            ),
        )

        val context = route.toQueueContext()

        val source = assertIs<CashFlowDestination.DocumentReviewQueueSource.Contact>(context.source)
        assertEquals("contact-42", source.contactId)
        assertEquals("Acme Corp", source.contactName)
    }
}
