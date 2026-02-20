package tech.dokus.features.cashflow.presentation.review.route

import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter
import tech.dokus.navigation.destinations.CashFlowDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DocumentReviewRouteMappingsTest {

    @Test
    fun `documents filter maps to review route filter`() {
        assertEquals(
            CashFlowDestination.DocumentReviewSourceFilter.All,
            DocumentFilter.All.toRouteFilter(),
        )
        assertEquals(
            CashFlowDestination.DocumentReviewSourceFilter.NeedsAttention,
            DocumentFilter.NeedsAttention.toRouteFilter(),
        )
        assertEquals(
            CashFlowDestination.DocumentReviewSourceFilter.Confirmed,
            DocumentFilter.Confirmed.toRouteFilter(),
        )
    }

    @Test
    fun `review route filter maps to list filter`() {
        assertEquals(DocumentListFilter.All, CashFlowDestination.DocumentReviewSourceFilter.All.toListFilter())
        assertEquals(
            DocumentListFilter.NeedsAttention,
            CashFlowDestination.DocumentReviewSourceFilter.NeedsAttention.toListFilter(),
        )
        assertEquals(
            DocumentListFilter.Confirmed,
            CashFlowDestination.DocumentReviewSourceFilter.Confirmed.toListFilter(),
        )
    }

    @Test
    fun `route context is absent when source filter is missing`() {
        val route = CashFlowDestination.DocumentReview(documentId = "doc-1")

        assertNull(route.toRouteContextOrNull())
    }

    @Test
    fun `route context normalizes blank search`() {
        val route = CashFlowDestination.DocumentReview(
            documentId = "doc-1",
            sourceFilter = CashFlowDestination.DocumentReviewSourceFilter.All.token,
            sourceSearch = "   ",
            sourceSort = CashFlowDestination.DocumentReviewSourceSort.NewestFirst.token,
        )

        val context = route.toRouteContextOrNull()

        requireNotNull(context)
        assertEquals(CashFlowDestination.DocumentReviewSourceFilter.All, context.filter)
        assertNull(context.search)
        assertEquals(CashFlowDestination.DocumentReviewSourceSort.NewestFirst, context.sort)
    }
}
