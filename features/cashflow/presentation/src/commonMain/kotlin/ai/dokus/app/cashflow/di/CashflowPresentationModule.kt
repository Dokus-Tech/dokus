package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val cashflowViewModelModule = module {
    viewModel { CashflowViewModel() }
}

val cashflowPresentationModule = module {
    includes(cashflowViewModelModule)
}
