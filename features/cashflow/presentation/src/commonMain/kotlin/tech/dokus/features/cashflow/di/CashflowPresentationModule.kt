package tech.dokus.features.cashflow.di

import org.koin.dsl.module
import tech.dokus.features.cashflow.mvi.AddDocumentAction
import tech.dokus.features.cashflow.mvi.AddDocumentContainer
import tech.dokus.features.cashflow.mvi.AddDocumentIntent
import tech.dokus.features.cashflow.mvi.AddDocumentState
import tech.dokus.features.cashflow.mvi.CashflowAction
import tech.dokus.features.cashflow.mvi.CashflowContainer
import tech.dokus.features.cashflow.mvi.CashflowIntent
import tech.dokus.features.cashflow.mvi.CashflowState
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsAction
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsContainer
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerAction
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerContainer
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerIntent
import tech.dokus.features.cashflow.presentation.ledger.mvi.CashflowLedgerState
import tech.dokus.features.cashflow.mvi.CreateInvoiceAction
import tech.dokus.features.cashflow.mvi.CreateInvoiceContainer
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.CreateInvoiceState
import tech.dokus.features.cashflow.mvi.PeppolConnectAction
import tech.dokus.features.cashflow.mvi.PeppolConnectContainer
import tech.dokus.features.cashflow.mvi.PeppolConnectIntent
import tech.dokus.features.cashflow.mvi.PeppolConnectState
import tech.dokus.features.cashflow.mvi.PeppolSendAction
import tech.dokus.features.cashflow.mvi.PeppolSendContainer
import tech.dokus.features.cashflow.mvi.PeppolSendIntent
import tech.dokus.features.cashflow.mvi.PeppolSendState
import tech.dokus.features.cashflow.mvi.PeppolSettingsAction
import tech.dokus.features.cashflow.mvi.PeppolSettingsContainer
import tech.dokus.features.cashflow.mvi.PeppolSettingsIntent
import tech.dokus.features.cashflow.mvi.PeppolSettingsState
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.FilterDocumentsUseCase
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.LoadVatSummaryUseCase
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.SearchCashflowDocumentsUseCase
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.ValidateInvoiceUseCase
import tech.dokus.features.cashflow.presentation.chat.ChatAction
import tech.dokus.features.cashflow.presentation.chat.ChatContainer
import tech.dokus.features.cashflow.presentation.chat.ChatIntent
import tech.dokus.features.cashflow.presentation.chat.ChatState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewAction
import tech.dokus.features.cashflow.presentation.review.DocumentReviewContainer
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.foundation.app.mvi.container

val cashflowViewModelModule = module {
    single {
        DocumentUploadManager(
            uploadDocumentUseCase = get(),
            deleteDocumentUseCase = get()
        )
    }

    factory { SearchCashflowDocumentsUseCase() }
    factory { FilterDocumentsUseCase() }
    factory { LoadVatSummaryUseCase() }
    factory { ValidateInvoiceUseCase() }

    // FlowMVI Containers
    container<AddDocumentContainer, AddDocumentState, AddDocumentIntent, AddDocumentAction> {
        AddDocumentContainer(uploadManager = get())
    }
    container<PeppolSettingsContainer, PeppolSettingsState, PeppolSettingsIntent, PeppolSettingsAction> {
        PeppolSettingsContainer(
            getPeppolSettings = get(),
            deletePeppolSettings = get()
        )
    }
    container<PeppolSendContainer, PeppolSendState, PeppolSendIntent, PeppolSendAction> {
        PeppolSendContainer(
            listPeppolTransmissions = get(),
            verifyPeppolRecipient = get(),
            validateInvoiceForPeppol = get(),
            sendInvoiceViaPeppol = get(),
            pollPeppolInbox = get(),
            getPeppolTransmissionForInvoice = get()
        )
    }
    container<PeppolConnectContainer, PeppolConnectState, PeppolConnectIntent, PeppolConnectAction> {
            (params: PeppolConnectContainer.Companion.Params) ->
        PeppolConnectContainer(
            provider = params.provider,
            connectPeppol = get()
        )
    }
    container<CashflowContainer, CashflowState, CashflowIntent, CashflowAction> {
        CashflowContainer(
            loadDocuments = get(),
            searchDocuments = get(),
            filterDocuments = get(),
            watchPendingDocuments = get(),
            loadVatSummary = get(),
            uploadManager = get()
        )
    }
    container<CreateInvoiceContainer, CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction> {
        CreateInvoiceContainer(
            getInvoiceNumberPreview = get(),
            validateInvoice = get(),
            submitInvoice = get()
        )
    }
    container<DocumentReviewContainer, DocumentReviewState, DocumentReviewIntent, DocumentReviewAction> {
        DocumentReviewContainer(
            getDocumentRecord = get(),
            updateDocumentDraft = get(),
            updateDocumentDraftContact = get(),
            confirmDocument = get(),
            rejectDocument = get(),
            reprocessDocument = get(),
            getDocumentPages = get(),
            getContact = get()
        )
    }
    container<ChatContainer, ChatState, ChatIntent, ChatAction> {
        ChatContainer(
            sendChatMessageUseCase = get(),
            getChatConfigurationUseCase = get(),
            listChatSessionsUseCase = get(),
            getChatSessionHistoryUseCase = get()
        )
    }
    container<DocumentsContainer, DocumentsState, DocumentsIntent, DocumentsAction> {
        DocumentsContainer(
            loadDocumentRecords = get()
        )
    }
    container<CashflowLedgerContainer, CashflowLedgerState, CashflowLedgerIntent, CashflowLedgerAction> {
            (highlightEntryId: tech.dokus.domain.ids.CashflowEntryId?) ->
        CashflowLedgerContainer(
            loadCashflowEntries = get(),
            highlightEntryId = highlightEntryId
        )
    }
}

val cashflowPresentationModule = module {
    includes(cashflowViewModelModule)
}
