package tech.dokus.app.viewmodel

import ai.dokus.app.auth.datasource.TenantRemoteDataSource
import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.domain.model.CompanyAvatar
import ai.dokus.foundation.domain.model.DocumentProcessingDto
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.common.PaginationState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DashboardViewModel(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
    private val watchPendingDocuments: WatchPendingDocumentsUseCase,
    private val tenantDataSource: TenantRemoteDataSource
) : ViewModel() {
    private val mutableCurrentTenantState = MutableStateFlow<DokusState<Tenant?>>(DokusState.idle())
    val currentTenantState = mutableCurrentTenantState.asStateFlow()

    private val _currentAvatar = MutableStateFlow<CompanyAvatar?>(null)
    val currentAvatar: StateFlow<CompanyAvatar?> = _currentAvatar.asStateFlow()

    // Pending documents state using PaginationState (lazy loading)
    private val _allPendingDocuments = MutableStateFlow<List<DocumentProcessingDto>>(emptyList())
    private val _pendingVisibleCount = MutableStateFlow(PENDING_PAGE_SIZE)
    private val _pendingPaginationState = MutableStateFlow(
        PaginationState<DocumentProcessingDto>(pageSize = PENDING_PAGE_SIZE)
    )

    // Full state for pending documents (includes loading, success, error)
    private val _pendingDocumentsState = MutableStateFlow<DokusState<PaginationState<DocumentProcessingDto>>>(DokusState.idle())
    val pendingDocumentsState: StateFlow<DokusState<PaginationState<DocumentProcessingDto>>> = _pendingDocumentsState.asStateFlow()

    init {
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

    fun refreshTenant() {
        viewModelScope.launch {
            mutableCurrentTenantState.value = DokusState.loading()

            val nextState = getCurrentTenantUseCase().fold(
                onSuccess = { tenant ->
                    // Use avatar from tenant (already included in response)
                    _currentAvatar.value = tenant?.avatar
                    DokusState.success(tenant)
                },
                onFailure = {
                    DokusState.error(it) { refreshTenant() }
                }
            )

            mutableCurrentTenantState.value = nextState
        }
    }

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

    companion object {
        private const val PENDING_PAGE_SIZE = 5
    }
}
