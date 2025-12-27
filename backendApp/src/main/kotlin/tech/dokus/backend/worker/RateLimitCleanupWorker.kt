package tech.dokus.backend.worker

import tech.dokus.foundation.ktor.utils.loggerFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import tech.dokus.backend.services.auth.RateLimitServiceInterface
import kotlin.time.Duration.Companion.hours

/**
 * Background job that periodically cleans up expired rate limit entries.
 *
 * This job runs every hour to remove entries that have:
 * - Expired attempt windows with no active lockout
 * - Expired lockout periods
 *
 * This prevents memory leaks in the in-memory rate limit cache and ensures
 * accurate tracking of login attempts.
 */
class RateLimitCleanupWorker(
    private val rateLimitService: RateLimitServiceInterface
) {
    private val logger = loggerFor()
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object Companion {
        /** Cleanup interval (1 hour) */
        private val CLEANUP_INTERVAL = 1.hours
    }

    /**
     * Starts the background cleanup job.
     * Runs in a coroutine and continues until the application shuts down.
     */
    fun start() {
        logger.info("Starting rate limit cleanup job - running every ${CLEANUP_INTERVAL.inWholeMinutes} minutes")

        scope.launch {
            while (isActive) {
                try {
                    delay(CLEANUP_INTERVAL)
                    logger.debug("Running rate limit cleanup job")
                    rateLimitService.cleanupExpiredEntries()
                } catch (e: Exception) {
                    logger.error("Rate limit cleanup job failed", e)
                }
            }
        }
    }
}