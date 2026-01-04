package tech.dokus.features.cashflow.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSourceImpl
import tech.dokus.features.cashflow.datasource.ChatRemoteDataSource
import tech.dokus.features.cashflow.datasource.ChatRemoteDataSourceImpl
import tech.dokus.features.cashflow.usecase.DocumentReviewUseCaseImpl
import tech.dokus.features.cashflow.usecase.DocumentUploadUseCaseImpl
import tech.dokus.features.cashflow.usecase.GetChatConfigurationUseCaseImpl
import tech.dokus.features.cashflow.usecase.GetChatSessionHistoryUseCaseImpl
import tech.dokus.features.cashflow.usecase.ListChatSessionsUseCaseImpl
import tech.dokus.features.cashflow.usecase.LoadCashflowDocumentsUseCaseImpl
import tech.dokus.features.cashflow.usecase.PeppolUseCaseImpl
import tech.dokus.features.cashflow.usecase.SendChatMessageUseCaseImpl
import tech.dokus.features.cashflow.usecase.SubmitInvoiceUseCaseImpl
import tech.dokus.features.cashflow.usecase.WatchPendingDocumentsUseCaseImpl
import tech.dokus.features.cashflow.usecases.DocumentReviewUseCase
import tech.dokus.features.cashflow.usecases.DocumentUploadUseCase
import tech.dokus.features.cashflow.usecases.GetChatConfigurationUseCase
import tech.dokus.features.cashflow.usecases.GetChatSessionHistoryUseCase
import tech.dokus.features.cashflow.usecases.ListChatSessionsUseCase
import tech.dokus.features.cashflow.usecases.LoadCashflowDocumentsUseCase
import tech.dokus.features.cashflow.usecases.PeppolUseCase
import tech.dokus.features.cashflow.usecases.SendChatMessageUseCase
import tech.dokus.features.cashflow.usecases.SubmitInvoiceUseCase
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
 * - Document review, upload, and Peppol use cases
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
    // USE CASES
    // ============================================================================

    // Chat messaging use case
    singleOf(::SendChatMessageUseCaseImpl) bind SendChatMessageUseCase::class

    // Chat session & config
    singleOf(::GetChatConfigurationUseCaseImpl) bind GetChatConfigurationUseCase::class
    singleOf(::ListChatSessionsUseCaseImpl) bind ListChatSessionsUseCase::class
    singleOf(::GetChatSessionHistoryUseCaseImpl) bind GetChatSessionHistoryUseCase::class

    // Document review & upload
    singleOf(::DocumentReviewUseCaseImpl) bind DocumentReviewUseCase::class
    singleOf(::DocumentUploadUseCaseImpl) bind DocumentUploadUseCase::class

    // Peppol
    singleOf(::PeppolUseCaseImpl) bind PeppolUseCase::class

    // Cashflow documents
    factory<LoadCashflowDocumentsUseCase> { LoadCashflowDocumentsUseCaseImpl(get()) }
    factory<WatchPendingDocumentsUseCase> { WatchPendingDocumentsUseCaseImpl(get()) }
    factory<SubmitInvoiceUseCase> { SubmitInvoiceUseCaseImpl(get()) }
}
