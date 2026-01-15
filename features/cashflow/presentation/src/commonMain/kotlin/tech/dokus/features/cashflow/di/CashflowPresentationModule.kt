package tech.dokus.features.cashflow.di

import org.koin.dsl.module
import tech.dokus.features.cashflow.mvi.AddDocumentAction
import tech.dokus.features.cashflow.mvi.AddDocumentContainer
import tech.dokus.features.cashflow.mvi.AddDocumentIntent
import tech.dokus.features.cashflow.mvi.AddDocumentState
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
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.ValidateInvoiceUseCase
import tech.dokus.features.cashflow.presentation.chat.ChatAction
import tech.dokus.features.cashflow.presentation.chat.ChatContainer
import tech.dokus.features.cashflow.presentation.chat.ChatIntent
import tech.dokus.features.cashflow.presentation.chat.ChatState
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationAction
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationContainer
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationIntent
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationState
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

    factory { ValidateInvoiceUseCase() }

    // FlowMVI Containers
    container<AddDocumentContainer, AddDocumentState, AddDocumentIntent, AddDocumentAction> {
        AddDocumentContainer(uploadManager = get())
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
            recordPayment = get(),
            highlightEntryId = highlightEntryId
        )
    }
    container<PeppolRegistrationContainer, PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction> {
        PeppolRegistrationContainer(
            getRegistration = get(),
            verifyPeppolId = get(),
            enablePeppol = get(),
            waitForTransfer = get(),
            optOut = get(),
            pollTransfer = get(),
            validateOgm = get()
        )
    }
}

val cashflowPresentationModule = module {
    includes(cashflowViewModelModule)
}
