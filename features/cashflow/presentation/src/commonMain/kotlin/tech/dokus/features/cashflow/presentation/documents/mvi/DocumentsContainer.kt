@file:Suppress("TooManyFunctions")

package tech.dokus.features.cashflow.presentation.documents.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.init
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
 */
internal class DocumentsContainer(
    private val loadDocumentRecords: LoadDocumentRecordsUseCase
) : Container<DocumentsState, DocumentsIntent, DocumentsAction> {

    private val logger = Logger.forClass<DocumentsContainer>()

    private var loadedDocuments: List<DocumentRecordDto> = emptyList()
    private var paginationInfo = PaginationInfo()
    private var currentFilter: DocumentFilter = DocumentFilter.All
    private var needsAttentionCount: Int = 0
    private var confirmedCount: Int = 0

    override val store: Store<DocumentsState, DocumentsIntent, DocumentsAction> =
        store(DocumentsState.Loading) {
            init {
                handleRefresh(forceGlobalLoading = true)
            }

            reduce { intent ->
                when (intent) {
                    is DocumentsIntent.Refresh -> handleRefresh(forceGlobalLoading = false)
                    is DocumentsIntent.ExternalDocumentsChanged -> handleRefresh(forceGlobalLoading = false)
                    is DocumentsIntent.LoadMore -> handleLoadMore()
                    is DocumentsIntent.UpdateFilter -> handleUpdateFilter(intent.filter)
                    is DocumentsIntent.OpenDocument -> handleOpenDocument(intent.documentId)
                }
            }
        }

    private suspend fun DocumentsCtx.handleRefresh(forceGlobalLoading: Boolean) {
        logger.d { "Refreshing documents" }

        // Safe: intents are processed sequentially (parallelIntents = false)
        var previousContent: DocumentsState.Content? = null
        withState<DocumentsState.Content, _> {
            previousContent = this
        }

        val hasContent = previousContent != null
        val showGlobalLoading = forceGlobalLoading || !hasContent

        if (showGlobalLoading) {
            updateState { DocumentsState.Loading }
        } else {
            val contentToRefresh = requireNotNull(previousContent)
            updateState {
                contentToRefresh.copy(isRefreshing = true)
            }
        }

        val previousDocuments = loadedDocuments
        val previousPaginationInfo = paginationInfo
        val previousNeedsAttentionCount = needsAttentionCount
        val previousConfirmedCount = confirmedCount

        needsAttentionCount = loadNeedsAttentionCount()
        confirmedCount = loadConfirmedCount()
        loadDocumentRecords(
            page = 0,
            pageSize = PAGE_SIZE,
            filter = currentFilter.toListFilter(),
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
                        needsAttentionCount = this@DocumentsContainer.needsAttentionCount,
                        confirmedCount = this@DocumentsContainer.confirmedCount,
                        isRefreshing = false,
                    )
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load documents" }
                val dokusError = error.asDokusException
                if (showGlobalLoading) {
                    updateState {
                        DocumentsState.Error(
                            exception = dokusError,
                            retryHandler = { intent(DocumentsIntent.Refresh) }
                        )
                    }
                } else {
                    loadedDocuments = previousDocuments
                    paginationInfo = previousPaginationInfo
                    needsAttentionCount = previousNeedsAttentionCount
                    confirmedCount = previousConfirmedCount
                    val contentToRestore = requireNotNull(previousContent)
                    updateState {
                        contentToRestore.copy(
                            documents = buildPaginationState(),
                            needsAttentionCount = this@DocumentsContainer.needsAttentionCount,
                            confirmedCount = this@DocumentsContainer.confirmedCount,
                            isRefreshing = false
                        )
                    }
                    action(DocumentsAction.ShowError(dokusError))
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
                            needsAttentionCount = this@DocumentsContainer.needsAttentionCount,
                            confirmedCount = this@DocumentsContainer.confirmedCount
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

    private suspend fun DocumentsCtx.handleUpdateFilter(filter: DocumentFilter) {
        val previousFilter = currentFilter
        val previousDocuments = loadedDocuments
        val previousPaginationInfo = paginationInfo

        withState<DocumentsState.Content, _> {
            currentFilter = filter
            updateState {
                copy(
                    filter = filter,
                    isRefreshing = true
                )
            }

            this@DocumentsContainer.needsAttentionCount = loadNeedsAttentionCount()
            this@DocumentsContainer.confirmedCount = loadConfirmedCount()
            loadDocumentRecords(
                page = 0,
                pageSize = PAGE_SIZE,
                filter = filter.toListFilter(),
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
                            filter = filter,
                            needsAttentionCount = this@DocumentsContainer.needsAttentionCount,
                            confirmedCount = this@DocumentsContainer.confirmedCount,
                            isRefreshing = false
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to filter documents" }
                    currentFilter = previousFilter
                    loadedDocuments = previousDocuments
                    paginationInfo = previousPaginationInfo
                    updateState {
                        copy(
                            documents = buildPaginationState(),
                            filter = previousFilter,
                            needsAttentionCount = this@DocumentsContainer.needsAttentionCount,
                            confirmedCount = this@DocumentsContainer.confirmedCount,
                            isRefreshing = false
                        )
                    }
                    action(DocumentsAction.ShowError(error.asDokusException))
                }
            )
        }
    }

    private suspend fun DocumentsCtx.handleOpenDocument(documentId: tech.dokus.domain.ids.DocumentId) {
        action(
            DocumentsAction.NavigateToDocumentReview(
                documentId = documentId,
                sourceFilter = currentFilter,
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

    private suspend fun loadConfirmedCount(): Int {
        val response = loadDocumentRecords(
            page = 0,
            pageSize = 1,
            filter = DocumentListFilter.Confirmed
        ).getOrNull() ?: return confirmedCount

        return response.total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
