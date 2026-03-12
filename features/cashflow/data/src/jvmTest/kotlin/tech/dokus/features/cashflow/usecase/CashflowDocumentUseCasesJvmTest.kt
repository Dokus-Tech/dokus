package tech.dokus.features.cashflow.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.ObserveDocumentCollectionChangesUseCase
import tech.dokus.foundation.app.state.DokusState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class CashflowDocumentUseCasesJvmTest {

    @Test
    fun `watch pending documents refreshes when document collection changes`() = runTest {
        val remoteDataSource = mockk<CashflowRemoteDataSource>()
        val invalidations = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val firstPage = pageResponse(
            items = listOf(documentRecord("00000000-0000-0000-0000-000000000101"))
        )
        val secondPage = pageResponse(
            items = listOf(documentRecord("00000000-0000-0000-0000-000000000202"))
        )

        coEvery {
            remoteDataSource.listDocuments(
                filter = null,
                documentStatus = DocumentStatus.NeedsReview,
                documentType = null,
                ingestionStatus = null,
                page = 0,
                limit = 5,
            )
        } returnsMany listOf(
            Result.success(firstPage),
            Result.success(secondPage),
        )

        val observeChanges = object : ObserveDocumentCollectionChangesUseCase {
            override fun invoke() = invalidations
        }

        val statesDeferred = async {
            WatchPendingDocumentsUseCaseImpl(
                cashflowRemoteDataSource = remoteDataSource,
                observeDocumentCollectionChanges = observeChanges,
            ).invoke(limit = 5).take(4).toList()
        }

        advanceUntilIdle()
        invalidations.emit(Unit)
        advanceUntilIdle()

        val states = statesDeferred.await()

        assertTrue(states[0] is DokusState.Loading<*>)
        assertEquals(
            firstPage.items,
            (states[1] as DokusState.Success<List<DocumentListItemDto>>).data
        )
        assertTrue(states[2] is DokusState.Loading<*>)
        assertEquals(
            secondPage.items,
            (states[3] as DokusState.Success<List<DocumentListItemDto>>).data
        )
        coVerify(exactly = 2) {
            remoteDataSource.listDocuments(
                filter = null,
                documentStatus = DocumentStatus.NeedsReview,
                documentType = null,
                ingestionStatus = null,
                page = 0,
                limit = 5,
            )
        }
    }

    @Test
    fun `observe document collection changes coalesces bursts`() = runTest {
        val remoteDataSource = mockk<CashflowRemoteDataSource>()
        val upstream = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
        every { remoteDataSource.observeDocumentCollectionChanges() } returns upstream

        val emissionsDeferred = async {
            ObserveDocumentCollectionChangesUseCaseImpl(remoteDataSource)
                .invoke()
                .take(1)
                .toList()
        }

        runCurrent()
        upstream.emit(Unit)
        upstream.emit(Unit)
        upstream.emit(Unit)

        advanceTimeBy(249)
        runCurrent()
        assertTrue(!emissionsDeferred.isCompleted)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(1, emissionsDeferred.await().size)
    }
}

private fun pageResponse(items: List<DocumentListItemDto>): PaginatedResponse<DocumentListItemDto> {
    return PaginatedResponse(
        items = items,
        total = items.size.toLong(),
        limit = 20,
        offset = 0,
        hasMore = false,
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
