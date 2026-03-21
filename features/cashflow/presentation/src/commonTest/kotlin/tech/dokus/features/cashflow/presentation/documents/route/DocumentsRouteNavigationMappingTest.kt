package tech.dokus.features.cashflow.presentation.documents.route

import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsAction
import tech.dokus.navigation.destinations.CashFlowDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DocumentsRouteNavigationMappingTest {

    @Test
    fun `documents action maps to document review route with queue source`() {
        val queueSource = CashFlowDestination.DocumentReviewQueueSource.DocumentList(
            filter = DocumentListFilter.NeedsAttention,
        )
        val action = DocumentsAction.NavigateToDocumentReview(
            documentId = DocumentId.parse("00000000-0000-0000-0000-000000000111"),
            queueSource = queueSource,
        )

        val destination = toDocumentReviewDestination(action)

        assertEquals(action.documentId.toString(), destination.documentId)
        val source = assertIs<CashFlowDestination.DocumentReviewQueueSource.DocumentList>(destination.queueSource)
        assertEquals(DocumentListFilter.NeedsAttention, source.filter)
    }
}
