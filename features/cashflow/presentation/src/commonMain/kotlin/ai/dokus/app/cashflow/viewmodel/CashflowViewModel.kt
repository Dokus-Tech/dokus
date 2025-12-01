package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.PaginatedResponse
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class CashflowViewModel :
    BaseViewModel<DokusState<PaginatedResponse<FinancialDocumentDto>>>(DokusState.idle()),
    KoinComponent {

    private val logger = Logger.forClass<CashflowViewModel>()
    private val cashflowDataSource: CashflowRemoteDataSource by inject()
    private val pageSize = 20

    fun loadCashflowPage(offset: Int = 0) {
        scope.launch {
            logger.d { "Loading cashflow data (offset=$offset, limit=$pageSize)" }
            mutableState.emitLoading()

            val pageResult = cashflowDataSource.listCashflowDocuments(
                limit = pageSize,
                offset = offset
            )

            if (pageResult.isSuccess) {
                val page = pageResult.getOrThrow()
                logger.i { "Loaded ${page.items.size} cashflow documents (total=${page.total})" }
                mutableState.emit(page)
            } else {
                val error = pageResult.exceptionOrNull()!!
                logger.e(error) { "Failed to load cashflow data" }
                mutableState.emit(error) { loadCashflowPage(offset) }
            }
        }
    }

    fun loadNextPage() {
        val current = (state.value as? DokusState.Success)?.data ?: return
        val nextOffset = current.offset + pageSize
        if (nextOffset >= current.total) return
        loadCashflowPage(nextOffset)
    }

    fun loadPreviousPage() {
        val current = (state.value as? DokusState.Success)?.data ?: return
        val previousOffset = (current.offset - pageSize).coerceAtLeast(0)
        if (previousOffset == current.offset) return
        loadCashflowPage(previousOffset)
    }
}
