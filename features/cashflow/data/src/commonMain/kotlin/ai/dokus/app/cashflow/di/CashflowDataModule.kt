package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.cache.CashflowCacheDatabase
import ai.dokus.app.cashflow.cache.CashflowDb
import ai.dokus.app.cashflow.cache.InvoiceLocalDataSource
import ai.dokus.app.cashflow.cache.InvoiceLocalDataSourceImpl
import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSourceImpl
import ai.dokus.app.cashflow.repository.CashflowDataRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin DI module for Cashflow feature network configuration.
 *
 * This module sets up authenticated HTTP communication with the Cashflow backend service,
 * including JWT token management and local caching for offline support.
 */
val cashflowNetworkModule = module {
    // Remote data source
    singleOf(::CashflowRemoteDataSourceImpl) bind CashflowRemoteDataSource::class

    // Database wrapper for cache
    single { CashflowDb.create() }
    single<CashflowCacheDatabase> { get<CashflowDb>().get() }

    // Local data source for caching
    single<InvoiceLocalDataSource> {
        InvoiceLocalDataSourceImpl(get())
    }

    // Repository with cache-first pattern
    singleOf(::CashflowDataRepository)
}
