package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.usecase.SearchCashflowDocumentsUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import ai.dokus.app.cashflow.viewmodel.AddDocumentViewModel
import ai.dokus.app.cashflow.viewmodel.CashflowViewModel
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceViewModel
import ai.dokus.app.cashflow.viewmodel.PeppolConnectAction
import ai.dokus.app.cashflow.viewmodel.PeppolConnectContainer
import ai.dokus.app.cashflow.viewmodel.PeppolConnectIntent
import ai.dokus.app.cashflow.viewmodel.PeppolConnectState
import ai.dokus.app.cashflow.viewmodel.PeppolSendViewModel
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import tech.dokus.foundation.app.mvi.container

val cashflowViewModelModule = module {
    single { DocumentUploadManager(dataSource = get()) }

    factory { SearchCashflowDocumentsUseCase() }
    factory { WatchPendingDocumentsUseCase(dataSource = get()) }
    viewModel { CashflowViewModel() }
    viewModel { AddDocumentViewModel() }
    viewModel { CreateInvoiceViewModel() }
    viewModel { PeppolSettingsViewModel() }
    viewModel { PeppolSendViewModel() }

    // FlowMVI Container for Peppol connection
    container<PeppolConnectContainer, PeppolConnectState, PeppolConnectIntent, PeppolConnectAction> { (params: PeppolConnectContainer.Companion.Params) ->
        PeppolConnectContainer(provider = params.provider, dataSource = get())
    }
}

val cashflowPresentationModule = module {
    includes(cashflowViewModelModule)
}
