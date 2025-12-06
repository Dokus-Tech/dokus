package ai.dokus.app.cashflow.usecase

import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.exceptions.asDokusException
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

/**
 * Use case for watching pending documents with automatic refresh capability.
 *
 * Fetches documents with Pending or Processing status.
 * Exposes a Flow of [DokusState] containing all pending documents.
 * Pagination is handled by the consumer (ViewModel).
 */
class WatchPendingDocumentsUseCase(
    private val mediaRepository: MediaRepository
) {
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /**
     * Watch pending documents as a Flow.
     * Fetches documents with Pending or Processing status.
     *
     * @param limit Maximum number of documents to fetch (default 100, must be positive)
     * @return Flow of [DokusState] containing all pending documents
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    operator fun invoke(limit: Int = 100): Flow<DokusState<List<MediaDto>>> {
        require(limit > 0) { "Limit must be positive" }

        return refreshTrigger
            .onStart { emit(Unit) } // Initial load
            .flatMapLatest {
                flow {
                    emit(DokusState.loading())
                    try {
                        val documents = mediaRepository.list(
                            statuses = PENDING_STATUSES,
                            limit = limit
                        )
                        emit(DokusState.success(documents))
                    } catch (e: Exception) {
                        emit(DokusState.error(e.asDokusException) { refresh() })
                    }
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
         * Statuses considered as "pending" - documents awaiting user confirmation.
         */
        private val PENDING_STATUSES = listOf(MediaStatus.Pending, MediaStatus.Processing)
    }
}
