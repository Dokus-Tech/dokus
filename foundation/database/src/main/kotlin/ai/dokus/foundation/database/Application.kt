package ai.dokus.foundation.database

import ai.dokus.foundation.database.configuration.configureDependencyInjection
import ai.dokus.foundation.database.tables.*
import ai.dokus.foundation.database.utils.DatabaseFactory
import ai.dokus.foundation.ktor.AppConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSecurity
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.foundation.ktor.routes.healthRoutes
import ai.dokus.foundation.ktor.services.*
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import kotlinx.coroutines.runBlocking
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    val appConfig = AppConfig.load()

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

fun Application.module(appConfig: AppConfig) {
    // Log application startup
    logger.info("Starting Dokus Database Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")
    logger.info("Financial Management System - Multi-tenant SaaS")

    // Configure application
    configureDependencyInjection(appConfig)
    configureSerialization()
    configureErrorHandling()
    configureSecurity(appConfig.security)
    configureMonitoring()

    // Initialize database with all financial tables
    runBlocking {
        val dbFactory = get<DatabaseFactory>()
        logger.info("Initializing database schema...")

        dbFactory.init(
            // Authentication & Multi-tenancy
            TenantsTable,
            UsersTable,
            RefreshTokensTable,

            // Business Entities
            ClientsTable,
            InvoicesTable,
            InvoiceItemsTable,
            ExpensesTable,
            PaymentsTable,

            // Bank Integration
            BankConnectionsTable,
            BankTransactionsTable,

            // Tax & Compliance
            VatReturnsTable,
            AuditLogsTable,

            // Configuration
            TenantSettingsTable,
            AttachmentsTable
        )

        logger.info("Database schema initialized successfully")
    }

    // Install KotlinX RPC plugin
    install(Krpc)

    // Configure routes
    routing {
        healthRoutes()

        // Register RPC services
        rpc("/api/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }

            // Register all RPC service implementations
            registerService<TenantService> { get<TenantService>() }
            registerService<UserService> { get<UserService>() }
            registerService<ClientService> { get<ClientService>() }
            registerService<InvoiceService> { get<InvoiceService>() }
            registerService<ExpenseService> { get<ExpenseService>() }
            registerService<PaymentService> { get<PaymentService>() }
        }

        logger.info("RPC services registered at /api/rpc")
    }

    // Configure graceful shutdown
    monitor.subscribe(ApplicationStopping) {
        logger.info("Application stopping, cleaning up resources...")
        runBlocking {
            val dbFactory = get<DatabaseFactory>()
            // Close database connections
            dbFactory.close()
        }
        logger.info("Cleanup complete")
    }

    logger.info("Dokus Database Service started successfully")
    logger.info("Financial precision: NUMERIC(12,2) for all monetary values")
    logger.info("Multi-tenant isolation: All queries filtered by tenant_id")
}