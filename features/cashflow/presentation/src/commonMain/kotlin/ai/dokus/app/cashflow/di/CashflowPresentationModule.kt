package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.usecase.SearchCashflowDocumentsUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import ai.dokus.app.cashflow.viewmodel.AddDocumentViewModel
import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val cashflowViewModelModule = module {
    // Upload manager as singleton to persist state across screens
    single { DocumentUploadManager(dataSource = get()) }

    factory { SearchCashflowDocumentsUseCase() }
    factory { WatchPendingDocumentsUseCase(mediaRepository = get()) }
    viewModel { CashflowViewModel() }
    viewModel { AddDocumentViewModel() }
}

val cashflowPresentationModule = module {
    includes(cashflowViewModelModule)
}
