package ai.dokus.app.cashflow.usecase

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.domain.enums.ProcessingStatus
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.DocumentProcessingDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

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
     * Returns documents in all pre-confirmation statuses.
     *
     * @param limit Maximum number of documents to fetch (default 100, max 100)
     * @return Flow of [DokusState] containing pending documents
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(limit: Int = 100): Flow<DokusState<List<DocumentProcessingDto>>> {
        require(limit > 0) { "Limit must be positive" }

        return refreshTrigger
            .onStart { emit(Unit) } // Initial load
            .flatMapLatest {
                flow {
                    emit(DokusState.loading())
                    dataSource.listDocumentProcessing(
                        statuses = PENDING_STATUSES,
                        page = 0,
                        limit = limit.coerceAtMost(100)
                    ).fold(
                        onSuccess = { response ->
                            emit(DokusState.success(response.items))
                        },
                        onFailure = { e ->
                            emit(DokusState.error(e.asDokusException) { refresh() })
                        }
                    )
                }
            }
    }

    /**
     * Trigger a refresh of the pending documents.
     */
    fun refresh() {
        refreshTrigger.tryEmit(Unit)
    }

    companion object {
        /**
         * All statuses before user confirmation.
         * Shows the full document processing pipeline.
         */
        private val PENDING_STATUSES = listOf(
            ProcessingStatus.Pending,    // Uploaded, waiting for AI
            ProcessingStatus.Queued,     // Picked up, in queue
            ProcessingStatus.Processing, // AI working
            ProcessingStatus.Processed   // Ready for user review
        )
    }
}
