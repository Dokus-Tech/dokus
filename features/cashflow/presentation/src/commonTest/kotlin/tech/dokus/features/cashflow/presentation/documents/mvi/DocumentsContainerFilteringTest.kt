package tech.dokus.features.cashflow.presentation.documents.mvi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentCountsResponse
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsContainerFilteringTest {

    @Test
    fun `filter changes keep existing rows visible and avoid global loading`() = runTest {
        val allDocs = listOf(documentRecord("00000000-0000-0000-0000-000000000101"))
        val needsAttentionDocs = listOf(documentRecord("00000000-0000-0000-0000-000000000201"))

        val loadDocuments = FakeLoadDocumentRecordsUseCase().apply {
            enqueuePageResult(filter = DocumentListFilter.All, result = pageResponse(allDocs))
        }
        val getDocumentCounts = FakeGetDocumentCountsUseCase().apply {
            enqueueResult(DocumentCountsResponse(total = 64L, needsAttention = 39L, confirmed = 25L))
        }
        val deferredNeedsAttentionResult = CompletableDeferred<Result<PaginatedResponse<DocumentListItemDto>>>()
        loadDocuments.enqueuePageDeferred(DocumentListFilter.NeedsAttention, deferredNeedsAttentionResult)

        val container = DocumentsContainer(loadDocuments, getDocumentCounts)

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val initial = states.value
            assertEquals(DocumentFilter.All, initial.filter)
            assertFalse(initial.documents.isLoading())
            assertEquals(39, initial.needsAttentionCount)
            assertEquals(25, initial.confirmedCount)
            assertEquals(allDocs.map { it.documentId }, initial.documents.lastData?.data?.map { it.documentId })

            emit(DocumentsIntent.UpdateFilter(DocumentFilter.NeedsAttention))
            runCurrent()

            val refreshing = states.value
            assertEquals(DocumentFilter.NeedsAttention, refreshing.filter)
            assertTrue(refreshing.documents.isLoading())
            assertEquals(allDocs.map { it.documentId }, refreshing.documents.lastData?.data?.map { it.documentId })

            deferredNeedsAttentionResult.complete(Result.success(pageResponse(needsAttentionDocs)))
            advanceUntilIdle()

            val updated = states.value
            assertFalse(updated.documents.isLoading())
            assertEquals(DocumentFilter.NeedsAttention, updated.filter)
            assertEquals(
                needsAttentionDocs.map { it.documentId },
                updated.documents.lastData?.data?.map { it.documentId }
            )
            assertEquals(39, updated.needsAttentionCount)
            assertEquals(25, updated.confirmedCount)
            assertEquals(1, getDocumentCounts.callCount)
        }
    }

    @Test
    fun `filter error preserves data and keeps new filter`() = runTest {
        val allDocs = listOf(documentRecord("00000000-0000-0000-0000-000000000101"))

        val loadDocuments = FakeLoadDocumentRecordsUseCase().apply {
            enqueuePageResult(filter = DocumentListFilter.All, result = pageResponse(allDocs))
        }
        val getDocumentCounts = FakeGetDocumentCountsUseCase().apply {
            enqueueResult(DocumentCountsResponse(total = 15L, needsAttention = 10L, confirmed = 5L))
        }
        val deferredFailure = CompletableDeferred<Result<PaginatedResponse<DocumentListItemDto>>>()
        loadDocuments.enqueuePageDeferred(DocumentListFilter.NeedsAttention, deferredFailure)

        val container = DocumentsContainer(loadDocuments, getDocumentCounts)

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val initial = states.value
            assertEquals(DocumentFilter.All, initial.filter)
            assertEquals(10, initial.needsAttentionCount)
            assertEquals(5, initial.confirmedCount)
            assertEquals(allDocs.map { it.documentId }, initial.documents.lastData?.data?.map { it.documentId })

            emit(DocumentsIntent.UpdateFilter(DocumentFilter.NeedsAttention))
            runCurrent()

            val refreshing = states.value
            assertTrue(refreshing.documents.isLoading())

            deferredFailure.complete(Result.failure(RuntimeException("network error")))
            advanceUntilIdle()

            val errorState = states.value
            assertTrue(errorState.documents.isError())
            assertEquals(DocumentFilter.NeedsAttention, errorState.filter)
            assertEquals(allDocs.map { it.documentId }, errorState.documents.lastData?.data?.map { it.documentId })
            assertEquals(1, getDocumentCounts.callCount)
        }
    }
}

private class FakeLoadDocumentRecordsUseCase : LoadDocumentRecordsUseCase {
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

private fun pageResponse(items: List<DocumentListItemDto>): PaginatedResponse<DocumentListItemDto> {
    return PaginatedResponse(
        items = items,
        total = items.size.toLong(),
        limit = 20,
        offset = 0,
        hasMore = false
    )
}

private fun documentRecord(documentId: String): DocumentListItemDto {
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
    )
}
