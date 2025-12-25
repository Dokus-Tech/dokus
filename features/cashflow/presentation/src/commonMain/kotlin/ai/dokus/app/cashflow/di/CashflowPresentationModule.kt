package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.manager.DocumentUploadManager
import ai.dokus.app.cashflow.usecase.FilterDocumentsUseCase
import ai.dokus.app.cashflow.usecase.LoadBusinessHealthUseCase
import ai.dokus.app.cashflow.usecase.LoadCashflowDocumentsUseCase
import ai.dokus.app.cashflow.usecase.LoadVatSummaryUseCase
import ai.dokus.app.cashflow.usecase.SearchCashflowDocumentsUseCase
import ai.dokus.app.cashflow.usecase.SubmitInvoiceUseCase
import ai.dokus.app.cashflow.usecase.ValidateInvoiceUseCase
import ai.dokus.app.cashflow.usecase.WatchPendingDocumentsUseCase
import ai.dokus.app.cashflow.viewmodel.AddDocumentAction
import ai.dokus.app.cashflow.viewmodel.AddDocumentContainer
import ai.dokus.app.cashflow.viewmodel.AddDocumentIntent
import ai.dokus.app.cashflow.viewmodel.AddDocumentState
import ai.dokus.app.cashflow.viewmodel.CashflowAction
import ai.dokus.app.cashflow.viewmodel.CashflowContainer
import ai.dokus.app.cashflow.viewmodel.CashflowIntent
import ai.dokus.app.cashflow.viewmodel.CashflowState
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceAction
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceContainer
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceIntent
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceState
import ai.dokus.app.cashflow.viewmodel.PeppolConnectAction
import ai.dokus.app.cashflow.viewmodel.PeppolConnectContainer
import ai.dokus.app.cashflow.viewmodel.PeppolConnectIntent
import ai.dokus.app.cashflow.viewmodel.PeppolConnectState
import ai.dokus.app.cashflow.viewmodel.PeppolSendAction
import ai.dokus.app.cashflow.viewmodel.PeppolSendContainer
import ai.dokus.app.cashflow.viewmodel.PeppolSendIntent
import ai.dokus.app.cashflow.viewmodel.PeppolSendState
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsAction
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsContainer
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsIntent
import ai.dokus.app.cashflow.viewmodel.PeppolSettingsState
import org.koin.dsl.module
import tech.dokus.foundation.app.mvi.container

val cashflowViewModelModule = module {
    single { DocumentUploadManager(dataSource = get()) }

    factory { SearchCashflowDocumentsUseCase() }
    factory { FilterDocumentsUseCase() }
    factory { LoadCashflowDocumentsUseCase(dataSource = get()) }
    factory { WatchPendingDocumentsUseCase(dataSource = get()) }
    factory { LoadVatSummaryUseCase() }
    factory { LoadBusinessHealthUseCase() }
    factory { ValidateInvoiceUseCase() }
    factory { SubmitInvoiceUseCase(dataSource = get()) }

    // FlowMVI Containers
    container<AddDocumentContainer, AddDocumentState, AddDocumentIntent, AddDocumentAction> {
        AddDocumentContainer(uploadManager = get())
    }
    container<PeppolSettingsContainer, PeppolSettingsState, PeppolSettingsIntent, PeppolSettingsAction> {
        PeppolSettingsContainer(dataSource = get())
    }
    container<PeppolSendContainer, PeppolSendState, PeppolSendIntent, PeppolSendAction> {
        PeppolSendContainer(dataSource = get())
    }
    container<PeppolConnectContainer, PeppolConnectState, PeppolConnectIntent, PeppolConnectAction> { (params: PeppolConnectContainer.Companion.Params) ->
        PeppolConnectContainer(provider = params.provider, dataSource = get())
    }
    container<CashflowContainer, CashflowState, CashflowIntent, CashflowAction> {
        CashflowContainer(
            loadDocuments = get(),
            searchDocuments = get(),
            filterDocuments = get(),
            watchPendingDocuments = get(),
            loadVatSummary = get(),
            loadBusinessHealth = get(),
            uploadManager = get()
        )
    }
    container<CreateInvoiceContainer, CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction> {
        CreateInvoiceContainer(
            tenantDataSource = get(),
            validateInvoice = get(),
            submitInvoice = get()
        )
    }
}

val cashflowPresentationModule = module {
    includes(cashflowViewModelModule)
}
