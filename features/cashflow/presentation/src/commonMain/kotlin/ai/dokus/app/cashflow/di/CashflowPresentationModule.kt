package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.usecase.SearchCashflowDocumentsUseCase
import ai.dokus.app.cashflow.viewmodel.AddDocumentViewModel
import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val cashflowViewModelModule = module {
    factory { SearchCashflowDocumentsUseCase() }
    viewModel { CashflowViewModel() }
    viewModel { AddDocumentViewModel() }
}

val cashflowPresentationModule = module {
    includes(cashflowViewModelModule)
}
