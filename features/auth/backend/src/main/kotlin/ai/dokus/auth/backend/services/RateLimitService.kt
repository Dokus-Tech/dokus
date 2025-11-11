@file:OptIn(kotlin.time.ExperimentalTime::class)

package ai.dokus.auth.backend.services

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ktor.database.now
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.minutes

/**
 * Tracks login attempts for a specific email address.
 *
 * @property attempts Number of failed login attempts
 * @property lockUntil Timestamp when the account will be unlocked (null if not locked)
 * @property firstAttemptAt Timestamp of the first failed attempt in the current window
 */
data class LoginAttemptTracker(
    var attempts: Int = 0,
    var lockUntil: Instant? = null,
    var firstAttemptAt: Instant = now()
)

/**
 * Service for rate limiting login attempts to prevent brute force attacks.
 *
 * Features:
 * - Tracks failed login attempts per email address
 * - Locks accounts after MAX_ATTEMPTS (5) failed attempts
 * - Automatic unlock after LOCKOUT_DURATION (15 minutes)
 * - Attempt window of ATTEMPT_WINDOW (15 minutes)
 * - Thread-safe with mutex for concurrent access
 * - In-memory storage (can be upgraded to Redis for multi-instance deployments)
 *
 * Security considerations:
 * - Email addresses are normalized to lowercase for consistent tracking
 * - Expired entries are automatically cleaned up to prevent memory leaks
 * - Lockout duration increases security without permanent account lockout
 */
class RateLimitService {
    private val logger = LoggerFactory.getLogger(RateLimitService::class.java)
    private val loginAttempts = mutableMapOf<String, LoginAttemptTracker>()
    private val mutex = Mutex()

    companion object {
        /** Maximum number of failed login attempts before account lockout */
        private const val MAX_ATTEMPTS = 5

        /** Time window for counting failed attempts (15 minutes) */
        private val ATTEMPT_WINDOW = 15.minutes

        /** Duration of account lockout after max attempts exceeded (15 minutes) */
        private val LOCKOUT_DURATION = 15.minutes
    }

    /**
     * Checks if a login attempt is allowed for the given email address.
     *
     * This method should be called BEFORE attempting to verify credentials.
     *
     * @param email Email address attempting to log in
     * @return Result.success if login attempt is allowed, Result.failure with TooManyLoginAttempts if blocked
     */
    suspend fun checkLoginAttempts(email: String): Result<Unit> = mutex.withLock {
        val normalizedEmail = email.lowercase()
        val tracker = loginAttempts[normalizedEmail]

        if (tracker != null) {
            val currentTime = now()

            // Check if account is currently locked
            val lockUntil = tracker.lockUntil
            if (lockUntil != null && lockUntil > currentTime) {
                val remainingSeconds = (lockUntil - currentTime).inWholeSeconds
                logger.warn("Login attempt blocked for $normalizedEmail - locked for $remainingSeconds seconds")
                return@withLock Result.failure(
                    DokusException.TooManyLoginAttempts(
                        message = "Too many login attempts. Please try again in $remainingSeconds seconds.",
                        retryAfterSeconds = remainingSeconds.toInt()
                    )
                )
            }

            // Check if attempt window has expired
            val windowExpiry = tracker.firstAttemptAt + ATTEMPT_WINDOW
            if (currentTime > windowExpiry) {
                // Window expired, reset tracker
                loginAttempts.remove(normalizedEmail)
            } else if (tracker.attempts >= MAX_ATTEMPTS) {
                // Too many attempts within window - lock the account
                tracker.lockUntil = currentTime + LOCKOUT_DURATION
                logger.warn("Account locked for $normalizedEmail - too many attempts")
                return@withLock Result.failure(
                    DokusException.TooManyLoginAttempts(
                        message = "Too many login attempts. Please try again in ${LOCKOUT_DURATION.inWholeMinutes} minutes.",
                        retryAfterSeconds = LOCKOUT_DURATION.inWholeSeconds.toInt()
                    )
                )
            }
        }

        Result.success(Unit)
    }

    /**
     * Records a failed login attempt for the given email address.
     *
     * This method should be called AFTER a failed credential verification.
     * If the max attempts threshold is reached, the account will be locked.
     *
     * @param email Email address that failed to log in
     */
    suspend fun recordFailedLogin(email: String) = mutex.withLock {
        val normalizedEmail = email.lowercase()
        val tracker = loginAttempts.getOrPut(normalizedEmail) {
            LoginAttemptTracker()
        }

        tracker.attempts++
        logger.debug("Failed login attempt ${tracker.attempts}/$MAX_ATTEMPTS for $normalizedEmail")

        if (tracker.attempts >= MAX_ATTEMPTS) {
            tracker.lockUntil = now() + LOCKOUT_DURATION
            logger.warn("Account locked for $normalizedEmail after $MAX_ATTEMPTS failed attempts")
        }
    }

    /**
     * Resets the login attempt counter for the given email address.
     *
     * This method should be called AFTER a successful login to clear any failed attempts.
     *
     * @param email Email address to reset
     */
    suspend fun resetLoginAttempts(email: String) = mutex.withLock {
        val normalizedEmail = email.lowercase()
        loginAttempts.remove(normalizedEmail)
        logger.debug("Login attempts reset for $normalizedEmail")
    }

    /**
     * Removes expired entries from the in-memory cache.
     *
     * This method should be called periodically (e.g., every hour) to prevent memory leaks.
     * Entries are removed if:
     * - The attempt window has expired AND the account is not locked
     * - The lockout period has expired
     */
    suspend fun cleanupExpiredEntries() = mutex.withLock {
        val currentTime = now()
        val toRemove = loginAttempts.filter { (_, tracker) ->
            val windowExpiry = tracker.firstAttemptAt + ATTEMPT_WINDOW
            val lockExpiry = tracker.lockUntil

            // Remove if window expired and not locked, or lock expired
            (currentTime > windowExpiry && lockExpiry == null) ||
            (lockExpiry != null && currentTime > lockExpiry)
        }.keys

        toRemove.forEach { loginAttempts.remove(it) }
        if (toRemove.isNotEmpty()) {
            logger.debug("Cleaned up ${toRemove.size} expired rate limit entries")
        }
    }

    /**
     * Gets the current attempt count for the given email address.
     * Useful for debugging and monitoring.
     *
     * @param email Email address to check
     * @return Number of failed attempts (0 if no attempts recorded)
     */
    suspend fun getAttemptCount(email: String): Int = mutex.withLock {
        loginAttempts[email.lowercase()]?.attempts ?: 0
    }

    /**
     * Checks if the given email address is currently locked out.
     * Useful for debugging and monitoring.
     *
     * @param email Email address to check
     * @return true if the account is locked, false otherwise
     */
    suspend fun isLocked(email: String): Boolean = mutex.withLock {
        val tracker = loginAttempts[email.lowercase()] ?: return@withLock false
        val lockUntil = tracker.lockUntil ?: return@withLock false
        lockUntil > now()
    }
}
