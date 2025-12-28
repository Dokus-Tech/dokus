package tech.dokus.backend.plugins

import tech.dokus.foundation.ktor.cache.RedisClient
import tech.dokus.foundation.ktor.config.AppBaseConfig
import tech.dokus.foundation.ktor.utils.loggerFor
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
        logger.info("Starting document processing worker")
        processingWorker.start()
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
