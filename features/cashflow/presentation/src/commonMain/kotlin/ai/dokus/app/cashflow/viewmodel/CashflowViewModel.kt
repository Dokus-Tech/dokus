package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.components.BusinessHealthData
import ai.dokus.app.cashflow.components.DocumentSortOption
import ai.dokus.app.cashflow.components.VatSummaryData
import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.model.DocumentDeletionHandle
import ai.dokus.app.cashflow.model.DocumentUploadTask
import ai.dokus.app.cashflow.usecase.FilterDocumentsUseCase
import ai.dokus.app.cashflow.usecase.LoadBusinessHealthUseCase
import ai.dokus.app.cashflow.usecase.LoadCashflowDocumentsUseCase
import ai.dokus.app.cashflow.usecase.LoadVatSummaryUseCase
import ai.dokus.app.cashflow.usecase.SearchCashflowDocumentsUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.emit
import tech.dokus.foundation.app.state.emitLoading
import kotlinx.coroutines.async
import tech.dokus.foundation.app.viewmodel.BaseViewModel
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

/**
 * ViewModel for the Cashflow screen managing documents, search, sort, and summary cards.
 */
internal class CashflowViewModel :
    BaseViewModel<DokusState<PaginationState<FinancialDocumentDto>>>(DokusState.idle()),
    KoinComponent {

    private val logger = Logger.forClass<CashflowViewModel>()

    private val loadDocuments: LoadCashflowDocumentsUseCase by inject()
    private val searchDocuments: SearchCashflowDocumentsUseCase by inject()
    private val filterDocuments: FilterDocumentsUseCase by inject()
    private val watchPendingDocuments: WatchPendingDocumentsUseCase by inject()
    private val loadVatSummary: LoadVatSummaryUseCase by inject()
    private val loadBusinessHealth: LoadBusinessHealthUseCase by inject()
    private val uploadManager: DocumentUploadManager by inject()

    private val loadedDocuments = MutableStateFlow<List<FinancialDocumentDto>>(emptyList())
    private val paginationState = MutableStateFlow(PaginationState<FinancialDocumentDto>(pageSize = PAGE_SIZE))

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var searchJob: Job? = null

    private val _sortOption = MutableStateFlow(DocumentSortOption.Default)
    val sortOption: StateFlow<DocumentSortOption> = _sortOption.asStateFlow()

    private val _isSidebarOpen = MutableStateFlow(false)
    val isSidebarOpen: StateFlow<Boolean> = _isSidebarOpen.asStateFlow()

    private val _isQrDialogOpen = MutableStateFlow(false)
    val isQrDialogOpen: StateFlow<Boolean> = _isQrDialogOpen.asStateFlow()

    val uploadTasks: StateFlow<List<DocumentUploadTask>> = uploadManager.uploadTasks
    val uploadedDocuments: StateFlow<Map<String, DocumentDto>> = uploadManager.uploadedDocuments
    val deletionHandles: StateFlow<Map<String, DocumentDeletionHandle>> = uploadManager.deletionHandles

    private val _allPendingDocuments = MutableStateFlow<List<DocumentProcessingDto>>(emptyList())
    private val _pendingVisibleCount = MutableStateFlow(PENDING_PAGE_SIZE)
    private val _pendingDocumentsState = MutableStateFlow<DokusState<PaginationState<DocumentProcessingDto>>>(DokusState.idle())
    val pendingDocumentsState: StateFlow<DokusState<PaginationState<DocumentProcessingDto>>> = _pendingDocumentsState.asStateFlow()

    private val _vatSummaryState = MutableStateFlow<DokusState<VatSummaryData>>(DokusState.loading())
    val vatSummaryState: StateFlow<DokusState<VatSummaryData>> = _vatSummaryState.asStateFlow()

    private val _businessHealthState = MutableStateFlow<DokusState<BusinessHealthData>>(DokusState.loading())
    val businessHealthState: StateFlow<DokusState<BusinessHealthData>> = _businessHealthState.asStateFlow()

    init {
        uploadManager.setOnUploadCompleteCallback { refresh(); refreshPendingDocuments() }
        viewModelScope.launch {
            watchPendingDocuments().collect { state ->
                when (state) {
                    is DokusState.Loading -> _pendingDocumentsState.value = DokusState.loading()
                    is DokusState.Success -> {
                        _allPendingDocuments.value = state.data
                        _pendingVisibleCount.value = PENDING_PAGE_SIZE
                        _pendingDocumentsState.value = DokusState.success(buildPendingPaginationState())
                    }
                    is DokusState.Error -> {
                        _allPendingDocuments.value = emptyList()
                        _pendingDocumentsState.value = DokusState.error(state.exception, state.retryHandler)
                    }
                    is DokusState.Idle -> _pendingDocumentsState.value = DokusState.idle()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        uploadManager.clearOnUploadCompleteCallback()
    }

    fun openSidebar() { _isSidebarOpen.value = true }
    fun closeSidebar() { _isSidebarOpen.value = false }
    fun showQrDialog() { _isQrDialogOpen.value = true }
    fun hideQrDialog() { _isQrDialogOpen.value = false }
    fun provideUploadManager(): DocumentUploadManager = uploadManager

    fun updateSortOption(option: DocumentSortOption) {
        _sortOption.value = option
        emitSuccess()
    }

    fun refreshPendingDocuments() { watchPendingDocuments.refresh() }

    fun pendingDocumentsLoadMore() {
        val allDocs = _allPendingDocuments.value
        val currentVisible = _pendingVisibleCount.value
        if (currentVisible >= allDocs.size) return
        _pendingVisibleCount.value = (currentVisible + PENDING_PAGE_SIZE).coerceAtMost(allDocs.size)
        _pendingDocumentsState.value = DokusState.success(buildPendingPaginationState())
    }

    private fun buildPendingPaginationState(): PaginationState<DocumentProcessingDto> {
        val allDocs = _allPendingDocuments.value
        val visibleCount = _pendingVisibleCount.value
        return PaginationState(
            data = allDocs.take(visibleCount),
            currentPage = visibleCount / PENDING_PAGE_SIZE,
            pageSize = PENDING_PAGE_SIZE,
            hasMorePages = visibleCount < allDocs.size,
            isLoadingMore = false
        )
    }

    fun refresh() {
        searchJob?.cancel()
        scope.launch {
            logger.d { "Refreshing cashflow data in parallel" }
            _vatSummaryState.value = DokusState.loading()
            _businessHealthState.value = DokusState.loading()
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedDocuments.value = emptyList()
            mutableState.emitLoading()

            val vatJob = async { loadVatSummary().collect { _vatSummaryState.value = it } }
            val healthJob = async { loadBusinessHealth().collect { _businessHealthState.value = it } }
            val documentsJob = async {
                if (_searchQuery.value.isNotEmpty()) loadSearchResults(_searchQuery.value)
                else loadPage(page = 0, reset = true)
            }
            vatJob.await(); healthJob.await(); documentsJob.await()
        }
    }

    fun loadNextPage() {
        if (_searchQuery.value.isNotEmpty()) return
        val current = paginationState.value
        if (current.isLoadingMore || !current.hasMorePages) return
        scope.launch {
            paginationState.value = current.copy(isLoadingMore = true)
            emitSuccess()
            loadPage(page = current.currentPage + 1, reset = false)
        }
    }

    fun updateSearchQuery(query: String) {
        val trimmed = query.trim()
        _searchQuery.value = trimmed
        searchJob?.cancel()
        searchJob = scope.launch {
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedDocuments.value = emptyList()
            mutableState.emitLoading()
            if (trimmed.isEmpty()) loadPage(page = 0, reset = true)
            else loadSearchResults(trimmed)
        }
    }

    private suspend fun loadPage(page: Int, reset: Boolean) {
        loadDocuments(page = page, pageSize = PAGE_SIZE).fold(
            onSuccess = { response ->
                logger.i { "Loaded ${response.items.size} documents (page=$page)" }
                loadedDocuments.value = if (reset) response.items else loadedDocuments.value + response.items
                paginationState.value = paginationState.value.copy(
                    currentPage = page, isLoadingMore = false,
                    hasMorePages = response.hasMore, pageSize = PAGE_SIZE
                )
                emitSuccess()
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load cashflow data" }
                paginationState.value = paginationState.value.copy(isLoadingMore = false, hasMorePages = false)
                if (loadedDocuments.value.isEmpty()) mutableState.emit(error) { refresh() }
                else emitSuccess()
            }
        )
    }

    private fun loadSearchResults(query: String) {
        viewModelScope.launch {
            loadDocuments.loadAll(pageSize = PAGE_SIZE).fold(
                onSuccess = { allDocuments ->
                    loadedDocuments.value = allDocuments
                    paginationState.value = paginationState.value.copy(
                        currentPage = allDocuments.size / PAGE_SIZE,
                        isLoadingMore = false, hasMorePages = false, pageSize = PAGE_SIZE
                    )
                    emitSuccess()
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load search results" }
                    paginationState.value = paginationState.value.copy(isLoadingMore = false, hasMorePages = false)
                    mutableState.emit(error) { loadSearchResults(query) }
                }
            )
        }
    }

    private fun emitSuccess() {
        val filtered = searchDocuments(loadedDocuments.value, _searchQuery.value)
        val sorted = filterDocuments(filtered, _sortOption.value)
        val updated = paginationState.value.copy(data = sorted, pageSize = PAGE_SIZE)
        paginationState.value = updated
        mutableState.value = DokusState.success(updated)
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val PENDING_PAGE_SIZE = 4
    }
}
