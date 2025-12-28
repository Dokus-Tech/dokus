package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.cache.CashflowCacheDatabase
import ai.dokus.app.cashflow.cache.CashflowDb
import ai.dokus.app.cashflow.cache.InvoiceLocalDataSource
import ai.dokus.app.cashflow.cache.InvoiceLocalDataSourceImpl
import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSourceImpl
import ai.dokus.app.cashflow.datasource.ChatRemoteDataSource
import ai.dokus.app.cashflow.datasource.ChatRemoteDataSourceImpl
import ai.dokus.app.cashflow.repository.CashflowDataRepository
import ai.dokus.app.cashflow.repository.ChatRepositoryImpl
import ai.dokus.app.cashflow.usecase.SendChatMessageUseCase
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import tech.dokus.domain.config.DynamicDokusEndpointProvider

/**
 * Koin DI module for Cashflow feature network configuration.
 *
 * This module sets up authenticated HTTP communication with the Cashflow backend service,
 * including JWT token management and local caching for offline support.
 *
 * Components registered:
 * - CashflowRemoteDataSource: Invoice/expense data operations
 * - ChatRemoteDataSource: Document Q&A and RAG chat operations
 * - InvoiceLocalDataSource: Local caching for offline support
 * - CashflowDataRepository: Invoice/expense repository
 * - ChatRepositoryImpl: Chat message and session repository
 * - SendChatMessageUseCase: Use case for sending chat messages
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
    // DATABASE & LOCAL STORAGE
    // ============================================================================

    // Database wrapper for cache
    single { CashflowDb.create() }
    single<CashflowCacheDatabase> { get<CashflowDb>().get() }

    // Local data source for caching
    single<InvoiceLocalDataSource> {
        InvoiceLocalDataSourceImpl(get())
    }

    // ============================================================================
    // REPOSITORIES
    // ============================================================================

    // Invoice/expense repository with cache-first pattern
    singleOf(::CashflowDataRepository)

    // Chat repository for document Q&A sessions
    singleOf(::ChatRepositoryImpl)

    // ============================================================================
    // USE CASES
    // ============================================================================

    // Chat messaging use case
    singleOf(::SendChatMessageUseCase)
}
