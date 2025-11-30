package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSourceImpl
import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.network.createAuthenticatedHttpClient
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin DI module for Cashflow feature network configuration.
 *
 * This module sets up authenticated HTTP communication with the Cashflow backend service,
 * including JWT token management.
 */
val cashflowNetworkModule = module {
    // Authenticated HTTP client for Cashflow service
    single<HttpClient>(named("cashflow_http_client")) {
        createAuthenticatedHttpClient(
            dokusEndpoint = DokusEndpoint.Cashflow,
            tokenManager = get<TokenManager>(),
            onAuthenticationFailed = {
                val authManager = get<AuthManager>()
                CoroutineScope(Dispatchers.Default).launch {
                    authManager.onAuthenticationFailed()
                }
            }
        )
    }

    // CashflowRemoteDataSource using HTTP client
    single<CashflowRemoteDataSource> {
        CashflowRemoteDataSourceImpl(
            httpClient = get<HttpClient>(named("cashflow_http_client"))
        )
    }
}
