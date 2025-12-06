package ai.dokus.app.viewmodel

import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.domain.model.DocumentProcessingDto
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.domain.model.common.PaginationStateCompanion
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class DashboardViewModel(
    private val getCurrentTenantUseCase: GetCurrentTenantUseCase,
    private val watchPendingDocuments: WatchPendingDocumentsUseCase
) : ViewModel() {
    private val mutableCurrentTenantState = MutableStateFlow<DokusState<Tenant?>>(DokusState.idle())
    val currentTenantState = mutableCurrentTenantState.asStateFlow()

    // Pending documents state using PaginationState
    private val _allPendingDocuments = MutableStateFlow<List<DocumentProcessingDto>>(emptyList())
    private val _pendingCurrentPage = MutableStateFlow(0)
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
                        _pendingCurrentPage.value = 0
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

    fun pendingDocumentsPreviousPage() {
        if (_pendingCurrentPage.value > 0) {
            _pendingCurrentPage.value -= 1
            updatePendingPaginationState()
            // Re-emit success state with updated pagination
            _pendingDocumentsState.value = DokusState.success(_pendingPaginationState.value)
        }
    }

    fun pendingDocumentsNextPage() {
        val allDocs = _allPendingDocuments.value
        val totalPages = calculateTotalPages(allDocs.size)
        if (_pendingCurrentPage.value < totalPages - 1) {
            _pendingCurrentPage.value += 1
            updatePendingPaginationState()
            // Re-emit success state with updated pagination
            _pendingDocumentsState.value = DokusState.success(_pendingPaginationState.value)
        }
    }

    private fun updatePendingPaginationState() {
        _pendingPaginationState.value = PaginationStateCompanion.fromLocalData(
            allData = _allPendingDocuments.value,
            currentPage = _pendingCurrentPage.value,
            pageSize = PENDING_PAGE_SIZE
        )
    }

    private fun calculateTotalPages(totalItems: Int): Int {
        return if (totalItems == 0) 1 else ((totalItems - 1) / PENDING_PAGE_SIZE) + 1
    }

    companion object {
        private const val PENDING_PAGE_SIZE = 5
    }
}
