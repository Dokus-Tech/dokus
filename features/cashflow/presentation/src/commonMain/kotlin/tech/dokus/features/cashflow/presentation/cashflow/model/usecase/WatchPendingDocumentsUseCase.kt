package tech.dokus.features.cashflow.presentation.cashflow.model.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.foundation.app.state.DokusState

private const val DEFAULT_DOCUMENT_LIMIT = 100

/**
 * Use case for watching pending documents with automatic refresh capability.
 *
 * Returns documents that are in the AI processing pipeline - from upload
 * through extraction to ready for user confirmation.
 *
 * Exposes a Flow of [DokusState] containing all pending documents.
 * Pagination is handled by the consumer (ViewModel).
 */
class WatchPendingDocumentsUseCase(
    private val dataSource: CashflowRemoteDataSource
) {
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Watch pending documents as a Flow.
     * Returns documents with drafts that need review.
     *
     * @param limit Maximum number of documents to fetch (default 100, max 100)
     * @return Flow of [DokusState] containing pending documents
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(limit: Int = DEFAULT_DOCUMENT_LIMIT): Flow<DokusState<List<DocumentRecordDto>>> {
        require(limit > 0) { "Limit must be positive" }

        return refreshTrigger
            .onStart { emit(Unit) } // Initial load
            .flatMapLatest {
                flow {
                    emit(DokusState.loading())
                    val statuses = listOf(
                        DraftStatus.NeedsReview,
                        DraftStatus.Ready,
                        DraftStatus.NeedsInput
                    )
                    val collected = mutableListOf<DocumentRecordDto>()
                    for (status in statuses) {
                        val result = dataSource.listDocuments(
                            draftStatus = status,
                            page = 0,
                            limit = limit.coerceAtMost(DEFAULT_DOCUMENT_LIMIT)
                        )
                        val failed = result.exceptionOrNull()
                        if (failed != null) {
                            emit(DokusState.error(failed.asDokusException) { refresh() })
                            return@flow
                        }
                        collected += result.getOrThrow().items
                    }

                    val unique = collected.distinctBy { it.document.id }
                    emit(DokusState.success(unique))
                }
            }
    }

    /**
     * Trigger a refresh of the pending documents.
     */
    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }
}
