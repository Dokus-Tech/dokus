package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class CashflowViewModel : BaseViewModel<CashflowViewModel.State>(State.Loading), KoinComponent {

    private val logger = Logger.forClass<CashflowViewModel>()
    private val cashflowDataSource: CashflowRemoteDataSource by inject()

    sealed interface State {
        data object Loading : State
        data class Success(
            val invoices: List<FinancialDocumentDto.InvoiceDto>,
            val expenses: List<FinancialDocumentDto.ExpenseDto>
        ) : State
        data class Error(val exception: DokusException) : State
    }

    init {
        loadCashflowData()
    }

    fun loadCashflowData() = scope.launch {
        logger.d { "Loading cashflow data" }
        mutableState.value = State.Loading

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
            mutableState.value = State.Success(
                invoices = invoices,
                expenses = expenses
            )
        } else {
            val error = invoicesResult.exceptionOrNull() ?: expensesResult.exceptionOrNull()!!
            logger.e(error) { "Failed to load cashflow data" }
            handleError(error)
        }
    }

    fun refresh() {
        loadCashflowData()
    }

    private fun handleError(error: Throwable) {
        val dokusException = when {
            error is DokusException -> error
            error.message?.contains("not found", ignoreCase = true) == true ->
                DokusException.ConnectionError()
            error.message?.contains("unauthorized", ignoreCase = true) == true ->
                DokusException.NotAuthenticated()
            else -> DokusException.ConnectionError()
        }
        mutableState.value = State.Error(dokusException)
    }
}
