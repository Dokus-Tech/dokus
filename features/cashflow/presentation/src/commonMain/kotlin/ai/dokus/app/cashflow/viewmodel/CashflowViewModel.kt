package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.components.DocumentSortOption
import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.usecase.SearchCashflowDocumentsUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.model.DocumentDto
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.domain.model.common.PaginationStateCompanion
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

    // Pending documents state using PaginationState
    private val _allPendingDocuments = MutableStateFlow<List<MediaDto>>(emptyList())
    private val _pendingCurrentPage = MutableStateFlow(0)
    private val _pendingDocumentsLoading = MutableStateFlow(true)

    private val _pendingPaginationState = MutableStateFlow(
        PaginationState<MediaDto>(pageSize = PENDING_PAGE_SIZE)
    )
    val pendingPaginationState: StateFlow<PaginationState<MediaDto>> = _pendingPaginationState.asStateFlow()

    val isPendingLoading: StateFlow<Boolean> = _pendingDocumentsLoading.asStateFlow()

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
                _pendingDocumentsLoading.value = state is DokusState.Loading
                when (state) {
                    is DokusState.Success -> {
                        _allPendingDocuments.value = state.data
                        _pendingCurrentPage.value = 0
                        updatePendingPaginationState()
                    }

                    is DokusState.Error -> {
                        _allPendingDocuments.value = emptyList()
                        updatePendingPaginationState()
                    }

                    else -> {}
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

    fun pendingDocumentsPreviousPage() {
        if (_pendingCurrentPage.value > 0) {
            _pendingCurrentPage.value -= 1
            updatePendingPaginationState()
        }
    }

    fun pendingDocumentsNextPage() {
        val allDocs = _allPendingDocuments.value
        val totalPages = calculateTotalPages(allDocs.size, PENDING_PAGE_SIZE)
        if (_pendingCurrentPage.value < totalPages - 1) {
            _pendingCurrentPage.value += 1
            updatePendingPaginationState()
        }
    }

    private fun updatePendingPaginationState() {
        _pendingPaginationState.value = PaginationStateCompanion.fromLocalData(
            allData = _allPendingDocuments.value,
            currentPage = _pendingCurrentPage.value,
            pageSize = PENDING_PAGE_SIZE
        )
    }

    private fun calculateTotalPages(totalItems: Int, pageSize: Int): Int {
        return if (totalItems == 0) 1 else ((totalItems - 1) / pageSize) + 1
    }

    // endregion

    // region Documents

    fun refresh() {
        searchJob?.cancel()
        scope.launch {
            logger.d { "Refreshing cashflow data" }
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedDocuments.value = emptyList()
            mutableState.emitLoading()
            if (_searchQuery.value.isNotEmpty()) {
                loadSearchResults(_searchQuery.value)
            } else {
                loadPage(page = 0, reset = true)
            }
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
        private const val PENDING_PAGE_SIZE = 5
    }
}
