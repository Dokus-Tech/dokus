package tech.dokus.features.cashflow.presentation.documents.route

import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsAction
import tech.dokus.navigation.destinations.CashFlowDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class DocumentsRouteNavigationMappingTest {

    @Test
    fun `documents action maps to document review route with filter`() {
        val queueSource = CashFlowDestination.DocumentDetailQueueContext.DocumentList(
            filter = DocumentListFilter.NeedsAttention,
        )
        val action = DocumentsAction.NavigateToDocumentDetail(
            documentId = DocumentId.parse("00000000-0000-0000-0000-000000000111"),
            queueSource = queueSource,
        )

        val destination = CashFlowDestination.DocumentDetail.from(
            action.documentId,
            action.queueSource,
        )

        assertEquals(action.documentId.toString(), destination.documentId)
        assertEquals(DocumentListFilter.NeedsAttention.name, destination.filter)
        val source = assertIs<CashFlowDestination.DocumentDetailQueueContext.DocumentList>(destination.queueSource)
        assertEquals(DocumentListFilter.NeedsAttention, source.filter)
    }
}
