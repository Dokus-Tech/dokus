@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.foundation.backend.security

import tech.dokus.domain.ids.UserId
import tech.dokus.foundation.backend.cache.RedisClient
import tech.dokus.foundation.backend.utils.loggerFor
import java.time.Duration
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi

/**
 * Service for managing JWT access token blacklist.
 * Used to invalidate tokens before their natural expiration.
 */
interface TokenBlacklistService {
    /**
     * Add a token to the blacklist.
     * @param jti The JWT ID to blacklist
     * @param expiresAt When the token would naturally expire (used for TTL)
     */
    suspend fun blacklistToken(jti: String, expiresAt: Instant)

    /**
     * Check if a token is blacklisted.
     * @param jti The JWT ID to check
     * @return true if the token is blacklisted
     */
    suspend fun isBlacklisted(jti: String): Boolean

    /**
     * Blacklist all active tokens for a user.
     * Used for security events like password change or account compromise.
     * @param userId The user whose tokens should be blacklisted
     */
    suspend fun blacklistAllUserTokens(userId: UserId)

    /**
     * Track a token JTI for a user.
     * Called when generating new tokens to enable blacklistAllUserTokens.
     * @param userId The user who owns the token
     * @param jti The JWT ID
     * @param expiresAt When the token expires
     */
    suspend fun trackUserToken(userId: UserId, jti: String, expiresAt: Instant)
}

/**
 * Redis-backed token blacklist implementation.
 * Provides distributed blacklist that works across multiple server instances.
 */
class RedisTokenBlacklistService(
    private val redisClient: RedisClient
) : TokenBlacklistService {
    private val logger = loggerFor()

    override suspend fun blacklistToken(jti: String, expiresAt: Instant) {
        val ttl = calculateTtl(expiresAt)
        if (ttl.isNegative || ttl.isZero) {
            logger.debug("Token $jti already expired, skipping blacklist")
            return
        }

        try {
            redisClient.set(blacklistKey(jti), "1", ttl)
            logger.debug("Blacklisted token $jti with TTL ${ttl.seconds}s")
        } catch (e: Exception) {
            logger.error("Failed to blacklist token $jti: ${e.message}", e)
            throw e
        }
    }

    override suspend fun isBlacklisted(jti: String): Boolean {
        return try {
            redisClient.exists(blacklistKey(jti))
        } catch (e: Exception) {
            logger.error("Failed to check blacklist for token $jti: ${e.message}", e)
            // Fail open - if Redis is unavailable, don't block valid requests
            // Consider fail-closed for high-security applications
            false
        }
    }

    override suspend fun blacklistAllUserTokens(userId: UserId) {
        try {
            val userTokensKey = userTokensKey(userId)
            val jtis = redisClient.keys("$userTokensKey:*")
                .map { it.substringAfterLast(":") }

            if (jtis.isEmpty()) {
                logger.debug("No active tokens found for user {}", userId.value)
                return
            }

            // Blacklist each JTI with remaining TTL
            for (jti in jtis) {
                val ttlKey = "$userTokensKey:$jti"
                val remainingTtl = redisClient.ttl(ttlKey)
                if (remainingTtl > 0) {
                    redisClient.set(blacklistKey(jti), "1", Duration.ofSeconds(remainingTtl))
                }
            }

            // Clean up user token tracking
            val keysToDelete = jtis.map { "$userTokensKey:$it" }.toTypedArray()
            if (keysToDelete.isNotEmpty()) {
                redisClient.deleteMany(*keysToDelete)
            }

            logger.info("Blacklisted ${jtis.size} tokens for user ${userId.value}")
        } catch (e: Exception) {
            logger.error("Failed to blacklist all tokens for user ${userId.value}: ${e.message}", e)
            throw e
        }
    }

    override suspend fun trackUserToken(userId: UserId, jti: String, expiresAt: Instant) {
        val ttl = calculateTtl(expiresAt)
        if (ttl.isNegative || ttl.isZero) {
            return
        }

        try {
            val key = "${userTokensKey(userId)}:$jti"
            redisClient.set(key, "1", ttl)
            logger.debug("Tracking token {} for user {}", jti, userId.value)
        } catch (e: Exception) {
            logger.error("Failed to track token for user ${userId.value}: ${e.message}", e)
            // Non-critical - token tracking is best-effort
        }
    }

    private fun blacklistKey(jti: String) = "blacklist:$jti"
    private fun userTokensKey(userId: UserId) = "user:${userId.value}:tokens"

    private fun calculateTtl(expiresAt: Instant): Duration {
        val now = Instant.now()
        return Duration.between(now, expiresAt)
    }
}
