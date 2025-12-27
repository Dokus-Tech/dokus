package tech.dokus.backend.plugins

import ai.dokus.foundation.ktor.cache.RedisClient
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.foundation.ktor.utils.loggerFor
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import tech.dokus.backend.worker.DocumentProcessingWorker
import tech.dokus.backend.worker.RateLimitCleanupWorker

private val logger = loggerFor("BackgroundWorkers")

fun Application.configureBackgroundWorkers(appConfig: AppBaseConfig) {
    val processingWorker by inject<DocumentProcessingWorker>()
    val rateLimitCleanupWorker by inject<RateLimitCleanupWorker>()

    monitor.subscribe(ApplicationStarted) {
        rateLimitCleanupWorker.start()

        val processorEnabled = runCatching {
            appConfig.config.getConfig("processor").getBoolean("enabled")
        }.getOrDefault(true)

        if (processorEnabled) {
            logger.info("Starting processor worker (enabled=true)")
            processingWorker.start()
        } else {
            logger.info("Processor worker disabled (enabled=false)")
        }
    }

    monitor.subscribe(ApplicationStopping) {
        processingWorker.stop()

        // Close optional Redis connection if present.
        val redisClient = runCatching { getKoin().getOrNull<RedisClient>() }.getOrNull()
        if (redisClient != null) {
            runBlocking { redisClient.close() }
        }
    }
}
