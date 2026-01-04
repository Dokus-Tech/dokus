package tech.dokus.features.cashflow.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSourceImpl
import tech.dokus.features.cashflow.datasource.ChatRemoteDataSource
import tech.dokus.features.cashflow.datasource.ChatRemoteDataSourceImpl
import tech.dokus.features.cashflow.gateway.DocumentReviewGateway
import tech.dokus.features.cashflow.gateway.DocumentReviewGatewayImpl
import tech.dokus.features.cashflow.gateway.DocumentUploadGateway
import tech.dokus.features.cashflow.gateway.DocumentUploadGatewayImpl
import tech.dokus.features.cashflow.gateway.PeppolConnectionGateway
import tech.dokus.features.cashflow.gateway.PeppolConnectionGatewayImpl
import tech.dokus.features.cashflow.gateway.PeppolInboxGateway
import tech.dokus.features.cashflow.gateway.PeppolInboxGatewayImpl
import tech.dokus.features.cashflow.gateway.PeppolInvoiceGateway
import tech.dokus.features.cashflow.gateway.PeppolInvoiceGatewayImpl
import tech.dokus.features.cashflow.gateway.PeppolRecipientGateway
import tech.dokus.features.cashflow.gateway.PeppolRecipientGatewayImpl
import tech.dokus.features.cashflow.gateway.PeppolTransmissionsGateway
import tech.dokus.features.cashflow.gateway.PeppolTransmissionsGatewayImpl
import tech.dokus.features.cashflow.usecase.ConfirmDocumentUseCaseImpl
import tech.dokus.features.cashflow.usecase.ConnectPeppolUseCaseImpl
import tech.dokus.features.cashflow.usecase.DeleteDocumentUseCaseImpl
import tech.dokus.features.cashflow.usecase.DeletePeppolSettingsUseCaseImpl
import tech.dokus.features.cashflow.usecase.GetChatConfigurationUseCaseImpl
import tech.dokus.features.cashflow.usecase.GetChatSessionHistoryUseCaseImpl
import tech.dokus.features.cashflow.usecase.GetDocumentPagesUseCaseImpl
import tech.dokus.features.cashflow.usecase.GetDocumentRecordUseCaseImpl
import tech.dokus.features.cashflow.usecase.GetPeppolSettingsUseCaseImpl
import tech.dokus.features.cashflow.usecase.GetPeppolTransmissionForInvoiceUseCaseImpl
import tech.dokus.features.cashflow.usecase.ListChatSessionsUseCaseImpl
import tech.dokus.features.cashflow.usecase.ListPeppolTransmissionsUseCaseImpl
import tech.dokus.features.cashflow.usecase.LoadCashflowDocumentsUseCaseImpl
import tech.dokus.features.cashflow.usecase.PollPeppolInboxUseCaseImpl
import tech.dokus.features.cashflow.usecase.RejectDocumentUseCaseImpl
import tech.dokus.features.cashflow.usecase.ReprocessDocumentUseCaseImpl
import tech.dokus.features.cashflow.usecase.SendChatMessageUseCaseImpl
import tech.dokus.features.cashflow.usecase.SendInvoiceViaPeppolUseCaseImpl
import tech.dokus.features.cashflow.usecase.SubmitInvoiceUseCaseImpl
import tech.dokus.features.cashflow.usecase.UpdateDocumentDraftContactUseCaseImpl
import tech.dokus.features.cashflow.usecase.UpdateDocumentDraftUseCaseImpl
import tech.dokus.features.cashflow.usecase.UploadDocumentUseCaseImpl
import tech.dokus.features.cashflow.usecase.ValidateInvoiceForPeppolUseCaseImpl
import tech.dokus.features.cashflow.usecase.VerifyPeppolRecipientUseCaseImpl
import tech.dokus.features.cashflow.usecase.WatchPendingDocumentsUseCaseImpl
import tech.dokus.features.cashflow.usecases.ConfirmDocumentUseCase
import tech.dokus.features.cashflow.usecases.ConnectPeppolUseCase
import tech.dokus.features.cashflow.usecases.DeleteDocumentUseCase
import tech.dokus.features.cashflow.usecases.DeletePeppolSettingsUseCase
import tech.dokus.features.cashflow.usecases.GetChatConfigurationUseCase
import tech.dokus.features.cashflow.usecases.GetChatSessionHistoryUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentRecordUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolSettingsUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolTransmissionForInvoiceUseCase
import tech.dokus.features.cashflow.usecases.ListChatSessionsUseCase
import tech.dokus.features.cashflow.usecases.ListPeppolTransmissionsUseCase
import tech.dokus.features.cashflow.usecases.LoadCashflowDocumentsUseCase
import tech.dokus.features.cashflow.usecases.PollPeppolInboxUseCase
import tech.dokus.features.cashflow.usecases.RejectDocumentUseCase
import tech.dokus.features.cashflow.usecases.ReprocessDocumentUseCase
import tech.dokus.features.cashflow.usecases.SendChatMessageUseCase
import tech.dokus.features.cashflow.usecases.SendInvoiceViaPeppolUseCase
import tech.dokus.features.cashflow.usecases.SubmitInvoiceUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftContactUseCase
import tech.dokus.features.cashflow.usecases.UpdateDocumentDraftUseCase
import tech.dokus.features.cashflow.usecases.UploadDocumentUseCase
import tech.dokus.features.cashflow.usecases.ValidateInvoiceForPeppolUseCase
import tech.dokus.features.cashflow.usecases.VerifyPeppolRecipientUseCase
import tech.dokus.features.cashflow.usecases.WatchPendingDocumentsUseCase

/**
 * Koin DI module for Cashflow feature network configuration.
 *
 * This module sets up authenticated HTTP communication with the Cashflow backend service,
 * including JWT token management.
 *
 * Components registered:
 * - CashflowRemoteDataSource: Invoice/expense data operations
 * - ChatRemoteDataSource: Document Q&A and RAG chat operations
 * - SendChatMessageUseCase: Use case for sending chat messages
 * - Chat session and configuration use cases
 * - Cashflow gateways for Peppol and document review/upload
 * - Peppol and document review/upload use cases
 */
val cashflowNetworkModule = module {
    // ============================================================================
    // REMOTE DATA SOURCES
    // ============================================================================

    // Invoice/expense data source (needs explicit DI for endpointProvider)
    single<CashflowRemoteDataSource> {
        CashflowRemoteDataSourceImpl(get(), get<DynamicDokusEndpointProvider>())
    }

    // Chat data source for document Q&A and RAG
    singleOf(::ChatRemoteDataSourceImpl) bind ChatRemoteDataSource::class

    // ============================================================================
    // GATEWAYS
    // ============================================================================

    singleOf(::PeppolConnectionGatewayImpl) bind PeppolConnectionGateway::class
    singleOf(::PeppolRecipientGatewayImpl) bind PeppolRecipientGateway::class
    singleOf(::PeppolInvoiceGatewayImpl) bind PeppolInvoiceGateway::class
    singleOf(::PeppolInboxGatewayImpl) bind PeppolInboxGateway::class
    singleOf(::PeppolTransmissionsGatewayImpl) bind PeppolTransmissionsGateway::class
    singleOf(::DocumentReviewGatewayImpl) bind DocumentReviewGateway::class
    singleOf(::DocumentUploadGatewayImpl) bind DocumentUploadGateway::class

    // ============================================================================
    // USE CASES
    // ============================================================================

    // Chat messaging use case
    singleOf(::SendChatMessageUseCaseImpl) bind SendChatMessageUseCase::class

    // Chat session & config
    singleOf(::GetChatConfigurationUseCaseImpl) bind GetChatConfigurationUseCase::class
    singleOf(::ListChatSessionsUseCaseImpl) bind ListChatSessionsUseCase::class
    singleOf(::GetChatSessionHistoryUseCaseImpl) bind GetChatSessionHistoryUseCase::class

    // Document review
    singleOf(::GetDocumentRecordUseCaseImpl) bind GetDocumentRecordUseCase::class
    singleOf(::UpdateDocumentDraftUseCaseImpl) bind UpdateDocumentDraftUseCase::class
    singleOf(::UpdateDocumentDraftContactUseCaseImpl) bind UpdateDocumentDraftContactUseCase::class
    singleOf(::ConfirmDocumentUseCaseImpl) bind ConfirmDocumentUseCase::class
    singleOf(::RejectDocumentUseCaseImpl) bind RejectDocumentUseCase::class
    singleOf(::GetDocumentPagesUseCaseImpl) bind GetDocumentPagesUseCase::class
    singleOf(::ReprocessDocumentUseCaseImpl) bind ReprocessDocumentUseCase::class

    // Document upload
    singleOf(::UploadDocumentUseCaseImpl) bind UploadDocumentUseCase::class
    singleOf(::DeleteDocumentUseCaseImpl) bind DeleteDocumentUseCase::class

    // Peppol
    singleOf(::ConnectPeppolUseCaseImpl) bind ConnectPeppolUseCase::class
    singleOf(::GetPeppolSettingsUseCaseImpl) bind GetPeppolSettingsUseCase::class
    singleOf(::DeletePeppolSettingsUseCaseImpl) bind DeletePeppolSettingsUseCase::class
    singleOf(::ListPeppolTransmissionsUseCaseImpl) bind ListPeppolTransmissionsUseCase::class
    singleOf(::VerifyPeppolRecipientUseCaseImpl) bind VerifyPeppolRecipientUseCase::class
    singleOf(::ValidateInvoiceForPeppolUseCaseImpl) bind ValidateInvoiceForPeppolUseCase::class
    singleOf(::SendInvoiceViaPeppolUseCaseImpl) bind SendInvoiceViaPeppolUseCase::class
    singleOf(::PollPeppolInboxUseCaseImpl) bind PollPeppolInboxUseCase::class
    singleOf(::GetPeppolTransmissionForInvoiceUseCaseImpl) bind GetPeppolTransmissionForInvoiceUseCase::class

    // Cashflow documents
    factory<LoadCashflowDocumentsUseCase> { LoadCashflowDocumentsUseCaseImpl(get()) }
    factory<WatchPendingDocumentsUseCase> { WatchPendingDocumentsUseCaseImpl(get()) }
    factory<SubmitInvoiceUseCase> { SubmitInvoiceUseCaseImpl(get()) }
}
