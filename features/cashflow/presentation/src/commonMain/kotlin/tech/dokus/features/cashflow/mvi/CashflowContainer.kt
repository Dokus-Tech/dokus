@file:Suppress(
    "TooManyFunctions", // Container handles multiple intents
    "LongParameterList", // DI requires multiple use cases
    "UnusedParameter" // Reserved parameters
)

package tech.dokus.features.cashflow.mvi

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.presentation.cashflow.components.BusinessHealthData
import tech.dokus.features.cashflow.presentation.cashflow.components.DocumentSortOption
import tech.dokus.features.cashflow.presentation.cashflow.components.VatSummaryData
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentDeletionHandle
import tech.dokus.features.cashflow.presentation.cashflow.model.DocumentUploadTask
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.FilterDocumentsUseCase
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.LoadBusinessHealthUseCase
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.LoadCashflowDocumentsUseCase
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.LoadVatSummaryUseCase
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.SearchCashflowDocumentsUseCase
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.platform.Logger

internal typealias CashflowCtx = PipelineContext<CashflowState, CashflowIntent, CashflowAction>

/**
 * Container for the Cashflow screen using FlowMVI.
 *
 * Manages documents with pagination, search, sort, VAT summary, and business health.
 * Integrates with [DocumentUploadManager] for upload tracking.
 *
 * Features:
 * - Pagination for financial documents with load more
 * - Search with debouncing (cancels previous search on new query)
 * - Pending documents watching with pagination
 * - VAT summary and business health cards
 * - Upload manager integration (exposed via [uploadTasks], [uploadedDocuments], [deletionHandles])
 *
 * Use with Koin's `container<>` DSL for automatic ViewModel wrapping and lifecycle management.
 */
internal class CashflowContainer(
    private val loadDocuments: LoadCashflowDocumentsUseCase,
    private val searchDocuments: SearchCashflowDocumentsUseCase,
    private val filterDocuments: FilterDocumentsUseCase,
    private val watchPendingDocuments: WatchPendingDocumentsUseCase,
    private val loadVatSummary: LoadVatSummaryUseCase,
    private val loadBusinessHealth: LoadBusinessHealthUseCase,
    private val uploadManager: DocumentUploadManager,
) : Container<CashflowState, CashflowIntent, CashflowAction> {

    private val logger = Logger.forClass<CashflowContainer>()

    // Upload manager flows exposed directly for UI consumption
    val uploadTasks: StateFlow<List<DocumentUploadTask>> = uploadManager.uploadTasks
    val uploadedDocuments: StateFlow<Map<String, DocumentDto>> = uploadManager.uploadedDocuments
    val deletionHandles: StateFlow<Map<String, DocumentDeletionHandle>> = uploadManager.deletionHandles

    // Internal state for search debouncing
    private var searchJob: Job? = null
    private var pendingDocumentsJob: Job? = null

    // Internal state for loaded documents and pagination
    private var loadedDocuments: List<FinancialDocumentDto> = emptyList()
    private var paginationInfo = PaginationInfo()

    // Internal state for pending documents
    private var allPendingDocuments: List<DocumentRecordDto> = emptyList()
    private var pendingVisibleCount: Int = PENDING_PAGE_SIZE

    override val store: Store<CashflowState, CashflowIntent, CashflowAction> =
        store(CashflowState.Loading) {
            reduce { intent ->
                when (intent) {
                    // Document Loading
                    is CashflowIntent.Refresh -> handleRefresh()
                    is CashflowIntent.LoadMore -> handleLoadMore()
                    is CashflowIntent.RefreshPendingDocuments -> handleRefreshPendingDocuments()
                    is CashflowIntent.LoadMorePendingDocuments -> handleLoadMorePendingDocuments()

                    // Search & Filter
                    is CashflowIntent.UpdateSearchQuery -> handleUpdateSearchQuery(intent.query)
                    is CashflowIntent.UpdateSortOption -> handleUpdateSortOption(intent.option)

                    // UI State
                    is CashflowIntent.OpenSidebar -> handleOpenSidebar()
                    is CashflowIntent.CloseSidebar -> handleCloseSidebar()
                    is CashflowIntent.ToggleSidebar -> handleToggleSidebar()
                    is CashflowIntent.ShowQrDialog -> handleShowQrDialog()
                    is CashflowIntent.HideQrDialog -> handleHideQrDialog()

                    // Document Actions
                    is CashflowIntent.CancelDeletion -> handleCancelDeletion(intent.documentId)
                    is CashflowIntent.RetryUpload -> handleRetryUpload(intent.taskId)
                    is CashflowIntent.CancelUpload -> handleCancelUpload(intent.taskId)
                    is CashflowIntent.DismissUpload -> handleDismissUpload(intent.taskId)
                }
            }
        }

    init {
        // Set callback to refresh when uploads complete
        uploadManager.setOnUploadCompleteCallback {
            store.intent(CashflowIntent.Refresh)
            store.intent(CashflowIntent.RefreshPendingDocuments)
        }
    }

    /**
     * Called when the container is cleared.
     * Should be invoked by the lifecycle owner.
     */
    fun onCleared() {
        uploadManager.clearOnUploadCompleteCallback()
    }

    /**
     * Returns the upload manager for components that need direct access.
     */
    fun provideUploadManager(): DocumentUploadManager = uploadManager

    // === Document Loading ===

    private suspend fun CashflowCtx.handleRefresh() {
        searchJob?.cancel()
        pendingDocumentsJob?.cancel()
        logger.d { "Refreshing cashflow data in parallel" }

        // Reset pagination state
        loadedDocuments = emptyList()
        paginationInfo = PaginationInfo()

        updateState { CashflowState.Loading }

        // Start pending documents watcher in background (runs indefinitely)
        pendingDocumentsJob = launch { watchPendingDocumentsAndUpdateState() }

        coroutineScope {
            // Start parallel loading of summary data and documents
            val vatJob = async { loadVatSummaryData() }
            val healthJob = async { loadBusinessHealthData() }
            val documentsJob = async { loadInitialDocuments() }

            // Wait for all to complete
            val vatSummary = vatJob.await()
            val businessHealth = healthJob.await()
            val documentsResult = documentsJob.await()

            // Transition to content state with loaded data
            documentsResult.fold(
                onSuccess = { documents ->
                    loadedDocuments = documents
                    updateState {
                        CashflowState.Content(
                            documents = buildDocumentsPaginationState(),
                            vatSummaryState = vatSummary,
                            businessHealthState = businessHealth,
                            pendingDocumentsState = buildPendingDocumentsState(),
                        )
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load cashflow data" }
                    updateState {
                        CashflowState.Error(
                            exception = error.asDokusException,
                            retryHandler = { intent(CashflowIntent.Refresh) }
                        )
                    }
                }
            )
        }
    }

    private suspend fun CashflowCtx.handleLoadMore() {
        withState<CashflowState.Content, _> {
            // Don't load more during search
            if (searchQuery.isNotEmpty()) return@withState

            // Check if we can load more
            if (paginationInfo.isLoadingMore || !paginationInfo.hasMorePages) return@withState

            paginationInfo = paginationInfo.copy(isLoadingMore = true)
            updateState {
                copy(documents = buildDocumentsPaginationState())
            }

            val nextPage = paginationInfo.currentPage + 1
            loadDocuments(page = nextPage, pageSize = PAGE_SIZE).fold(
                onSuccess = { response ->
                    logger.i { "Loaded ${response.items.size} more documents (page=$nextPage)" }
                    loadedDocuments = loadedDocuments + response.items
                    paginationInfo = paginationInfo.copy(
                        currentPage = nextPage,
                        isLoadingMore = false,
                        hasMorePages = response.hasMore
                    )
                    updateState {
                        copy(documents = buildDocumentsPaginationState())
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load more documents" }
                    paginationInfo = paginationInfo.copy(
                        isLoadingMore = false,
                        hasMorePages = false
                    )
                    updateState {
                        copy(documents = buildDocumentsPaginationState())
                    }
                }
            )
        }
    }

    private suspend fun CashflowCtx.handleRefreshPendingDocuments() {
        watchPendingDocuments.refresh()
    }

    private suspend fun CashflowCtx.handleLoadMorePendingDocuments() {
        withState<CashflowState.Content, _> {
            val currentVisible = pendingVisibleCount
            if (currentVisible >= allPendingDocuments.size) return@withState

            pendingVisibleCount = (currentVisible + PENDING_PAGE_SIZE).coerceAtMost(allPendingDocuments.size)
            updateState {
                copy(pendingDocumentsState = buildPendingDocumentsState())
            }
        }
    }

    // === Search & Filter ===

    private suspend fun CashflowCtx.handleUpdateSearchQuery(query: String) {
        val trimmed = query.trim()

        withState<CashflowState.Content, _> {
            // Cancel previous search
            searchJob?.cancel()

            // Reset pagination for new search
            loadedDocuments = emptyList()
            paginationInfo = PaginationInfo()

            // Update state immediately with new query and loading state
            updateState {
                copy(
                    searchQuery = trimmed,
                    documents = PaginationState(pageSize = PAGE_SIZE)
                )
            }

            // Start new search/load job
            searchJob = launch {
                if (trimmed.isEmpty()) {
                    // Load normal paginated data
                    loadInitialDocuments().fold(
                        onSuccess = { documents ->
                            loadedDocuments = documents
                            updateState {
                                copy(documents = buildDocumentsPaginationState())
                            }
                        },
                        onFailure = { error ->
                            logger.e(error) { "Failed to reload documents" }
                        }
                    )
                } else {
                    // Load all documents for search
                    loadAllDocumentsForSearch(trimmed)
                }
            }
        }
    }

    private suspend fun CashflowCtx.loadAllDocumentsForSearch(query: String) {
        loadDocuments.loadAll(pageSize = PAGE_SIZE).fold(
            onSuccess = { allDocuments ->
                loadedDocuments = allDocuments
                paginationInfo = paginationInfo.copy(
                    currentPage = allDocuments.size / PAGE_SIZE,
                    isLoadingMore = false,
                    hasMorePages = false
                )

                withState<CashflowState.Content, _> {
                    updateState {
                        copy(documents = buildDocumentsPaginationState())
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load search results" }
                withState<CashflowState.Content, _> {
                    updateState {
                        copy(
                            documents = buildDocumentsPaginationState()
                        )
                    }
                }
            }
        )
    }

    private suspend fun CashflowCtx.handleUpdateSortOption(option: DocumentSortOption) {
        withState<CashflowState.Content, _> {
            updateState {
                copy(
                    sortOption = option,
                    documents = buildDocumentsPaginationState(option)
                )
            }
        }
    }

    // === UI State ===

    private suspend fun CashflowCtx.handleOpenSidebar() {
        withState<CashflowState.Content, _> {
            updateState { copy(isSidebarOpen = true) }
        }
    }

    private suspend fun CashflowCtx.handleCloseSidebar() {
        withState<CashflowState.Content, _> {
            updateState { copy(isSidebarOpen = false) }
        }
    }

    private suspend fun CashflowCtx.handleToggleSidebar() {
        withState<CashflowState.Content, _> {
            updateState { copy(isSidebarOpen = !isSidebarOpen) }
        }
    }

    private suspend fun CashflowCtx.handleShowQrDialog() {
        withState<CashflowState.Content, _> {
            updateState { copy(isQrDialogOpen = true) }
        }
    }

    private suspend fun CashflowCtx.handleHideQrDialog() {
        withState<CashflowState.Content, _> {
            updateState { copy(isQrDialogOpen = false) }
        }
    }

    // === Document Actions ===

    private suspend fun CashflowCtx.handleCancelDeletion(documentId: String) {
        uploadManager.cancelDeletion(documentId)
    }

    private suspend fun CashflowCtx.handleRetryUpload(taskId: String) {
        uploadManager.retryUpload(taskId)
    }

    private suspend fun CashflowCtx.handleCancelUpload(taskId: String) {
        uploadManager.cancelUpload(taskId)
    }

    private suspend fun CashflowCtx.handleDismissUpload(taskId: String) {
        uploadManager.removeCompletedTask(taskId)
    }

    // === Private Helpers ===

    private suspend fun loadVatSummaryData(): DokusState<VatSummaryData> {
        var result: DokusState<VatSummaryData> = DokusState.loading()
        loadVatSummary().collect { state ->
            result = state
        }
        return result
    }

    private suspend fun loadBusinessHealthData(): DokusState<BusinessHealthData> {
        var result: DokusState<BusinessHealthData> = DokusState.loading()
        loadBusinessHealth().collect { state ->
            result = state
        }
        return result
    }

    private suspend fun loadInitialDocuments(): Result<List<FinancialDocumentDto>> {
        return loadDocuments(page = 0, pageSize = PAGE_SIZE).map { response ->
            paginationInfo = paginationInfo.copy(
                currentPage = 0,
                isLoadingMore = false,
                hasMorePages = response.hasMore
            )
            response.items
        }
    }

    /**
     * Watches pending documents and updates the UI state when emissions arrive.
     * This runs indefinitely in the background until cancelled.
     */
    private suspend fun CashflowCtx.watchPendingDocumentsAndUpdateState() {
        watchPendingDocuments().collect { state ->
            when (state) {
                is DokusState.Success -> {
                    allPendingDocuments = state.data
                    pendingVisibleCount = PENDING_PAGE_SIZE
                    // Update UI state if we're in Content
                    withState<CashflowState.Content, _> {
                        updateState { copy(pendingDocumentsState = buildPendingDocumentsState()) }
                    }
                }
                is DokusState.Error -> {
                    allPendingDocuments = emptyList()
                    withState<CashflowState.Content, _> {
                        updateState { copy(pendingDocumentsState = buildPendingDocumentsState()) }
                    }
                }
                else -> { /* No-op for loading/idle */ }
            }
        }
    }

    /**
     * Builds the current pagination state for documents.
     * Applies search filtering and sorting.
     */
    private fun CashflowState.Content.buildDocumentsPaginationState(
        sortOverride: DocumentSortOption? = null
    ): PaginationState<FinancialDocumentDto> {
        val currentSort = sortOverride ?: sortOption
        val filtered = searchDocuments(loadedDocuments, searchQuery)
        val sorted = filterDocuments(filtered, currentSort)

        return PaginationState(
            data = sorted,
            currentPage = paginationInfo.currentPage,
            pageSize = PAGE_SIZE,
            hasMorePages = paginationInfo.hasMorePages,
            isLoadingMore = paginationInfo.isLoadingMore
        )
    }

    /**
     * Builds the current pagination state for documents without Content state context.
     */
    private fun buildDocumentsPaginationState(): PaginationState<FinancialDocumentDto> {
        return PaginationState(
            data = loadedDocuments,
            currentPage = paginationInfo.currentPage,
            pageSize = PAGE_SIZE,
            hasMorePages = paginationInfo.hasMorePages,
            isLoadingMore = paginationInfo.isLoadingMore
        )
    }

    /**
     * Builds the current state for pending documents.
     */
    private fun buildPendingDocumentsState(): DokusState<PaginationState<DocumentRecordDto>> {
        return DokusState.success(
            PaginationState(
                data = allPendingDocuments.take(pendingVisibleCount),
                currentPage = pendingVisibleCount / PENDING_PAGE_SIZE,
                pageSize = PENDING_PAGE_SIZE,
                hasMorePages = pendingVisibleCount < allPendingDocuments.size,
                isLoadingMore = false
            )
        )
    }

    /**
     * Internal pagination tracking.
     */
    private data class PaginationInfo(
        val currentPage: Int = 0,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = true
    )

    companion object {
        private const val PAGE_SIZE = 20
        private const val PENDING_PAGE_SIZE = 4
    }
}
