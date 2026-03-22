package tech.dokus.features.cashflow.di

import org.koin.dsl.module
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.mvi.AddDocumentAction
import tech.dokus.features.cashflow.mvi.AddDocumentContainer
import tech.dokus.features.cashflow.mvi.AddDocumentIntent
import tech.dokus.features.cashflow.mvi.AddDocumentState
import tech.dokus.features.cashflow.mvi.CreateInvoiceAction
import tech.dokus.features.cashflow.mvi.CreateInvoiceContainer
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.CreateInvoiceState
import tech.dokus.features.cashflow.mvi.clientlookup.ClientLookupContainer
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.ValidateInvoiceUseCase
import tech.dokus.features.cashflow.presentation.chat.ChatAction
import tech.dokus.features.cashflow.presentation.chat.ChatContainer
import tech.dokus.features.cashflow.presentation.chat.ChatIntent
import tech.dokus.features.cashflow.presentation.chat.ChatState
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsAction
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsContainer
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.features.cashflow.presentation.overview.mvi.CashFlowOverviewAction
import tech.dokus.features.cashflow.presentation.overview.mvi.CashFlowOverviewContainer
import tech.dokus.features.cashflow.presentation.overview.mvi.CashFlowOverviewIntent
import tech.dokus.features.cashflow.presentation.overview.mvi.CashFlowOverviewState
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationAction
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationContainer
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationIntent
import tech.dokus.features.cashflow.presentation.peppol.mvi.PeppolRegistrationState
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailAction
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailContainer
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailIntent
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewAction
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewContainer
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewIntent
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewState
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.ids.DocumentMatchReviewId
import tech.dokus.features.cashflow.presentation.detail.mvi.payment.DocumentPaymentContainer
import tech.dokus.features.cashflow.presentation.detail.mvi.preview.DocumentPreviewContainer
import tech.dokus.foundation.app.mvi.container
import tech.dokus.navigation.destinations.CashFlowDestination

val cashflowViewModelModule = module {
    single {
        DocumentUploadManager(
            uploadDocumentUseCase = get(),
            deleteDocumentUseCase = get()
        )
    }

    factory { ValidateInvoiceUseCase() }

    // FlowMVI Child Containers (factory — new instance per parent)
    factory { ClientLookupContainer(get(), get(), get(), get(), get()) }
    factory { DocumentPaymentContainer(get(), get(), get(), get(), get()) }
    factory { DocumentPreviewContainer(get(), get(), get()) }

    // FlowMVI Containers
    container<AddDocumentContainer, AddDocumentState, AddDocumentIntent, AddDocumentAction> {
        AddDocumentContainer(uploadManager = get())
    }
    container<CreateInvoiceContainer, CreateInvoiceState, CreateInvoiceIntent, CreateInvoiceAction> {
        CreateInvoiceContainer(
            getInvoiceNumberPreview = get(),
            getTenantSettings = get(),
            getCurrentTenant = get(),
            validateInvoice = get(),
            submitInvoiceWithDelivery = get(),
            clientLookupContainer = get(),
        )
    }
    container<DocumentDetailContainer, DocumentDetailState, DocumentDetailIntent, DocumentDetailAction> { (initialDocumentId: DocumentId, queueContext: CashFlowDestination.DocumentDetailQueueContext) ->
        DocumentDetailContainer(
            getDocumentRecord = get(),
            updateDocumentDraft = get(),
            updateDocumentDraftContact = get(),
            confirmDocument = get(),
            unconfirmDocument = get(),
            rejectDocument = get(),
            reprocessDocument = get(),
            resolveDocumentMatchReview = get(),
            getContact = get(),
            loadDocumentRecords = get(),
            downloadDocument = get(),
            paymentContainer = get(),
            previewContainer = get(),
            initialDocumentId = initialDocumentId,
            queueContext = queueContext,
        )
    }
    container<DuplicateReviewContainer, DuplicateReviewState, DuplicateReviewIntent, DuplicateReviewAction> {
            (existingDocumentId: DocumentId, incomingDocumentId: DocumentId, reviewId: DocumentMatchReviewId, reasonType: ReviewReason) ->
        DuplicateReviewContainer(
            existingDocumentId = existingDocumentId,
            incomingDocumentId = incomingDocumentId,
            reviewId = reviewId,
            reasonType = reasonType,
            getDocumentRecord = get(),
            getDocumentPages = get(),
            resolveMatchReview = get(),
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
            loadDocumentRecords = get(),
            getDocumentCounts = get()
        )
    }
    container<CashFlowOverviewContainer, CashFlowOverviewState, CashFlowOverviewIntent, CashFlowOverviewAction> { (highlightEntryId: tech.dokus.domain.ids.CashflowEntryId?) ->
        CashFlowOverviewContainer(
            loadCashflowEntries = get(),
            getCashflowOverview = get(),
            recordPayment = get(),
            highlightEntryId = highlightEntryId
        )
    }
    container<PeppolRegistrationContainer, PeppolRegistrationState, PeppolRegistrationIntent, PeppolRegistrationAction> {
        PeppolRegistrationContainer(
            getCurrentTenant = get(),
            getRegistration = get(),
            verifyPeppolId = get(),
            enablePeppol = get(),
            enableSendingOnly = get(),
            waitForTransfer = get(),
            pollTransfer = get()
        )
    }
}

val cashflowPresentationModule = module {
    includes(cashflowViewModelModule)
}
