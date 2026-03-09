package tech.dokus.features.banking.di

import org.koin.dsl.module
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsAction
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsContainer
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsIntent
import tech.dokus.features.banking.presentation.payments.mvi.PaymentsState
import tech.dokus.foundation.app.mvi.container

val bankingViewModelModule = module {
    container<PaymentsContainer, PaymentsState, PaymentsIntent, PaymentsAction> {
        PaymentsContainer(
            listTransactions = get(),
            getTransactionSummary = get(),
            ignoreTransaction = get(),
            confirmTransaction = get(),
        )
    }
}

val bankingPresentationModule = module {
    includes(bankingViewModelModule)
}
