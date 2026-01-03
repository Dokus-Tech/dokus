@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.database.repository.auth

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.auth.RefreshTokensTable
import tech.dokus.database.utils.toKotlinxInstant
import tech.dokus.domain.ids.UserId
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.database.now
import tech.dokus.foundation.backend.utils.loggerFor
import java.security.MessageDigest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid

/**
 * Information about a refresh token
 *
 * @property tokenId The database ID of the token (for display purposes)
 * @property createdAt When this token was created
 * @property expiresAt When this token will expire
 * @property isRevoked Whether this token has been revoked
 */
data class RefreshTokenInfo(
    val tokenId: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val isRevoked: Boolean
)

/**
 * Repository for managing JWT refresh tokens with persistence, rotation, and revocation.
 *
 * Security features:
 * - Tokens are hashed before logging for security
 * - Expired tokens are automatically rejected
 * - Revoked tokens cannot be reused
 * - Token rotation prevents token replay attacks
 * - All database operations are transactional
 *
 * Implementation details:
 * - Uses Exposed ORM for database operations
 * - Converts between kotlin.uuid.Uuid and java.util.UUID
 * - Uses kotlinx.datetime for timestamp handling
 * - Provides comprehensive error handling and logging
 */
class RefreshTokenRepository {
    private val logger = loggerFor()

    /**
     * Save a refresh token to the database
     *
     * @param userId The user this token belongs to
     * @param token The JWT refresh token string
     * @param expiresAt When this token expires
     * @return Result indicating success or failure
     */
    suspend fun saveRefreshToken(
        userId: UserId,
        token: String,
        expiresAt: Instant
    ): Result<Unit> = runCatching {
        dbQuery {
            val userUuid = userId.uuid.toJavaUuid()
            val tokenHash = tokenHash(token)

            RefreshTokensTable.insert {
                it[RefreshTokensTable.userId] = userUuid
                it[RefreshTokensTable.tokenHash] = tokenHash
                it[RefreshTokensTable.expiresAt] = expiresAt.toLocalDateTime(TimeZone.UTC)
                it[RefreshTokensTable.isRevoked] = false
            }

            logger.debug(
                "Saved refresh token for user: {}, token hash: {}, expires: {}",
                userId.value,
                tokenHash.take(8),
                expiresAt
            )
        }
    }.onFailure { error ->
        logger.error("Failed to save refresh token for user: ${userId.value}", error)
    }

    /**
     * Validate a refresh token and rotate it to a new one
     *
     * This implements token rotation security:
     * 1. Validates the old token (not expired, not revoked)
     * 2. Marks the old token as revoked
     * 3. Returns userId for generating new tokens
     *
     * @param oldToken The current refresh token to validate
     * @return Result containing userId if successful, or error if invalid
     */
    suspend fun validateAndRotate(oldToken: String): Result<UserId> = runCatching {
        dbQuery {
            val oldTokenHash = tokenHash(oldToken)

            // Find the token
            val tokenRow = RefreshTokensTable
                .selectAll()
                .where { RefreshTokensTable.tokenHash eq oldTokenHash }
                .singleOrNull()
                ?: throw IllegalArgumentException("Refresh token not found")

            val tokenId = tokenRow[RefreshTokensTable.id].value
            val userId = tokenRow[RefreshTokensTable.userId].value
            val expiresAt = tokenRow[RefreshTokensTable.expiresAt]
            val isRevoked = tokenRow[RefreshTokensTable.isRevoked]

            // Security checks
            if (isRevoked) {
                logger.warn(
                    "Attempt to use revoked token (ID: $tokenId, hash: ${oldTokenHash.take(8)})"
                )
                throw SecurityException("Refresh token has been revoked")
            }

            val now = now()
            val expiresAtInstant = expiresAt.toKotlinxInstant()

            if (now > expiresAtInstant) {
                logger.warn(
                    "Attempt to use expired token (ID: $tokenId, expired: $expiresAt, hash: ${
                        oldTokenHash.take(
                            8
                        )
                    })"
                )
                throw IllegalArgumentException("Refresh token has expired")
            }

            // Mark old token as revoked (token rotation)
            val updated = RefreshTokensTable.update({ RefreshTokensTable.id eq tokenId }) {
                it[RefreshTokensTable.isRevoked] = true
            }

            if (updated == 0) {
                throw IllegalStateException("Failed to revoke old token during rotation")
            }

            logger.info(
                "Validated and rotated refresh token for user: $userId, token ID: $tokenId"
            )

            // Return userId for generating new tokens
            @OptIn(ExperimentalUuidApi::class)
            UserId(Uuid.parse(userId.toString()).toString())
        }
    }.onFailure { error ->
        when (error) {
            is SecurityException, is IllegalArgumentException -> {
                // Expected validation errors, already logged with details
                logger.debug("Token validation failed: ${error.message}")
            }

            else -> {
                // Unexpected errors
                logger.error("Unexpected error during token validation and rotation", error)
            }
        }
    }

    /**
     * Revoke a specific refresh token
     *
     * Used during logout to invalidate the current session.
     *
     * @param token The refresh token to revoke
     * @return Result indicating success or failure
     */
    suspend fun revokeToken(token: String): Result<Unit> = runCatching {
        dbQuery {
            val tokenHash = tokenHash(token)
            val updated = RefreshTokensTable.update(
                { RefreshTokensTable.tokenHash eq tokenHash }
            ) {
                it[RefreshTokensTable.isRevoked] = true
            }

            if (updated == 0) {
                logger.warn("Attempted to revoke non-existent token (hash: ${tokenHash.take(8)})")
                throw IllegalArgumentException("Refresh token not found")
            }

            logger.info("Revoked refresh token (hash: ${tokenHash.take(8)}, count: $updated)")
        }
    }.onFailure { error ->
        if (error !is IllegalArgumentException) {
            logger.error("Failed to revoke refresh token", error)
        }
    }

    /**
     * Revoke all refresh tokens for a user
     *
     * Used for security purposes (e.g., password reset, account compromise).
     *
     * @param userId The user whose tokens should be revoked
     * @return Result indicating success or failure
     */
    suspend fun revokeAllUserTokens(userId: UserId): Result<Unit> = runCatching {
        dbQuery {
            val userUuid = userId.uuid.toJavaUuid()

            val updated = RefreshTokensTable.update(
                {
                    (RefreshTokensTable.userId eq userUuid) and
                        (RefreshTokensTable.isRevoked eq false)
                }
            ) {
                it[RefreshTokensTable.isRevoked] = true
            }

            logger.info("Revoked all refresh tokens for user: ${userId.value}, count: $updated")
        }
    }.onFailure { error ->
        logger.error("Failed to revoke all tokens for user: ${userId.value}", error)
    }

    /**
     * Clean up expired and revoked tokens
     *
     * Should be called periodically to maintain database hygiene.
     *
     * @return Result containing count of deleted tokens
     */
    suspend fun cleanupExpiredTokens(): Result<Int> = runCatching {
        dbQuery {
            val now = now().toLocalDateTime(TimeZone.UTC)

            // Delete tokens that are either expired OR revoked
            val deleted = RefreshTokensTable.deleteWhere {
                (expiresAt less now) or (isRevoked eq true)
            }

            logger.info("Cleaned up expired/revoked refresh tokens, count: $deleted")
            deleted
        }
    }.onFailure { error ->
        logger.error("Failed to cleanup expired tokens", error)
    }

    /**
     * Count active sessions (non-revoked, non-expired tokens) for a user.
     *
     * Used for enforcing concurrent session limits.
     *
     * @param userId The user to count sessions for
     * @return Number of active sessions
     */
    suspend fun countActiveForUser(userId: UserId): Int = try {
        dbQuery {
            val userUuid = userId.uuid.toJavaUuid()
            val now = now().toLocalDateTime(TimeZone.UTC)

            RefreshTokensTable
                .selectAll()
                .where {
                    (RefreshTokensTable.userId eq userUuid) and
                        (RefreshTokensTable.isRevoked eq false) and
                        (RefreshTokensTable.expiresAt greater now)
                }
                .count()
                .toInt()
        }
    } catch (error: Exception) {
        logger.error("Failed to count active sessions for user: ${userId.value}", error)
        0
    }

    /**
     * Revoke the oldest active session for a user.
     *
     * Used when the user reaches their concurrent session limit
     * to make room for a new session.
     *
     * @param userId The user whose oldest session should be revoked
     * @return Result indicating success or failure
     */
    suspend fun revokeOldestForUser(userId: UserId): Result<Unit> = runCatching {
        dbQuery {
            val userUuid = userId.uuid.toJavaUuid()
            val now = now().toLocalDateTime(TimeZone.UTC)

            // Find the oldest active token
            val oldestToken = RefreshTokensTable
                .selectAll()
                .where {
                    (RefreshTokensTable.userId eq userUuid) and
                        (RefreshTokensTable.isRevoked eq false) and
                        (RefreshTokensTable.expiresAt greater now)
                }
                .orderBy(RefreshTokensTable.createdAt, SortOrder.ASC)
                .limit(1)
                .singleOrNull()

            if (oldestToken != null) {
                val tokenId = oldestToken[RefreshTokensTable.id].value
                RefreshTokensTable.update({ RefreshTokensTable.id eq tokenId }) {
                    it[RefreshTokensTable.isRevoked] = true
                }
                logger.info("Revoked oldest session for user: ${userId.value}, token ID: $tokenId")
            } else {
                logger.debug("No active sessions to revoke for user: ${userId.value}")
            }
        }
    }.onFailure { error ->
        logger.error("Failed to revoke oldest session for user: ${userId.value}", error)
    }

    /**
     * Get all active tokens for a user
     *
     * Useful for displaying active sessions to the user.
     *
     * @param userId The user to query
     * @return List of active token information
     */
    suspend fun getUserActiveTokens(userId: UserId): List<RefreshTokenInfo> = try {
        dbQuery {
            val userUuid = userId.uuid.toJavaUuid()
            val now = now().toLocalDateTime(TimeZone.UTC)

            RefreshTokensTable
                .selectAll()
                .where {
                    (RefreshTokensTable.userId eq userUuid) and
                        (RefreshTokensTable.isRevoked eq false) and
                        (RefreshTokensTable.expiresAt greater now)
                }
                .orderBy(RefreshTokensTable.createdAt, SortOrder.DESC)
                .map { row ->
                    RefreshTokenInfo(
                        tokenId = row[RefreshTokensTable.id].value.toString(),
                        createdAt = row[RefreshTokensTable.createdAt].toKotlinxInstant(),
                        expiresAt = row[RefreshTokensTable.expiresAt].toKotlinxInstant(),
                        isRevoked = row[RefreshTokensTable.isRevoked]
                    )
                }
        }
    } catch (error: Exception) {
        logger.error("Failed to get active tokens for user: ${userId.value}", error)
        emptyList()
    }

    /**
     * Generate a secure hash of a token for logging purposes
     *
     * Never log the actual token value - this creates a security risk.
     * Instead, log a hash that can be used for debugging/correlation.
     *
     * @param token The token to hash
     * @return First 8 characters of SHA-256 hash
     */
    private fun tokenHash(token: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(token.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("Failed to hash token", e)
            throw e
        }
    }
}
