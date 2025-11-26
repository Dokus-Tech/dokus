package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import ai.dokus.app.cashflow.viewmodel.AddDocumentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val cashflowViewModelModule = module {
    viewModel { CashflowViewModel() }
    viewModel { AddDocumentViewModel() }
}

val cashflowPresentationModule = module {
    includes(cashflowViewModelModule)
}
