package tech.dokus.features.cashflow.presentation.documents.mvi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentCountsResponse
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import tech.dokus.foundation.app.state.isLoading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsContainerExternalRefreshTest {

    @Test
    fun `external documents change keeps existing rows visible while refreshing`() = runTest {
        val initialDocs = listOf(externalRefreshDocumentRecord("00000000-0000-0000-0000-000000000101"))
        val refreshedDocs = listOf(externalRefreshDocumentRecord("00000000-0000-0000-0000-000000000202"))

        val loadDocuments = ExternalRefreshLoadDocumentRecordsUseCase().apply {
            enqueuePageResult(filter = DocumentListFilter.All, result = externalRefreshPageResponse(initialDocs))
        }
        val getDocumentCounts = FakeGetDocumentCountsUseCase().apply {
            enqueueResult(DocumentCountsResponse(total = 11L, needsAttention = 4L, confirmed = 7L))
            enqueueResult(DocumentCountsResponse(total = 15L, needsAttention = 6L, confirmed = 9L))
        }
        val deferredRefresh = CompletableDeferred<Result<PaginatedResponse<DocumentListItemDto>>>()
        loadDocuments.enqueuePageDeferred(DocumentListFilter.All, deferredRefresh)

        val container = DocumentsContainer(loadDocuments, getDocumentCounts)

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val initial = states.value
            assertFalse(initial.documents.isLoading())
            assertEquals(4, initial.needsAttentionCount)
            assertEquals(7, initial.confirmedCount)
            assertEquals(initialDocs.map { it.documentId }, initial.documents.lastData?.data?.map { it.documentId })

            emit(DocumentsIntent.ExternalDocumentsChanged)
            runCurrent()

            val refreshing = states.value
            assertTrue(refreshing.documents.isLoading())
            assertEquals(initialDocs.map { it.documentId }, refreshing.documents.lastData?.data?.map { it.documentId })

            deferredRefresh.complete(Result.success(externalRefreshPageResponse(refreshedDocs)))
            advanceUntilIdle()

            val updated = states.value
            assertFalse(updated.documents.isLoading())
            assertEquals(6, updated.needsAttentionCount)
            assertEquals(9, updated.confirmedCount)
            assertEquals(refreshedDocs.map { it.documentId }, updated.documents.lastData?.data?.map { it.documentId })
            assertEquals(2, getDocumentCounts.callCount)
        }
    }
}

private class ExternalRefreshLoadDocumentRecordsUseCase : LoadDocumentRecordsUseCase {
    private val pageResults: MutableMap<DocumentListFilter, ArrayDeque<CompletableDeferred<Result<PaginatedResponse<DocumentListItemDto>>>>> =
        mutableMapOf()

    fun enqueuePageResult(
        filter: DocumentListFilter,
        result: PaginatedResponse<DocumentListItemDto>,
    ) {
        enqueuePageDeferred(
            filter = filter,
            deferred = CompletableDeferred<Result<PaginatedResponse<DocumentListItemDto>>>().apply {
                complete(Result.success(result))
            }
        )
    }

    fun enqueuePageDeferred(
        filter: DocumentListFilter,
        deferred: CompletableDeferred<Result<PaginatedResponse<DocumentListItemDto>>>,
    ) {
        pageResults.getOrPut(filter) { ArrayDeque() }.addLast(deferred)
    }

    override suspend fun invoke(
        page: Int,
        pageSize: Int,
        filter: DocumentListFilter?,
        documentStatus: DocumentStatus?,
        ingestionStatus: IngestionStatus?,
        sortBy: String?,
        contactId: ContactId?,
    ): Result<PaginatedResponse<DocumentListItemDto>> {
        val effectiveFilter = filter ?: DocumentListFilter.All
        val queue = requireNotNull(pageResults[effectiveFilter]) {
            "No paged responses queued for filter=$effectiveFilter"
        }
        return requireNotNull(queue.removeFirstOrNull()) {
            "No remaining paged responses for filter=$effectiveFilter"
        }.await()
    }
}

private fun externalRefreshPageResponse(items: List<DocumentListItemDto>): PaginatedResponse<DocumentListItemDto> {
    return PaginatedResponse(
        items = items,
        total = items.size.toLong(),
        limit = 20,
        offset = 0,
        hasMore = false
    )
}

private fun externalRefreshDocumentRecord(documentId: String): DocumentListItemDto {
    return DocumentListItemDto(
        documentId = DocumentId.parse(documentId),
        tenantId = TenantId.parse("00000000-0000-0000-0000-000000000001"),
        filename = "doc-$documentId.pdf",
        documentType = null,
        direction = null,
        documentStatus = null,
        ingestionStatus = null,
        effectiveOrigin = DocumentSource.Upload,
        uploadedAt = LocalDateTime(2026, 1, 1, 10, 0),
        counterpartyDisplayName = null,
        purposeRendered = null,
        totalAmount = null,
        currency = null,
        sortDate = LocalDate(2026, 1, 1),
    )
}
