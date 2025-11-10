@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package ai.dokus.auth.backend.database.services

import ai.dokus.auth.backend.database.tables.RefreshTokensTable
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.ktor.database.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toJavaUuid

/**
 * Implementation of RefreshTokenService with secure token management
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
class RefreshTokenServiceImpl : RefreshTokenService {
    private val logger = LoggerFactory.getLogger(RefreshTokenServiceImpl::class.java)

    override suspend fun saveRefreshToken(
        userId: UserId,
        token: String,
        expiresAt: Instant
    ): Result<Unit> = runCatching {
        dbQuery {
            val userUuid = userId.uuid.toJavaUuid()

            RefreshTokensTable.insert {
                it[RefreshTokensTable.userId] = userUuid
                it[RefreshTokensTable.token] = token
                it[RefreshTokensTable.expiresAt] = expiresAt.toLocalDateTime(TimeZone.UTC)
                it[RefreshTokensTable.isRevoked] = false
            }

            logger.debug(
                "Saved refresh token for user: ${userId.value}, token hash: ${hashToken(token)}, expires: $expiresAt"
            )
        }
    }.onFailure { error ->
        logger.error("Failed to save refresh token for user: ${userId.value}", error)
    }

    override suspend fun validateAndRotate(oldToken: String): Result<UserId> = runCatching {
        dbQuery {
            // Find the token
            val tokenRow = RefreshTokensTable
                .selectAll()
                .where { RefreshTokensTable.token eq oldToken }
                .singleOrNull()
                ?: throw IllegalArgumentException("Refresh token not found")

            val tokenId = tokenRow[RefreshTokensTable.id].value
            val userId = tokenRow[RefreshTokensTable.userId].value
            val expiresAt = tokenRow[RefreshTokensTable.expiresAt]
            val isRevoked = tokenRow[RefreshTokensTable.isRevoked]

            // Security checks
            if (isRevoked) {
                logger.warn(
                    "Attempt to use revoked token (ID: $tokenId, hash: ${hashToken(oldToken)})"
                )
                throw SecurityException("Refresh token has been revoked")
            }

            val now = kotlinx.datetime.Clock.System.now()
            val expiresAtInstant = expiresAt.toInstant(TimeZone.UTC)

            if (expiresAtInstant < now) {
                logger.warn(
                    "Attempt to use expired token (ID: $tokenId, expired: $expiresAt, hash: ${hashToken(oldToken)})"
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
            UserId(kotlin.uuid.Uuid.parse(userId.toString()).toString())
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

    override suspend fun revokeToken(token: String): Result<Unit> = runCatching {
        dbQuery {
            val updated = RefreshTokensTable.update(
                { RefreshTokensTable.token eq token }
            ) {
                it[RefreshTokensTable.isRevoked] = true
            }

            if (updated == 0) {
                logger.warn("Attempted to revoke non-existent token (hash: ${hashToken(token)})")
                throw IllegalArgumentException("Refresh token not found")
            }

            logger.info("Revoked refresh token (hash: ${hashToken(token)}, count: $updated)")
        }
    }.onFailure { error ->
        if (error !is IllegalArgumentException) {
            logger.error("Failed to revoke refresh token", error)
        }
    }

    override suspend fun revokeAllUserTokens(userId: UserId): Result<Unit> = runCatching {
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

    override suspend fun cleanupExpiredTokens(): Result<Int> = runCatching {
        dbQuery {
            val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.UTC)

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

    override suspend fun getUserActiveTokens(userId: UserId): List<RefreshTokenInfo> = try {
        dbQuery {
            val userUuid = userId.uuid.toJavaUuid()
            val now = kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.UTC)

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
                        createdAt = row[RefreshTokensTable.createdAt].toInstant(TimeZone.UTC),
                        expiresAt = row[RefreshTokensTable.expiresAt].toInstant(TimeZone.UTC),
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
    private fun hashToken(token: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(token.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }.take(8)
        } catch (e: Exception) {
            logger.error("Failed to hash token", e)
            "********" // Fallback to masked value
        }
    }
}
