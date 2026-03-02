package tech.dokus.features.cashflow.presentation.documents.mvi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsContainerFilteringTest {

    @Test
    fun `filter changes keep existing rows visible and avoid global loading`() = runTest {
        val allDocs = listOf(documentRecord("00000000-0000-0000-0000-000000000101"))
        val needsAttentionDocs = listOf(documentRecord("00000000-0000-0000-0000-000000000201"))

        val loadDocuments = FakeLoadDocumentRecordsUseCase().apply {
            countTotals[DocumentListFilter.NeedsAttention] = 39L
            countTotals[DocumentListFilter.Confirmed] = 25L
            enqueuePageResult(
                filter = DocumentListFilter.All,
                result = pageResponse(allDocs)
            )
        }
        val deferredNeedsAttentionResult = CompletableDeferred<Result<PaginatedResponse<DocumentRecordDto>>>()
        loadDocuments.enqueuePageDeferred(DocumentListFilter.NeedsAttention, deferredNeedsAttentionResult)

        val container = DocumentsContainer(loadDocuments)

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val initial = assertIs<DocumentsState.Content>(states.value)
            assertEquals(DocumentFilter.All, initial.filter)
            assertFalse(initial.isRefreshing)
            assertEquals(allDocs.map { it.document.id }, initial.documents.data.map { it.document.id })

            emit(DocumentsIntent.UpdateFilter(DocumentFilter.NeedsAttention))
            runCurrent()

            val refreshing = assertIs<DocumentsState.Content>(states.value)
            assertEquals(DocumentFilter.NeedsAttention, refreshing.filter)
            assertTrue(refreshing.isRefreshing)
            assertEquals(allDocs.map { it.document.id }, refreshing.documents.data.map { it.document.id })

            deferredNeedsAttentionResult.complete(Result.success(pageResponse(needsAttentionDocs)))
            advanceUntilIdle()

            val updated = assertIs<DocumentsState.Content>(states.value)
            assertFalse(updated.isRefreshing)
            assertEquals(DocumentFilter.NeedsAttention, updated.filter)
            assertEquals(
                needsAttentionDocs.map { it.document.id },
                updated.documents.data.map { it.document.id }
            )
            assertEquals(39, updated.needsAttentionCount)
            assertEquals(25, updated.confirmedCount)
        }
    }
}

private class FakeLoadDocumentRecordsUseCase : LoadDocumentRecordsUseCase {
    val countTotals: MutableMap<DocumentListFilter, Long> = mutableMapOf()
    private val pageResults: MutableMap<DocumentListFilter, ArrayDeque<CompletableDeferred<Result<PaginatedResponse<DocumentRecordDto>>>>> =
        mutableMapOf()

    fun enqueuePageResult(
        filter: DocumentListFilter,
        result: PaginatedResponse<DocumentRecordDto>,
    ) {
        enqueuePageDeferred(
            filter = filter,
            deferred = CompletableDeferred<Result<PaginatedResponse<DocumentRecordDto>>>().apply {
                complete(Result.success(result))
            }
        )
    }

    fun enqueuePageDeferred(
        filter: DocumentListFilter,
        deferred: CompletableDeferred<Result<PaginatedResponse<DocumentRecordDto>>>,
    ) {
        pageResults.getOrPut(filter) { ArrayDeque() }.addLast(deferred)
    }

    override suspend fun invoke(
        page: Int,
        pageSize: Int,
        filter: DocumentListFilter?,
        documentStatus: tech.dokus.domain.enums.DocumentStatus?,
        ingestionStatus: tech.dokus.domain.enums.IngestionStatus?,
    ): Result<PaginatedResponse<DocumentRecordDto>> {
        val effectiveFilter = filter ?: DocumentListFilter.All
        if (pageSize == 1) {
            return Result.success(
                PaginatedResponse(
                    items = emptyList(),
                    total = countTotals[effectiveFilter] ?: 0L,
                    limit = pageSize,
                    offset = page,
                    hasMore = false
                )
            )
        }

        val queue = requireNotNull(pageResults[effectiveFilter]) {
            "No paged responses queued for filter=$effectiveFilter"
        }
        return requireNotNull(queue.removeFirstOrNull()) {
            "No remaining paged responses for filter=$effectiveFilter"
        }.await()
    }
}

private fun pageResponse(items: List<DocumentRecordDto>): PaginatedResponse<DocumentRecordDto> {
    return PaginatedResponse(
        items = items,
        total = items.size.toLong(),
        limit = 20,
        offset = 0,
        hasMore = false
    )
}

private fun documentRecord(documentId: String): DocumentRecordDto {
    return DocumentRecordDto(
        document = DocumentDto(
            id = DocumentId.parse(documentId),
            tenantId = TenantId.parse("00000000-0000-0000-0000-000000000001"),
            filename = "doc-$documentId.pdf",
            contentType = "application/pdf",
            sizeBytes = 1024,
            storageKey = "documents/$documentId.pdf",
            uploadedAt = LocalDateTime(2026, 1, 1, 10, 0),
            downloadUrl = null,
        ),
        draft = null,
        latestIngestion = null,
        confirmedEntity = null,
        cashflowEntryId = null,
        pendingMatchReview = null,
        sources = emptyList(),
    )
}
