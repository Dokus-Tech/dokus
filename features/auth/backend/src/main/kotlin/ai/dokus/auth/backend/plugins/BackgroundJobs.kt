package ai.dokus.auth.backend.plugins

import ai.dokus.auth.backend.jobs.RateLimitCleanupJob
import io.ktor.server.application.Application
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("BackgroundJobs")

/**
 * Configures and starts background jobs.
 * Currently starts the rate limit cleanup job.
 */
fun Application.configureBackgroundJobs() {
    logger.info("Starting background jobs...")

    val rateLimitCleanupJob = get<RateLimitCleanupJob>()
    rateLimitCleanupJob.start()

    logger.info("Rate limit cleanup job started")
}
