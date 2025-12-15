package ai.dokus.auth.backend.jobs

import ai.dokus.auth.backend.services.RateLimitServiceInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
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
class RateLimitCleanupJob(
    private val rateLimitService: RateLimitServiceInterface
) {
    private val logger = LoggerFactory.getLogger(RateLimitCleanupJob::class.java)
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
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
