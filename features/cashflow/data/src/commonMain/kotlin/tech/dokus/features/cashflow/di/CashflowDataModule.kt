package tech.dokus.features.cashflow.di

import tech.dokus.features.cashflow.cache.CashflowCacheDatabase
import tech.dokus.features.cashflow.cache.CashflowDb
import tech.dokus.features.cashflow.cache.InvoiceLocalDataSource
import tech.dokus.features.cashflow.cache.InvoiceLocalDataSourceImpl
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSourceImpl
import tech.dokus.features.cashflow.datasource.ChatRemoteDataSource
import tech.dokus.features.cashflow.datasource.ChatRemoteDataSourceImpl
import tech.dokus.features.cashflow.repository.CashflowDataRepository
import tech.dokus.features.cashflow.repository.ChatRepositoryImpl
import tech.dokus.features.cashflow.presentation.cashflow.model.usecase.SendChatMessageUseCase
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
