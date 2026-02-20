@file:Suppress("TooManyFunctions")

package tech.dokus.features.cashflow.presentation.documents.mvi

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.DocumentListFilter
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.usecases.LoadDocumentRecordsUseCase
import tech.dokus.foundation.platform.Logger

internal typealias DocumentsCtx = PipelineContext<DocumentsState, DocumentsIntent, DocumentsAction>

/**
 * Container for the Documents screen using FlowMVI.
 *
 * Manages the document inbox with:
 * - Pagination
 * - Status filtering
 * - Search
 */
internal class DocumentsContainer(
    private val loadDocumentRecords: LoadDocumentRecordsUseCase
) : Container<DocumentsState, DocumentsIntent, DocumentsAction> {

    private val logger = Logger.forClass<DocumentsContainer>()

    private var searchJob: Job? = null

    private var loadedDocuments: List<DocumentRecordDto> = emptyList()
    private var paginationInfo = PaginationInfo()
    private var currentFilter: DocumentFilter = DocumentFilter.All
    private var currentSearchQuery: String = ""
    private var needsAttentionCount: Int = 0

    override val store: Store<DocumentsState, DocumentsIntent, DocumentsAction> =
        store(DocumentsState.Loading) {
            reduce { intent ->
                when (intent) {
                    is DocumentsIntent.Refresh -> handleRefresh()
                    is DocumentsIntent.LoadMore -> handleLoadMore()
                    is DocumentsIntent.UpdateSearchQuery -> handleUpdateSearchQuery(intent.query)
                    is DocumentsIntent.UpdateFilter -> handleUpdateFilter(intent.filter)
                    is DocumentsIntent.OpenDocument -> handleOpenDocument(intent.documentId)
                }
            }
        }

    private suspend fun DocumentsCtx.handleRefresh() {
        searchJob?.cancel()
        logger.d { "Refreshing documents" }

        loadedDocuments = emptyList()
        paginationInfo = PaginationInfo()

        updateState { DocumentsState.Loading }

        needsAttentionCount = loadNeedsAttentionCount()
        loadDocumentRecords(
            page = 0,
            pageSize = PAGE_SIZE,
            filter = currentFilter.toListFilter(),
            search = currentSearchQuery.takeIf { it.isNotEmpty() }
        ).fold(
            onSuccess = { response ->
                loadedDocuments = response.items
                paginationInfo = paginationInfo.copy(
                    currentPage = 0,
                    isLoadingMore = false,
                    hasMorePages = response.hasMore
                )
                updateState {
                    DocumentsState.Content(
                        documents = buildPaginationState(),
                        filter = currentFilter,
                        searchQuery = currentSearchQuery,
                        needsAttentionCount = this@DocumentsContainer.needsAttentionCount
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load documents" }
                updateState {
                    DocumentsState.Error(
                        exception = error.asDokusException,
                        retryHandler = { intent(DocumentsIntent.Refresh) }
                    )
                }
            }
        )
    }

    private suspend fun DocumentsCtx.handleLoadMore() {
        withState<DocumentsState.Content, _> {
            if (paginationInfo.isLoadingMore || !paginationInfo.hasMorePages) return@withState

            paginationInfo = paginationInfo.copy(isLoadingMore = true)
            updateState { copy(documents = buildPaginationState()) }

            val nextPage = paginationInfo.currentPage + 1
            loadDocumentRecords(
                page = nextPage,
                pageSize = PAGE_SIZE,
                filter = filter.toListFilter(),
                search = searchQuery.takeIf { it.isNotEmpty() }
            ).fold(
                onSuccess = { response ->
                    loadedDocuments = loadedDocuments + response.items
                    paginationInfo = paginationInfo.copy(
                        currentPage = nextPage,
                        isLoadingMore = false,
                        hasMorePages = response.hasMore
                    )
                    updateState {
                        copy(
                            documents = buildPaginationState(),
                            needsAttentionCount = this@DocumentsContainer.needsAttentionCount
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load more documents" }
                    paginationInfo = paginationInfo.copy(
                        isLoadingMore = false,
                        hasMorePages = false
                    )
                    updateState { copy(documents = buildPaginationState()) }
                }
            )
        }
    }

    private suspend fun DocumentsCtx.handleUpdateSearchQuery(query: String) {
        val trimmed = query.trim()
        currentSearchQuery = trimmed

        withState<DocumentsState.Content, _> {
            searchJob?.cancel()

            loadedDocuments = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    searchQuery = trimmed,
                    documents = PaginationState(pageSize = PAGE_SIZE)
                )
            }

            searchJob = launch {
                loadDocumentRecords(
                    page = 0,
                    pageSize = PAGE_SIZE,
                    filter = currentFilter.toListFilter(),
                    search = trimmed.takeIf { it.isNotEmpty() }
                ).fold(
                    onSuccess = { response ->
                        loadedDocuments = response.items
                        paginationInfo = paginationInfo.copy(
                            currentPage = 0,
                            isLoadingMore = false,
                            hasMorePages = response.hasMore
                        )
                        updateState {
                            copy(
                                documents = buildPaginationState(),
                                searchQuery = trimmed,
                                filter = currentFilter,
                                needsAttentionCount = this@DocumentsContainer.needsAttentionCount
                            )
                        }
                    },
                    onFailure = { error ->
                        logger.e(error) { "Failed to search documents" }
                    }
                )
            }
        }
    }

    private suspend fun DocumentsCtx.handleUpdateFilter(filter: DocumentFilter) {
        currentFilter = filter

        withState<DocumentsState.Content, _> {
            searchJob?.cancel()

            loadedDocuments = emptyList()
            paginationInfo = PaginationInfo()

            updateState {
                copy(
                    filter = filter,
                    documents = PaginationState(pageSize = PAGE_SIZE)
                )
            }

            this@DocumentsContainer.needsAttentionCount = loadNeedsAttentionCount()
            loadDocumentRecords(
                page = 0,
                pageSize = PAGE_SIZE,
                filter = filter.toListFilter(),
                search = currentSearchQuery.takeIf { it.isNotEmpty() }
            ).fold(
                onSuccess = { response ->
                    loadedDocuments = response.items
                    paginationInfo = paginationInfo.copy(
                        currentPage = 0,
                        isLoadingMore = false,
                        hasMorePages = response.hasMore
                    )
                    updateState {
                        copy(
                            documents = buildPaginationState(),
                            searchQuery = currentSearchQuery,
                            filter = filter,
                            needsAttentionCount = this@DocumentsContainer.needsAttentionCount
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to filter documents" }
                }
            )
        }
    }

    private suspend fun DocumentsCtx.handleOpenDocument(documentId: tech.dokus.domain.ids.DocumentId) {
        action(
            DocumentsAction.NavigateToDocumentReview(
                documentId = documentId,
                sourceFilter = currentFilter,
                sourceSearch = currentSearchQuery.takeIf { it.isNotBlank() },
            )
        )
    }

    private fun buildPaginationState(): PaginationState<DocumentRecordDto> {
        return PaginationState(
            data = loadedDocuments,
            currentPage = paginationInfo.currentPage,
            pageSize = PAGE_SIZE,
            hasMorePages = paginationInfo.hasMorePages,
            isLoadingMore = paginationInfo.isLoadingMore
        )
    }

    private data class PaginationInfo(
        val currentPage: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true
    )

    private fun DocumentFilter.toListFilter(): DocumentListFilter = when (this) {
        DocumentFilter.All -> DocumentListFilter.All
        DocumentFilter.NeedsAttention -> DocumentListFilter.NeedsAttention
        DocumentFilter.Confirmed -> DocumentListFilter.Confirmed
    }

    private suspend fun loadNeedsAttentionCount(): Int {
        val response = loadDocumentRecords(
            page = 0,
            pageSize = 1,
            filter = DocumentListFilter.NeedsAttention
        ).getOrNull() ?: return needsAttentionCount

        return response.total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
