package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.components.BusinessHealthData
import ai.dokus.app.cashflow.components.DocumentSortOption
import ai.dokus.app.cashflow.components.VatSummaryData
import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.usecase.SearchCashflowDocumentsUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import kotlinx.coroutines.async
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.model.DocumentDto
import ai.dokus.foundation.domain.model.DocumentProcessingDto
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.platform.Logger
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class CashflowViewModel :
    BaseViewModel<DokusState<PaginationState<FinancialDocumentDto>>>(DokusState.idle()),
    KoinComponent {

    private val logger = Logger.forClass<CashflowViewModel>()
    private val cashflowDataSource: CashflowRemoteDataSource by inject()
    private val searchDocuments: SearchCashflowDocumentsUseCase by inject()
    private val uploadManager: DocumentUploadManager by inject()
    private val watchPendingDocuments: WatchPendingDocumentsUseCase by inject()

    // Documents state
    private val loadedDocuments = MutableStateFlow<List<FinancialDocumentDto>>(emptyList())
    private val paginationState = MutableStateFlow(PaginationState<FinancialDocumentDto>(pageSize = PAGE_SIZE))

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var searchJob: Job? = null

    // Sort state
    private val _sortOption = MutableStateFlow(DocumentSortOption.Default)
    val sortOption: StateFlow<DocumentSortOption> = _sortOption.asStateFlow()

    // Sidebar state for desktop
    private val _isSidebarOpen = MutableStateFlow(false)
    val isSidebarOpen: StateFlow<Boolean> = _isSidebarOpen.asStateFlow()

    // QR dialog state
    private val _isQrDialogOpen = MutableStateFlow(false)
    val isQrDialogOpen: StateFlow<Boolean> = _isQrDialogOpen.asStateFlow()

    // Upload state exposed from manager
    val uploadTasks: StateFlow<List<DocumentUploadTask>> = uploadManager.uploadTasks
    val uploadedDocuments: StateFlow<Map<String, DocumentDto>> = uploadManager.uploadedDocuments
    val deletionHandles: StateFlow<Map<String, DocumentDeletionHandle>> = uploadManager.deletionHandles

    // Pending documents state using PaginationState (lazy loading)
    private val _allPendingDocuments = MutableStateFlow<List<DocumentProcessingDto>>(emptyList())
    private val _pendingVisibleCount = MutableStateFlow(PENDING_PAGE_SIZE)
    private val _pendingPaginationState = MutableStateFlow(
        PaginationState<DocumentProcessingDto>(pageSize = PENDING_PAGE_SIZE)
    )

    // Full state for pending documents (includes loading, success, error)
    private val _pendingDocumentsState = MutableStateFlow<DokusState<PaginationState<DocumentProcessingDto>>>(DokusState.idle())
    val pendingDocumentsState: StateFlow<DokusState<PaginationState<DocumentProcessingDto>>> = _pendingDocumentsState.asStateFlow()

    // VAT Summary state (independent loading)
    private val _vatSummaryState = MutableStateFlow<DokusState<VatSummaryData>>(DokusState.loading())
    val vatSummaryState: StateFlow<DokusState<VatSummaryData>> = _vatSummaryState.asStateFlow()

    // Business Health state (independent loading)
    private val _businessHealthState = MutableStateFlow<DokusState<BusinessHealthData>>(DokusState.loading())
    val businessHealthState: StateFlow<DokusState<BusinessHealthData>> = _businessHealthState.asStateFlow()

    init {
        // Set up auto-refresh when uploads complete
        uploadManager.setOnUploadCompleteCallback {
            logger.d { "Upload completed, refreshing cashflow documents" }
            refresh()
            refreshPendingDocuments()
        }

        // Start watching pending documents
        viewModelScope.launch {
            watchPendingDocuments().collect { state ->
                when (state) {
                    is DokusState.Loading -> {
                        _pendingDocumentsState.value = DokusState.loading()
                    }

                    is DokusState.Success -> {
                        _allPendingDocuments.value = state.data
                        _pendingVisibleCount.value = PENDING_PAGE_SIZE
                        updatePendingPaginationState()
                        // Wrap pagination state in Success
                        _pendingDocumentsState.value = DokusState.success(_pendingPaginationState.value)
                    }

                    is DokusState.Error -> {
                        _allPendingDocuments.value = emptyList()
                        updatePendingPaginationState()
                        // Preserve error with retry handler
                        _pendingDocumentsState.value = DokusState.error(state.exception, state.retryHandler)
                    }

                    is DokusState.Idle -> {
                        _pendingDocumentsState.value = DokusState.idle()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        uploadManager.clearOnUploadCompleteCallback()
    }

    // region Sidebar & Dialog

    fun openSidebar() {
        _isSidebarOpen.value = true
    }

    fun closeSidebar() {
        _isSidebarOpen.value = false
    }

    fun showQrDialog() {
        _isQrDialogOpen.value = true
    }

    fun hideQrDialog() {
        _isQrDialogOpen.value = false
    }

    fun provideUploadManager(): DocumentUploadManager = uploadManager

    // endregion

    // region Sort

    fun updateSortOption(option: DocumentSortOption) {
        _sortOption.value = option
        emitSuccess()
    }

    private fun sortDocuments(documents: List<FinancialDocumentDto>): List<FinancialDocumentDto> {
        return when (_sortOption.value) {
            DocumentSortOption.Default -> documents
            DocumentSortOption.DateNewest -> documents.sortedByDescending { it.date }
            DocumentSortOption.DateOldest -> documents.sortedBy { it.date }
            DocumentSortOption.AmountHighest -> documents.sortedByDescending {
                it.amount.value.toDoubleOrNull() ?: 0.0
            }

            DocumentSortOption.AmountLowest -> documents.sortedBy {
                it.amount.value.toDoubleOrNull() ?: 0.0
            }

            DocumentSortOption.Type -> documents.sortedBy { document ->
                when (document) {
                    is FinancialDocumentDto.InvoiceDto -> 0
                    is FinancialDocumentDto.ExpenseDto -> 1
                    is FinancialDocumentDto.BillDto -> 2
                }
            }
        }
    }

    // endregion

    // region Pending Documents

    fun refreshPendingDocuments() {
        watchPendingDocuments.refresh()
    }

    /**
     * Load more pending documents for infinite scroll.
     * Increases the visible count by PENDING_PAGE_SIZE.
     */
    fun pendingDocumentsLoadMore() {
        val allDocs = _allPendingDocuments.value
        val currentVisible = _pendingVisibleCount.value

        // Don't load more if we're already showing all items
        if (currentVisible >= allDocs.size) return

        // Increase visible count
        _pendingVisibleCount.value = (currentVisible + PENDING_PAGE_SIZE).coerceAtMost(allDocs.size)
        updatePendingPaginationState()
        _pendingDocumentsState.value = DokusState.success(_pendingPaginationState.value)
    }

    private fun updatePendingPaginationState() {
        val allDocs = _allPendingDocuments.value
        val visibleCount = _pendingVisibleCount.value
        val visibleDocs = allDocs.take(visibleCount)
        val hasMore = visibleCount < allDocs.size

        _pendingPaginationState.value = PaginationState(
            data = visibleDocs,
            currentPage = visibleCount / PENDING_PAGE_SIZE,
            pageSize = PENDING_PAGE_SIZE,
            hasMorePages = hasMore,
            isLoadingMore = false
        )
    }

    // endregion

    // region Documents

    fun refresh() {
        searchJob?.cancel()
        scope.launch {
            logger.d { "Refreshing cashflow data in parallel" }

            // Start all loading states
            _vatSummaryState.value = DokusState.loading()
            _businessHealthState.value = DokusState.loading()
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedDocuments.value = emptyList()
            mutableState.emitLoading()

            // Load all sections in parallel
            val vatJob = async { loadVatSummary() }
            val healthJob = async { loadBusinessHealth() }
            val documentsJob = async {
                if (_searchQuery.value.isNotEmpty()) {
                    loadSearchResults(_searchQuery.value)
                } else {
                    loadPage(page = 0, reset = true)
                }
            }

            // Await all jobs (each updates its own state)
            vatJob.await()
            healthJob.await()
            documentsJob.await()
        }
    }

    /**
     * Load VAT summary data.
     * TODO: Replace with actual API call when endpoint is available.
     */
    private suspend fun loadVatSummary() {
        try {
            // Placeholder data - replace with API call
            val data = VatSummaryData.empty
            _vatSummaryState.value = DokusState.success(data)
            logger.d { "VAT summary loaded successfully" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to load VAT summary" }
            _vatSummaryState.value = DokusState.error(
                ai.dokus.foundation.domain.exceptions.DokusException.Unknown(e)
            ) { scope.launch { loadVatSummary() } }
        }
    }

    /**
     * Load business health data.
     * TODO: Replace with actual API call when endpoint is available.
     */
    private suspend fun loadBusinessHealth() {
        try {
            // Placeholder data - replace with API call
            val data = BusinessHealthData.empty
            _businessHealthState.value = DokusState.success(data)
            logger.d { "Business health loaded successfully" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to load business health" }
            _businessHealthState.value = DokusState.error(
                ai.dokus.foundation.domain.exceptions.DokusException.Unknown(e)
            ) { scope.launch { loadBusinessHealth() } }
        }
    }

    fun loadNextPage() {
        if (_searchQuery.value.isNotEmpty()) return
        val current = paginationState.value
        if (current.isLoadingMore || !current.hasMorePages) return

        scope.launch {
            logger.d { "Loading next cashflow page (current=${current.currentPage})" }
            paginationState.value = current.copy(isLoadingMore = true)
            emitSuccess()
            loadPage(page = current.currentPage + 1, reset = false)
        }
    }

    fun updateSearchQuery(query: String) {
        val trimmedQuery = query.trim()
        _searchQuery.value = trimmedQuery
        searchJob?.cancel()
        searchJob = scope.launch {
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedDocuments.value = emptyList()
            mutableState.emitLoading()
            if (trimmedQuery.isEmpty()) {
                loadPage(page = 0, reset = true)
            } else {
                loadSearchResults(trimmedQuery)
            }
        }
    }

    private suspend fun loadPage(page: Int, reset: Boolean) {
        val offset = page * PAGE_SIZE
        val pageResult = cashflowDataSource.listCashflowDocuments(
            limit = PAGE_SIZE,
            offset = offset
        )

        pageResult.fold(
            onSuccess = { response ->
                logger.i { "Loaded ${response.items.size} cashflow documents (offset=$offset, total=${response.total})" }
                loadedDocuments.value =
                    if (reset) response.items else loadedDocuments.value + response.items

                paginationState.value = paginationState.value.copy(
                    currentPage = page,
                    isLoadingMore = false,
                    hasMorePages = response.hasMore,
                    pageSize = PAGE_SIZE
                )
                emitSuccess()
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load cashflow data" }
                paginationState.value = paginationState.value.copy(
                    isLoadingMore = false,
                    hasMorePages = false
                )
                if (loadedDocuments.value.isEmpty()) {
                    mutableState.emit(error) { refresh() }
                } else {
                    emitSuccess()
                }
            }
        )
    }

    private fun loadSearchResults(query: String) {
        viewModelScope.launch {
            val allDocuments = mutableListOf<FinancialDocumentDto>()
            var offset = 0
            var hasMore: Boolean

            do {
                val pageResult = cashflowDataSource.listCashflowDocuments(
                    limit = PAGE_SIZE,
                    offset = offset
                )

                if (pageResult.isFailure) {
                    val error = pageResult.exceptionOrNull()!!
                    logger.e(error) { "Failed to load cashflow search results" }
                    paginationState.value = paginationState.value.copy(
                        isLoadingMore = false,
                        hasMorePages = false
                    )
                    mutableState.emit(error) { loadSearchResults(query) }
                    return@launch
                }

                val page = pageResult.getOrThrow()
                allDocuments.addAll(page.items)
                hasMore = page.hasMore
                offset += PAGE_SIZE
            } while (hasMore)

            loadedDocuments.value = allDocuments
            paginationState.value = paginationState.value.copy(
                currentPage = allDocuments.size / PAGE_SIZE,
                isLoadingMore = false,
                hasMorePages = false,
                pageSize = PAGE_SIZE
            )
            emitSuccess()
        }
    }

    private fun emitSuccess() {
        val filteredDocuments = searchDocuments(loadedDocuments.value, _searchQuery.value)
        val sortedDocuments = sortDocuments(filteredDocuments)
        val updatedState = paginationState.value.copy(
            data = sortedDocuments,
            pageSize = PAGE_SIZE
        )
        paginationState.value = updatedState
        mutableState.value = DokusState.success(updatedState)
    }

    // endregion

    companion object {
        private const val PAGE_SIZE = 20
        private const val PENDING_PAGE_SIZE = 4
    }
}
