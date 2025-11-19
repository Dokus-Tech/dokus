package ai.dokus.auth.backend

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.auth.backend.database.repository.TenantRepository
import ai.dokus.auth.backend.rpc.AccountRemoteServiceImpl
import ai.dokus.auth.backend.rpc.ClientRemoteServiceImpl
import ai.dokus.auth.backend.rpc.TenantRemoteServiceImpl
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.foundation.domain.rpc.AuthValidationRemoteService
import ai.dokus.foundation.domain.rpc.CashflowRemoteService
import ai.dokus.foundation.domain.rpc.ClientRemoteService
import ai.dokus.foundation.domain.rpc.TenantRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.services.ClientService
import io.ktor.server.routing.Route
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RemoteServices")

/**
 * Extension function to configure authenticated RPC routes.
 * Wraps services with authentication context injection.
 */
fun Route.withRemoteServices() {
    rpc("/rpc") {
        rpcConfig {
            serialization {
                json()
            }
        }

        // Wrap AccountRemoteService with authentication context injection
        registerService<AccountRemoteService> {
            AccountRemoteServiceImpl(
                authService = get<AuthService>(),
                authInfoProvider = AuthInfoProvider(call)
            )
        }

        registerService<TenantRemoteService> {
            TenantRemoteServiceImpl(
                tenantService = get<TenantRepository>(),
                authInfoProvider = AuthInfoProvider(call)
            )
        }
        registerService<ClientRemoteService> {
            ClientRemoteServiceImpl(
                clientService = get<ClientService>(),
                authInfoProvider = AuthInfoProvider(call)
            )
        }
        registerService<CashflowRemoteService> { get<CashflowRemoteService>() }

        // Auth validation service for inter-service communication
        registerService<AuthValidationRemoteService> { get<AuthValidationRemoteService>() }
    }

    logger.info("RPC APIs registered at /rpc")
}