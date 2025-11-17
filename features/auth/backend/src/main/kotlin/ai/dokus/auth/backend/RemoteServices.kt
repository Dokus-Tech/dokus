package ai.dokus.auth.backend

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.auth.backend.rpc.AuthenticatedAccountService
import ai.dokus.foundation.domain.rpc.CashflowApi
import ai.dokus.foundation.domain.rpc.ClientApi
import ai.dokus.foundation.domain.rpc.TenantApi
import ai.dokus.foundation.ktor.security.RequestAuthHolder
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
            AuthenticatedAccountService(
                delegate = get<AccountRemoteService>(),
                authInfoProvider = { RequestAuthHolder.get() }
            )
        }

        registerService<TenantApi> { get<TenantApi>() }
        registerService<ClientApi> { get<ClientApi>() }
        registerService<CashflowApi> { get<CashflowApi>() }
    }

    logger.info("Public RPC APIs registered at /api with JWT authentication")
}