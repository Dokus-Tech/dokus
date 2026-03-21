package tech.dokus.features.cashflow.presentation.documents.mvi

import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.init
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.usecases.GetDocumentCountsUseCase
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.platform.Logger

internal typealias DocumentsCtx = PipelineContext<DocumentsState, DocumentsIntent, DocumentsAction>

/**
 * Container for the Documents screen using FlowMVI.
 *
 * Manages the document inbox with:
 * - Pagination
 * - Status filtering
 */
internal class DocumentsContainer(
    private val loadDocumentRecords: LoadDocumentRecordsUseCase,
    private val getDocumentCounts: GetDocumentCountsUseCase,
) : Container<DocumentsState, DocumentsIntent, DocumentsAction> {

    private val logger = Logger.forClass<DocumentsContainer>()

    override val store: Store<DocumentsState, DocumentsIntent, DocumentsAction> =
        store(DocumentsState.initial) {
            init { handleRefresh() }

            reduce { intent ->
                when (intent) {
                    is DocumentsIntent.Refresh -> handleRefresh()
                    is DocumentsIntent.ExternalDocumentsChanged -> handleRefresh()
                    is DocumentsIntent.LoadMore -> handleLoadMore()
                    is DocumentsIntent.UpdateFilter -> handleUpdateFilter(intent.filter)
                    is DocumentsIntent.UpdateSort -> handleUpdateSort(intent.sort)
                    is DocumentsIntent.OpenDocument -> handleOpenDocument(intent.documentId)
                }
            }
        }

    private suspend fun DocumentsCtx.handleRefresh() {
        withState {
            logger.d { "Refreshing documents" }
            updateState { copy(documents = documents.asLoading) }

            launch { loadCounts() }
            loadDocumentRecords(
                page = 0,
                pageSize = PAGE_SIZE,
                filter = filter.toListFilter(),
                sortBy = sortField.apiValue,
            ).fold(
                onSuccess = { response ->
                    val documents = DokusState.success(
                        PaginationState(
                            data = response.items,
                            hasMorePages = response.hasMore,
                            currentPage = 0,
                            pageSize = PAGE_SIZE,
                        )
                    )
                    updateState {
                        copy(documents = documents)
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load documents" }
                    val dokusError = error.asDokusException
                    updateState {
                        copy(
                            documents = DokusState.error(
                                exception = dokusError,
                                retryHandler = { intent(DocumentsIntent.Refresh) },
                                lastData = documents.lastData
                            )
                        )
                    }
                }
            )
        }
    }

    private suspend fun DocumentsCtx.handleLoadMore() {
        withState {
            // Only success state can load data. Otherwise, it's error or already loading
            val paginationState =
                documents.let { if (it.isSuccess()) it.data else return@withState }

            updateState { copy(documents = documents.asLoading) }

            val nextPage = paginationState.currentPage + 1
            loadDocumentRecords(
                page = nextPage,
                pageSize = PAGE_SIZE,
                filter = filter.toListFilter(),
                sortBy = sortField.apiValue,
            ).fold(
                onSuccess = { response ->
                    val documents = DokusState.success(
                        PaginationState(
                            data = paginationState.data + response.items,
                            hasMorePages = response.hasMore,
                            currentPage = nextPage,
                            pageSize = PAGE_SIZE,
                        )
                    )
                    updateState { copy(documents = documents) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load more documents" }
                    val dokusError = error.asDokusException
                    updateState {
                        copy(
                            documents = DokusState.error(
                                exception = dokusError,
                                retryHandler = { intent(DocumentsIntent.Refresh) },
                                lastData = documents.lastData
                            )
                        )
                    }
                }
            )
        }
    }

    private suspend fun DocumentsCtx.handleUpdateFilter(filter: DocumentFilter) {
        withState {
            val currentSortBy = sortField.apiValue
            updateState {
                copy(documents = documents.asLoading, filter = filter)
            }

            loadDocumentRecords(
                page = 0,
                pageSize = PAGE_SIZE,
                filter = filter.toListFilter(),
                sortBy = currentSortBy,
            ).fold(
                onSuccess = { response ->
                    val documents = DokusState.success(
                        PaginationState(
                            data = response.items,
                            hasMorePages = response.hasMore,
                            currentPage = 0,
                            pageSize = PAGE_SIZE,
                        )
                    )
                    updateState {
                        copy(documents = documents)
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load documents" }
                    val dokusError = error.asDokusException
                    updateState {
                        copy(
                            documents = DokusState.error(
                                exception = dokusError,
                                retryHandler = { intent(DocumentsIntent.Refresh) },
                                lastData = documents.lastData
                            )
                        )
                    }
                }
            )
        }
    }

    private suspend fun DocumentsCtx.handleUpdateSort(sort: DocumentSortField) {
        withState {
            val currentFilter = filter
            updateState {
                copy(documents = documents.asLoading, sortField = sort)
            }

            loadDocumentRecords(
                page = 0,
                pageSize = PAGE_SIZE,
                filter = currentFilter.toListFilter(),
                sortBy = sort.apiValue,
            ).fold(
                onSuccess = { response ->
                    val documents = DokusState.success(
                        PaginationState(
                            data = response.items,
                            hasMorePages = response.hasMore,
                            currentPage = 0,
                            pageSize = PAGE_SIZE,
                        )
                    )
                    updateState { copy(documents = documents) }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load documents" }
                    val dokusError = error.asDokusException
                    updateState {
                        copy(
                            documents = DokusState.error(
                                exception = dokusError,
                                retryHandler = { intent(DocumentsIntent.Refresh) },
                                lastData = documents.lastData
                            )
                        )
                    }
                }
            )
        }
    }

    private suspend fun DocumentsCtx.handleOpenDocument(documentId: DocumentId) {
        withState {
            val queueSource = CashFlowDestination.DocumentReviewQueueSource.DocumentList(
                filter = filter.toListFilter(),
            )
            action(DocumentsAction.NavigateToDocumentReview(documentId, queueSource))
        }
    }

    private fun DocumentFilter.toListFilter(): DocumentListFilter = when (this) {
        DocumentFilter.All -> DocumentListFilter.All
        DocumentFilter.NeedsAttention -> DocumentListFilter.NeedsAttention
        DocumentFilter.Confirmed -> DocumentListFilter.Confirmed
    }

    private suspend fun DocumentsCtx.loadCounts() {
        getDocumentCounts()
            .onSuccess { counts ->
                updateState {
                    copy(
                        totalCount = counts.total.toUiCount(),
                        needsAttentionCount = counts.needsAttention.toUiCount(),
                        confirmedCount = counts.confirmed.toUiCount()
                    )
                }
            }
            .onFailure { error ->
                logger.e(error) { "Failed to load document counts" }
            }
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}

private fun Long.toUiCount(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
