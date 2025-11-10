package ai.dokus.auth.backend

import ai.dokus.app.auth.domain.AccountRemoteService
import ai.dokus.auth.backend.config.configureAuthentication
import ai.dokus.auth.backend.config.configureDependencyInjection
import ai.dokus.auth.backend.jobs.RateLimitCleanupJob
import ai.dokus.auth.backend.routes.identityRoutes
import ai.dokus.auth.backend.routes.passwordlessAuthRoutes
import ai.dokus.auth.backend.routes.userRoutes
import ai.dokus.auth.backend.rpc.AuthenticatedAccountService
import ai.dokus.auth.backend.security.AuthContextElement
import ai.dokus.auth.backend.security.JwtValidator
import ai.dokus.auth.backend.security.RequestAuthHolder
import ai.dokus.foundation.domain.rpc.*
import ai.dokus.foundation.ktor.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.foundation.ktor.routes.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.header
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import kotlinx.coroutines.withContext
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

/**
 * Plugin that extracts JWT authentication and stores it in RequestAuthHolder for RPC access.
 */
private val RpcAuthPlugin = createApplicationPlugin(name = "RpcAuthPlugin") {
    val logger = LoggerFactory.getLogger("RpcAuthPlugin")

    onCall { call ->
        try {
            val jwtValidator = call.application.environment.config.propertyOrNull("jwt.secret")?.let {
                // Get from Koin
                call.application.attributes.getOrNull(io.ktor.util.AttributeKey<JwtValidator>("JwtValidator"))
            }

            if (jwtValidator != null) {
                val authHeader = call.request.header("Authorization")
                if (authHeader != null && authHeader.startsWith("Bearer ", ignoreCase = true)) {
                    val token = authHeader.substring(7)
                    val authInfo = jwtValidator.validateAndExtract(token)
                    if (authInfo != null) {
                        logger.debug("Setting auth context for user: ${authInfo.userId.value}")
                        RequestAuthHolder.set(authInfo)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Error extracting auth info", e)
        }
    }

    onCallRespond { call, _ ->
        // Clear auth context after request
        RequestAuthHolder.clear()
    }
}

/**
 * Extension function to configure authenticated RPC routes.
 * Wraps services with authentication context injection.
 */
private fun Route.configureAuthenticatedRpc() {
    // Register RPC services
    rpc("/api") {
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
        registerService<InvoiceApi> { get<InvoiceApi>() }
        registerService<ExpenseApi> { get<ExpenseApi>() }
    }

    logger.info("Public RPC APIs registered at /api with JWT authentication")
}

fun main() {
    val appConfig = AppBaseConfig.load()

    logger.info("Loaded configuration: ${appConfig.ktor.deployment.environment}")


    val server = embeddedServer(
        Netty,
        port = appConfig.ktor.deployment.port,
        host = appConfig.ktor.deployment.host,
    ) {
        module(appConfig)
    }

    // Setup shutdown hook for graceful shutdown
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down server gracefully...")
        server.stop(5000, 10000)
        logger.info("Server shutdown complete")
    })

    server.start(wait = true)
}

fun Application.module(appConfig: AppBaseConfig) {
    // Log application startup
    logger.info("Starting Dokus Auth Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")

    // Configure application
    configureDependencyInjection(appConfig)
    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureAuthentication(appConfig)
    configureMonitoring()

    // Install KotlinX RPC plugin
    install(Krpc)

    // Install RPC authentication plugin
    val jwtValidator = get<JwtValidator>()
    attributes.put(io.ktor.util.AttributeKey("JwtValidator"), jwtValidator)
    install(RpcAuthPlugin)

    // Start background jobs
    val rateLimitCleanupJob = get<RateLimitCleanupJob>()
    rateLimitCleanupJob.start()
    logger.info("Started rate limit cleanup job")

    // Configure routes
    routing {
        healthRoutes()
        identityRoutes()
        userRoutes()
        passwordlessAuthRoutes()

        // Register public RPC APIs with authentication support
        configureAuthenticatedRpc()
    }

    // Configure graceful shutdown
    monitor.subscribe(ApplicationStopping) {
        logger.info("Application stopping, cleaning up resources...")
        logger.info("Cleanup complete")
    }

    logger.info("Dokus Auth Service started successfully")
}