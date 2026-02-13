package tech.dokus.app.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.usecases.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal typealias TodayCtx = PipelineContext<TodayState, TodayIntent, TodayAction>

/**
 * Container for Today screen using FlowMVI.
 *
 * Manages the today data including:
 * - Pending documents for processing
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class TodayContainer(
    private val watchPendingDocuments: WatchPendingDocumentsUseCase,
) : Container<TodayState, TodayIntent, TodayAction> {

    private val logger = Logger.forClass<TodayContainer>()

    // Internal state for pending documents pagination
    private val allPendingDocuments = MutableStateFlow<List<DocumentRecordDto>>(emptyList())
    private val pendingVisibleCount = MutableStateFlow(TodayState.PENDING_PAGE_SIZE)

    override val store: Store<TodayState, TodayIntent, TodayAction> =
        store(TodayState.Content()) {
            init {
                // Start watching pending documents on init
                launchWatchPendingDocuments()
            }

            reduce { intent ->
                when (intent) {
                    is TodayIntent.RefreshPendingDocuments -> handleRefreshPendingDocuments()
                    is TodayIntent.LoadMorePendingDocuments -> handleLoadMorePendingDocuments()
                }
            }
        }

    private suspend fun TodayCtx.launchWatchPendingDocuments() {
        launch {
            watchPendingDocuments().collectLatest { state ->
                when (state) {
                    is DokusState.Loading -> {
                        withState<TodayState.Content, _> {
                            updateState { copy(pendingDocumentsState = DokusState.loading()) }
                        }
                    }

                    is DokusState.Success -> {
                        allPendingDocuments.value = state.data
                        pendingVisibleCount.value = TodayState.PENDING_PAGE_SIZE
                        updatePendingPaginationState()
                    }

                    is DokusState.Error -> {
                        allPendingDocuments.value = emptyList()
                        withState<TodayState.Content, _> {
                            updateState {
                                copy(
                                    pendingDocumentsState = DokusState.error(
                                        state.exception,
                                        state.retryHandler
                                    )
                                )
                            }
                        }
                    }

                    is DokusState.Idle -> {
                        withState<TodayState.Content, _> {
                            updateState { copy(pendingDocumentsState = DokusState.idle()) }
                        }
                    }
                }
            }
        }
    }

    private fun handleRefreshPendingDocuments() {
        logger.d { "Refreshing pending documents" }
        watchPendingDocuments.refresh()
    }

    private suspend fun TodayCtx.handleLoadMorePendingDocuments() {
        val allDocs = allPendingDocuments.value
        val currentVisible = pendingVisibleCount.value

        // Don't load more if we're already showing all items
        if (currentVisible >= allDocs.size) return

        logger.d { "Loading more pending documents" }

        // Increase visible count
        pendingVisibleCount.value = (currentVisible + TodayState.PENDING_PAGE_SIZE)
            .coerceAtMost(allDocs.size)

        updatePendingPaginationState()
    }

    private suspend fun TodayCtx.updatePendingPaginationState() {
        val allDocs = allPendingDocuments.value
        val visibleCount = pendingVisibleCount.value
        val visibleDocs = allDocs.take(visibleCount)
        val hasMore = visibleCount < allDocs.size

        val paginationState = PaginationState(
            data = visibleDocs,
            currentPage = visibleCount / TodayState.PENDING_PAGE_SIZE,
            pageSize = TodayState.PENDING_PAGE_SIZE,
            hasMorePages = hasMore,
            isLoadingMore = false
        )

        withState<TodayState.Content, _> {
            updateState {
                copy(pendingDocumentsState = DokusState.success(paginationState))
            }
        }
    }
}
