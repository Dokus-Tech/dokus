package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSourceImpl
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin DI module for Cashflow feature network configuration.
 *
 * This module sets up authenticated HTTP communication with the Cashflow backend service,
 * including JWT token management.
 */
val cashflowNetworkModule = module {
    singleOf(::CashflowRemoteDataSourceImpl) bind CashflowRemoteDataSource::class
}
