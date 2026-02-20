package tech.dokus.features.cashflow.presentation.documents.route

import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsAction
import tech.dokus.navigation.destinations.CashFlowDestination
import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentsRouteNavigationMappingTest {

    @Test
    fun `documents action maps to document review route with context`() {
        val action = DocumentsAction.NavigateToDocumentReview(
            documentId = DocumentId.parse("00000000-0000-0000-0000-000000000111"),
            sourceFilter = DocumentFilter.NeedsAttention,
            sourceSearch = "acme",
            sourceSort = CashFlowDestination.DocumentReviewSourceSort.NewestFirst,
        )

        val destination = toDocumentReviewDestination(action)

        assertEquals(action.documentId.toString(), destination.documentId)
        assertEquals(
            CashFlowDestination.DocumentReviewSourceFilter.NeedsAttention.token,
            destination.sourceFilter,
        )
        assertEquals("acme", destination.sourceSearch)
        assertEquals(CashFlowDestination.DocumentReviewSourceSort.NewestFirst.token, destination.sourceSort)
    }

    @Test
    fun `documents action preserves null search context`() {
        val action = DocumentsAction.NavigateToDocumentReview(
            documentId = DocumentId.parse("00000000-0000-0000-0000-000000000112"),
            sourceFilter = DocumentFilter.All,
            sourceSearch = null,
        )

        val destination = toDocumentReviewDestination(action)

        assertEquals(CashFlowDestination.DocumentReviewSourceFilter.All.token, destination.sourceFilter)
        assertEquals(null, destination.sourceSearch)
    }
}
