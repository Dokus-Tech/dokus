package ai.dokus.app.cashflow.di

import ai.dokus.app.cashflow.network.ResilientCashflowRemoteService
import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.config.DokusEndpoint
import ai.dokus.foundation.domain.model.common.Feature
import ai.dokus.foundation.domain.rpc.CashflowRemoteService
import ai.dokus.foundation.network.createAuthenticatedRpcClient
import ai.dokus.foundation.network.service
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Koin DI module for Cashflow feature network configuration.
 *
 * This module sets up authenticated RPC communication with the Cashflow backend service,
 * including JWT token management and graceful degradation to mock service when offline.
 */
val cashflowNetworkModule = module {
    // Authenticated RPC client for Cashflow service
    // Uses createAuthenticatedRpcClient to automatically include JWT tokens
    factory<KtorRpcClient>(named(Feature.Cashflow)) {
        createAuthenticatedRpcClient(
            endpoint = DokusEndpoint.Cashflow,
            tokenManager = get<TokenManager>(),
            onAuthenticationFailed = {
                val authManager = get<AuthManager>()
                CoroutineScope(Dispatchers.Default).launch {
                    authManager.onAuthenticationFailed()
                }
            },
            waitForServices = false
        )
    }

    // CashflowApi service with stub fallback
    // If RPC client is unavailable (offline/network error), falls back to MockCashflowApi
    single<CashflowRemoteService> {
        // Wrap the service with a resiliency layer that recreates the RPC client
        ResilientCashflowRemoteService(
            serviceProvider = {
                // Resolve a fresh RPC client instance (factory binding)
                val rpcClient = get<KtorRpcClient>(named(Feature.Cashflow))
                rpcClient.service<CashflowRemoteService>()
            }
        )
    }
}
