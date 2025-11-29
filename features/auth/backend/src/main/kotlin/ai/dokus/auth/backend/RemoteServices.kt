package ai.dokus.auth.backend

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.app.auth.domain.IdentityRemoteService
import ai.dokus.app.auth.domain.TenantRemoteService
import ai.dokus.auth.backend.database.repository.TenantRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.auth.backend.rpc.AccountRemoteServiceImpl
import ai.dokus.auth.backend.rpc.ClientRemoteServiceImpl
import ai.dokus.auth.backend.rpc.IdentityRemoteServiceImpl
import ai.dokus.auth.backend.rpc.TenantRemoteServiceImpl
import ai.dokus.auth.backend.services.AuthService
import ai.dokus.foundation.domain.rpc.AuthValidationRemoteService
import ai.dokus.foundation.domain.rpc.ClientRemoteService
import ai.dokus.foundation.ktor.security.AuthInfoProvider
import ai.dokus.foundation.ktor.security.JwtValidator
import ai.dokus.foundation.ktor.services.ClientService
import io.ktor.server.routing.*
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("RemoteServices")

/**
 * Extension function to configure authenticated RPC routes.
 * Wraps services with authentication context injection.
 */
fun Route.withRemoteServices() {
    val jwtValidator by inject<JwtValidator>()

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
                authInfoProvider = AuthInfoProvider(call, jwtValidator)
            )
        }

        // IdentityRemoteService (no authentication required)
        registerService<IdentityRemoteService> {
            IdentityRemoteServiceImpl(
                authService = get<AuthService>()
            )
        }

        registerService<TenantRemoteService> {
            TenantRemoteServiceImpl(
                tenantRepository = get<TenantRepository>(),
                userRepository = get<UserRepository>(),
                authInfoProvider = AuthInfoProvider(call, jwtValidator)
            )
        }
        registerService<ClientRemoteService> {
            ClientRemoteServiceImpl(
                clientService = get<ClientService>(),
                authInfoProvider = AuthInfoProvider(call, jwtValidator)
            )
        }

        // Auth validation service for inter-service communication
        registerService<AuthValidationRemoteService> { get<AuthValidationRemoteService>() }
    }

    logger.info("RPC APIs registered at /rpc")
}