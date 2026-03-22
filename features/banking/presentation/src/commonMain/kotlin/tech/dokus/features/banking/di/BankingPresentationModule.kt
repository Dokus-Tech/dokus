package tech.dokus.features.banking.di

import org.koin.dsl.module
import tech.dokus.features.banking.presentation.balances.mvi.BalancesContainer
import tech.dokus.features.banking.presentation.balances.mvi.BalancesIntent
import tech.dokus.features.banking.presentation.balances.mvi.BalancesState
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsContainer
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsIntent
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsState
import tech.dokus.foundation.app.mvi.container

val bankingViewModelModule = module {
    container<PaymentsContainer, PaymentsState, PaymentsIntent, Nothing> {
        PaymentsContainer(
            listTransactions = get(),
            getTransactionSummary = get(),
            listAccounts = get(),
            ignoreTransaction = get(),
            confirmTransaction = get(),
            createExpenseFromTransaction = get(),
            markTransferTransaction = get(),
            undoTransferTransaction = get(),
        )
    }

    container<BalancesContainer, BalancesState, BalancesIntent, Nothing> {
        BalancesContainer(
            listAccounts = get(),
            getAccountSummary = get(),
            getTransactionSummary = get(),
            getBalanceHistory = get(),
        )
    }
}

val bankingPresentationModule = module {
    includes(bankingViewModelModule)
}
