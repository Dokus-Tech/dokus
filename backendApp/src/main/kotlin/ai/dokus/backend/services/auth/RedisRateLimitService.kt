package ai.dokus.backend.services.auth

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ktor.cache.RedisClient
import ai.dokus.foundation.ktor.database.now
import org.slf4j.LoggerFactory
import java.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Redis-backed rate limiting for multi-instance deployments.
 *
 * Uses Redis sorted sets for sliding window rate limiting:
 * - Each login attempt is stored with a timestamp as score
 * - Old attempts are automatically removed based on the window
 * - Lockout state is stored with TTL for automatic expiration
 *
 * This implementation is distributed and works across multiple server instances.
 *
 * @param redisClient Redis client for storage
 * @param maxAttempts Maximum failed attempts before lockout (default: 5)
 * @param attemptWindowMinutes Time window for counting attempts (default: 15)
 * @param lockoutDurationMinutes Duration of lockout (default: 15)
 */
class RedisRateLimitService(
    private val redisClient: RedisClient,
    private val maxAttempts: Int = 5,
    private val attemptWindowMinutes: Long = 15,
    private val lockoutDurationMinutes: Long = 15
) : RateLimitServiceInterface {

    private val logger = LoggerFactory.getLogger(RedisRateLimitService::class.java)
    private val attemptWindow = attemptWindowMinutes.minutes
    private val lockoutDuration = lockoutDurationMinutes.minutes

    override suspend fun checkLoginAttempts(email: String): Result<Unit> {
        val normalizedEmail = email.lowercase()

        try {
            // Check if currently locked
            val lockKey = lockoutKey(normalizedEmail)
            val lockTtl = redisClient.ttl(lockKey)

            if (lockTtl > 0) {
                logger.warn("Login attempt blocked for $normalizedEmail - locked for $lockTtl seconds")
                return Result.failure(
                    DokusException.TooManyLoginAttempts(
                        message = "Too many login attempts. Please try again in $lockTtl seconds.",
                        retryAfterSeconds = lockTtl.toInt()
                    )
                )
            }

            // Count attempts in current window
            val attemptCount = getAttemptCountInternal(normalizedEmail)

            if (attemptCount >= maxAttempts) {
                // Lock the account
                redisClient.set(
                    lockKey,
                    "locked",
                    Duration.ofMinutes(lockoutDurationMinutes)
                )
                logger.warn("Account locked for $normalizedEmail - too many attempts ($attemptCount)")
                return Result.failure(
                    DokusException.TooManyLoginAttempts(
                        message = "Too many login attempts. Please try again in ${lockoutDuration.inWholeMinutes} minutes.",
                        retryAfterSeconds = lockoutDuration.inWholeSeconds.toInt()
                    )
                )
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Error checking rate limit for $normalizedEmail: ${e.message}", e)
            // Fail open - allow the request if Redis is unavailable
            return Result.success(Unit)
        }
    }

    override suspend fun recordFailedLogin(email: String) {
        val normalizedEmail = email.lowercase()

        try {
            val attemptsKey = attemptsKey(normalizedEmail)
            val currentTime = now().toEpochMilliseconds()
            val windowStart = currentTime - attemptWindow.inWholeMilliseconds

            // Add current attempt with timestamp
            // Using increment as a simple counter since we don't have ZADD
            // We'll use a different approach: store individual attempt keys
            val attemptKey = "$attemptsKey:$currentTime"
            redisClient.set(
                attemptKey,
                "1",
                Duration.ofMinutes(attemptWindowMinutes)
            )

            val attemptCount = getAttemptCountInternal(normalizedEmail)
            logger.debug("Failed login attempt $attemptCount/$maxAttempts for $normalizedEmail")

            if (attemptCount >= maxAttempts) {
                val lockKey = lockoutKey(normalizedEmail)
                redisClient.set(
                    lockKey,
                    "locked",
                    Duration.ofMinutes(lockoutDurationMinutes)
                )
                logger.warn("Account locked for $normalizedEmail after $maxAttempts failed attempts")
            }
        } catch (e: Exception) {
            logger.error("Error recording failed login for $normalizedEmail: ${e.message}", e)
            // Non-critical - continue even if Redis fails
        }
    }

    override suspend fun resetLoginAttempts(email: String) {
        val normalizedEmail = email.lowercase()

        try {
            // Delete all attempt keys and lockout key
            val attemptsPattern = "${attemptsKey(normalizedEmail)}:*"
            val attemptKeys = redisClient.keys(attemptsPattern)

            if (attemptKeys.isNotEmpty()) {
                redisClient.deleteMany(*attemptKeys.toTypedArray())
            }

            redisClient.delete(lockoutKey(normalizedEmail))
            logger.debug("Login attempts reset for $normalizedEmail")
        } catch (e: Exception) {
            logger.error("Error resetting login attempts for $normalizedEmail: ${e.message}", e)
        }
    }

    override suspend fun cleanupExpiredEntries() {
        // Redis handles TTL automatically, so no explicit cleanup needed
        logger.debug("Redis handles TTL-based cleanup automatically")
    }

    override suspend fun getAttemptCount(email: String): Int {
        return try {
            getAttemptCountInternal(email.lowercase())
        } catch (e: Exception) {
            logger.error("Error getting attempt count: ${e.message}", e)
            0
        }
    }

    override suspend fun isLocked(email: String): Boolean {
        return try {
            redisClient.ttl(lockoutKey(email.lowercase())) > 0
        } catch (e: Exception) {
            logger.error("Error checking lock status: ${e.message}", e)
            false
        }
    }

    private suspend fun getAttemptCountInternal(normalizedEmail: String): Int {
        val attemptsPattern = "${attemptsKey(normalizedEmail)}:*"
        return redisClient.keys(attemptsPattern).size
    }

    private fun attemptsKey(email: String) = "ratelimit:attempts:$email"
    private fun lockoutKey(email: String) = "ratelimit:lockout:$email"
}
