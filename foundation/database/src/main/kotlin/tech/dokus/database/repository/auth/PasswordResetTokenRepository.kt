
package tech.dokus.database.repository.auth
import kotlin.uuid.Uuid

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import tech.dokus.database.tables.auth.PasswordResetTokensTable
import tech.dokus.database.utils.toKotlinxInstant
import tech.dokus.domain.ids.UserId
import tech.dokus.foundation.backend.database.dbQuery
import tech.dokus.foundation.backend.database.now
import tech.dokus.foundation.backend.utils.loggerFor
import java.security.MessageDigest

/**
 * Information about a password reset token
 *
 * @property tokenId The database ID of the token
 * @property userId The user this token belongs to
 * @property expiresAt When this token expires
 * @property isUsed Whether this token has been used
 */
data class PasswordResetTokenInfo(
    val tokenId: Uuid,
    val userId: UserId,
    val expiresAt: Instant,
    val isUsed: Boolean
)

/**
 * Repository for managing password reset tokens.
 *
 * Security features:
 * - One-time use tokens (marked as used after password reset)
 * - Automatic expiration checking
 * - Secure token storage
 * - Automatic cleanup of expired/used tokens
 *
 * All database operations are transactional via dbQuery wrapper.
 */
class PasswordResetTokenRepository {
    private val logger = loggerFor()

    /**
     * Create a new password reset token for a user.
     *
     * @param userId The user requesting password reset
     * @param token The cryptographically secure token string
     * @param expiresAt When this token expires (typically 1 hour from creation)
     * @return Result indicating success or failure
     */
    suspend fun createToken(
        userId: UserId,
        token: String,
        expiresAt: Instant
    ): Result<Unit> = runCatching {
        dbQuery {
            val userUuid = userId.uuid
            val tokenHash = tokenHash(token)

            PasswordResetTokensTable.insert {
                it[PasswordResetTokensTable.userId] = userUuid
                it[PasswordResetTokensTable.tokenHash] = tokenHash
                it[PasswordResetTokensTable.expiresAt] = expiresAt.toLocalDateTime(TimeZone.UTC)
                it[PasswordResetTokensTable.isUsed] = false
            }

            logger.debug("Created password reset token for user: ${userId.value}")
        }
    }.onFailure { error ->
        logger.error("Failed to create password reset token for user: ${userId.value}", error)
    }

    /**
     * Find a password reset token by its token string.
     *
     * @param token The token string to search for
     * @return Token info if found and not used, null otherwise
     */
    suspend fun findByToken(token: String): PasswordResetTokenInfo? = try {
        dbQuery {
            val tokenHash = tokenHash(token)
            PasswordResetTokensTable
                .selectAll()
                .where { PasswordResetTokensTable.tokenHash eq tokenHash }
                .singleOrNull()
                ?.let { row ->
                    PasswordResetTokenInfo(
                        tokenId = row[PasswordResetTokensTable.id].value,
                        userId = UserId(row[PasswordResetTokensTable.userId].value.toString()),
                        expiresAt = row[PasswordResetTokensTable.expiresAt].toKotlinxInstant(),
                        isUsed = row[PasswordResetTokensTable.isUsed]
                    )
                }
        }
    } catch (error: Exception) {
        logger.error("Failed to find password reset token", error)
        null
    }

    /**
     * Mark a password reset token as used.
     *
     * @param tokenId The ID of the token to mark as used
     * @return Result indicating success or failure
     */
    suspend fun markAsUsed(tokenId: Uuid): Result<Unit> = runCatching {
        dbQuery {
            val updated =
                PasswordResetTokensTable.update({ PasswordResetTokensTable.id eq tokenId }) {
                    it[isUsed] = true
                }

            if (updated == 0) {
                throw IllegalArgumentException("Password reset token not found: $tokenId")
            }

            logger.debug("Marked password reset token as used: $tokenId")
        }
    }.onFailure { error ->
        logger.error("Failed to mark password reset token as used: $tokenId", error)
    }

    /**
     * Clean up expired and used password reset tokens.
     *
     * Should be called periodically (e.g., daily cron job) to maintain database hygiene.
     *
     * @return Result containing count of deleted tokens
     */
    suspend fun cleanupExpiredTokens(): Result<Int> = runCatching {
        dbQuery {
            val currentTime = now().toLocalDateTime(TimeZone.UTC)
            val deleted = PasswordResetTokensTable.deleteWhere {
                (expiresAt less currentTime) or (isUsed eq true)
            }

            logger.info("Cleaned up $deleted expired/used password reset tokens")
            deleted
        }
    }.onFailure { error ->
        logger.error("Failed to cleanup expired password reset tokens", error)
    }

    private fun tokenHash(token: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(token.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            logger.error("Failed to hash password reset token", e)
            throw e
        }
    }
}
