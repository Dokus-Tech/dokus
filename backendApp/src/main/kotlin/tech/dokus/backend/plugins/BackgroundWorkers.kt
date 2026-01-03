package tech.dokus.backend.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import tech.dokus.backend.worker.DocumentProcessingWorker
import tech.dokus.backend.worker.RateLimitCleanupWorker
import tech.dokus.foundation.backend.cache.RedisClient
import tech.dokus.foundation.backend.config.AppBaseConfig
import tech.dokus.foundation.backend.utils.loggerFor

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
