package ai.dokus.server.backend.plugins

import ai.dokus.auth.backend.jobs.RateLimitCleanupJob
import ai.dokus.foundation.ktor.cache.RedisClient
import ai.dokus.foundation.ktor.config.AppBaseConfig
import ai.dokus.processor.backend.worker.DocumentProcessingWorker
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.runBlocking
import org.koin.ktor.ext.getKoin
import org.koin.ktor.ext.inject
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("BackgroundWorkers")

fun Application.configureBackgroundWorkers(appConfig: AppBaseConfig) {
    val processingWorker by inject<DocumentProcessingWorker>()
    val rateLimitCleanupJob by inject<RateLimitCleanupJob>()

    monitor.subscribe(ApplicationStarted) {
        rateLimitCleanupJob.start()

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

