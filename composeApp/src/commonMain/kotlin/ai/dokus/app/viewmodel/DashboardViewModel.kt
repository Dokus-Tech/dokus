package ai.dokus.app.viewmodel

import ai.dokus.app.auth.usecases.GetCurrentTenantUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import ai.dokus.app.core.state.DokusState
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.domain.model.Tenant
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

    // Pending documents state for mobile dashboard
    private val _pendingDocumentsState = MutableStateFlow<DokusState<List<MediaDto>>>(DokusState.idle())
    val pendingDocumentsState: StateFlow<DokusState<List<MediaDto>>> = _pendingDocumentsState.asStateFlow()

    private val _pendingCurrentPage = MutableStateFlow(0)
    val pendingCurrentPage: StateFlow<Int> = _pendingCurrentPage.asStateFlow()

    init {
        // Start watching pending documents
        viewModelScope.launch {
            watchPendingDocuments().collect { state ->
                _pendingDocumentsState.value = state
                // Reset to first page when data changes
                if (state is DokusState.Success) {
                    _pendingCurrentPage.value = 0
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

    /**
     * Refresh pending documents.
     */
    fun refreshPendingDocuments() {
        watchPendingDocuments.refresh()
    }

    /**
     * Go to previous page of pending documents.
     */
    fun pendingDocumentsPreviousPage() {
        if (_pendingCurrentPage.value > 0) {
            _pendingCurrentPage.value = _pendingCurrentPage.value - 1
        }
    }

    /**
     * Go to next page of pending documents.
     */
    fun pendingDocumentsNextPage() {
        val state = _pendingDocumentsState.value
        if (state is DokusState.Success) {
            val totalPages = calculateTotalPages(state.data.size)
            if (_pendingCurrentPage.value < totalPages - 1) {
                _pendingCurrentPage.value = _pendingCurrentPage.value + 1
            }
        }
    }

    private fun calculateTotalPages(totalItems: Int): Int {
        return if (totalItems == 0) 1 else ((totalItems - 1) / PENDING_PAGE_SIZE) + 1
    }

    companion object {
        private const val PENDING_PAGE_SIZE = 5
    }
}
