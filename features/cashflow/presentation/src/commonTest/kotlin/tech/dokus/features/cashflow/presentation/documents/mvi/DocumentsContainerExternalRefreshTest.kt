package tech.dokus.features.cashflow.presentation.documents.mvi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentCountsResponse
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import tech.dokus.foundation.app.state.isLoading
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
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
            enqueueResult(DocumentCountsResponse(needsAttention = 4L, confirmed = 7L))
            enqueueResult(DocumentCountsResponse(needsAttention = 6L, confirmed = 9L))
        }
        val deferredRefresh = CompletableDeferred<Result<PaginatedResponse<DocumentRecordDto>>>()
        loadDocuments.enqueuePageDeferred(DocumentListFilter.All, deferredRefresh)

        val container = DocumentsContainer(loadDocuments, getDocumentCounts)

        container.store.subscribeAndTest {
            advanceUntilIdle()

            val initial = states.value
            assertFalse(initial.documents.isLoading())
            assertEquals(4, initial.needsAttentionCount)
            assertEquals(7, initial.confirmedCount)
            assertEquals(initialDocs.map { it.document.id }, initial.documents.lastData?.data?.map { it.document.id })

            emit(DocumentsIntent.ExternalDocumentsChanged)
            runCurrent()

            val refreshing = states.value
            assertTrue(refreshing.documents.isLoading())
            assertEquals(initialDocs.map { it.document.id }, refreshing.documents.lastData?.data?.map { it.document.id })

            deferredRefresh.complete(Result.success(externalRefreshPageResponse(refreshedDocs)))
            advanceUntilIdle()

            val updated = states.value
            assertFalse(updated.documents.isLoading())
            assertEquals(6, updated.needsAttentionCount)
            assertEquals(9, updated.confirmedCount)
            assertEquals(refreshedDocs.map { it.document.id }, updated.documents.lastData?.data?.map { it.document.id })
            assertEquals(2, getDocumentCounts.callCount)
        }
    }
}

private class ExternalRefreshLoadDocumentRecordsUseCase : LoadDocumentRecordsUseCase {
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
        documentStatus: DocumentStatus?,
        ingestionStatus: IngestionStatus?,
    ): Result<PaginatedResponse<DocumentRecordDto>> {
        val effectiveFilter = filter ?: DocumentListFilter.All
        val queue = requireNotNull(pageResults[effectiveFilter]) {
            "No paged responses queued for filter=$effectiveFilter"
        }
        return requireNotNull(queue.removeFirstOrNull()) {
            "No remaining paged responses for filter=$effectiveFilter"
        }.await()
    }
}

private fun externalRefreshPageResponse(items: List<DocumentRecordDto>): PaginatedResponse<DocumentRecordDto> {
    return PaginatedResponse(
        items = items,
        total = items.size.toLong(),
        limit = 20,
        offset = 0,
        hasMore = false
    )
}

private fun externalRefreshDocumentRecord(documentId: String): DocumentRecordDto {
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
