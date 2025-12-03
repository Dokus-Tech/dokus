package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.cashflow.usecase.SearchCashflowDocumentsUseCase
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.common.PaginationState
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class CashflowViewModel :
    BaseViewModel<DokusState<PaginationState<FinancialDocumentDto>>>(DokusState.idle()),
    KoinComponent {

    private val logger = Logger.forClass<CashflowViewModel>()
    private val cashflowDataSource: CashflowRemoteDataSource by inject()
    private val searchDocuments: SearchCashflowDocumentsUseCase by inject()
    private val loadedDocuments = MutableStateFlow<List<FinancialDocumentDto>>(emptyList())
    private val paginationState =
        MutableStateFlow(PaginationState<FinancialDocumentDto>(pageSize = PAGE_SIZE))
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun refresh() {
        scope.launch {
            logger.d { "Refreshing cashflow data" }
            paginationState.value = PaginationState(pageSize = PAGE_SIZE)
            loadedDocuments.value = emptyList()
            mutableState.emitLoading()
            loadPage(page = 0, reset = true)
        }
    }

    fun loadNextPage() {
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
        _searchQuery.value = query
        val currentState = mutableState.value
        if (loadedDocuments.value.isEmpty() &&
            (currentState is DokusState.Loading || currentState is DokusState.Idle)
        ) {
            return
        }
        emitSuccess()
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

    private fun emitSuccess() {
        val filteredDocuments = searchDocuments(loadedDocuments.value, _searchQuery.value)
        val updatedState = paginationState.value.copy(
            data = filteredDocuments,
            pageSize = PAGE_SIZE
        )
        paginationState.value = updatedState
        mutableState.value = DokusState.success(updatedState)
    }

    companion object {
        private const val PAGE_SIZE = 20
    }
}
