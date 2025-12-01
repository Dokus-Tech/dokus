package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.emit
import ai.dokus.app.core.state.emitLoading
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class CashflowViewModel : BaseViewModel<DokusState<List<FinancialDocumentDto>>>(DokusState.idle()),
    KoinComponent {

    private val logger = Logger.forClass<CashflowViewModel>()
    private val cashflowDataSource: CashflowRemoteDataSource by inject()

    fun loadCashflowData() {
        scope.launch {
            logger.d { "Loading cashflow data" }
            mutableState.emitLoading()

            // Load invoices and expenses in parallel
            val invoicesDeferred = async {
                cashflowDataSource.listInvoices(limit = 50, offset = 0)
            }
            val expensesDeferred = async {
                cashflowDataSource.listExpenses(limit = 50, offset = 0)
            }

            val invoicesResult = invoicesDeferred.await()
            val expensesResult = expensesDeferred.await()

            if (invoicesResult.isSuccess && expensesResult.isSuccess) {
                val invoices = invoicesResult.getOrThrow()
                val expenses = expensesResult.getOrThrow()
                logger.i { "Loaded ${invoices.size} invoices and ${expenses.size} expenses" }
                mutableState.emit(invoices + expenses)
            } else {
                val error = invoicesResult.exceptionOrNull() ?: expensesResult.exceptionOrNull()!!
                logger.e(error) { "Failed to load cashflow data" }
                mutableState.emit(error) { loadCashflowData() }
            }
        }
    }
}
