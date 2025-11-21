package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.model.Expense
import ai.dokus.foundation.domain.model.Invoice
import ai.dokus.foundation.domain.rpc.CashflowRemoteService
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class CashflowViewModel : BaseViewModel<CashflowViewModel.State>(State.Loading), KoinComponent {

    private val logger = Logger.forClass<CashflowViewModel>()
    private val cashflowRemoteService: CashflowRemoteService by inject()

    sealed interface State {
        data object Loading : State
        data class Success(
            val invoices: List<Invoice>,
            val expenses: List<Expense>
        ) : State
        data class Error(val exception: DokusException) : State
    }

    init {
        loadCashflowData()
    }

    fun loadCashflowData() = scope.launch {
        logger.d { "Loading cashflow data" }
        mutableState.value = State.Loading

        try {
            // Load invoices and expenses from RPC (will throw on error)
            val invoices = cashflowRemoteService.listInvoices(
                limit = 50,
                offset = 0
            )

            val expenses = cashflowRemoteService.listExpenses(
                limit = 50,
                offset = 0
            )

            logger.i { "Loaded ${invoices.size} invoices and ${expenses.size} expenses" }
            mutableState.value = State.Success(
                invoices = invoices,
                expenses = expenses
            )
        } catch (e: Exception) {
            logger.e(e) { "Failed to load cashflow data" }
            handleError(e)
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
