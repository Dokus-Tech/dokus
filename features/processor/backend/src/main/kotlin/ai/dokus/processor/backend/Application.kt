package ai.dokus.processor.backend

import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.configure.configureErrorHandling
import ai.dokus.foundation.ktor.configure.configureMonitoring
import ai.dokus.foundation.ktor.configure.configureSerialization
import ai.dokus.processor.backend.config.configureDependencyInjection
import ai.dokus.processor.backend.plugins.configureDatabase
import ai.dokus.processor.backend.plugins.configureGracefulDatabaseShutdown
import ai.dokus.processor.backend.worker.DocumentProcessingWorker
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ProcessorApplication")

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
        logger.info("Shutting down Processor Service gracefully...")
        server.stop(5000, 10000)
        logger.info("Server shutdown complete")
    })

    server.start(wait = true)
}

fun Application.module(appConfig: AppBaseConfig) {
    logger.info("Starting Dokus Document Processor Service...")
    logger.info("Environment: ${appConfig.ktor.deployment.environment}")
    logger.info("Port: ${appConfig.ktor.deployment.port}")

    // Core configuration
    configureDependencyInjection(appConfig)
    configureDatabase()

    // Ktor plugins
    configureSerialization()
    configureErrorHandling()
    configureMonitoring()

    // Health check endpoint
    configureHealthRoutes()

    // Start the processing worker
    startProcessingWorker()

    // Lifecycle management
    configureGracefulDatabaseShutdown()

    logger.info("Dokus Document Processor Service started successfully on port ${appConfig.ktor.deployment.port}")
}

private fun Application.configureHealthRoutes() {
    routing {
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "healthy"))
        }

        get("/health/ready") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ready"))
        }

        get("/health/live") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "alive"))
        }
    }
}

private fun Application.startProcessingWorker() {
    val worker by inject<DocumentProcessingWorker>()

    // Start worker on application start
    worker.start()

    // Stop worker on application stop
    monitor.subscribe(ApplicationStopped) {
        logger.info("Stopping document processing worker...")
        worker.stop()
    }

    logger.info("Document processing worker started")
}
