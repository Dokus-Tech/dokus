package ai.dokus.auth.backend.services

import ai.dokus.auth.backend.database.services.RefreshTokenService
import ai.dokus.auth.backend.database.tables.PasswordResetTokensTable
import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.foundation.ktor.database.now
import ai.dokus.foundation.ktor.services.UserService
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Duration.Companion.hours

/**
 * Helper function to convert kotlinx.datetime.LocalDateTime to kotlinx.datetime.Instant
 */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun kotlinx.datetime.LocalDateTime.toKotlinxInstant(): Instant {
    val kotlinTimeInstant = this.toInstant(TimeZone.UTC)
    return Instant.fromEpochSeconds(
        kotlinTimeInstant.epochSeconds,
        kotlinTimeInstant.nanosecondsOfSecond.toLong()
    )
}

/**
 * Service for handling password reset flows with email tokens.
 *
 * Security considerations:
 * - Email enumeration protection: Always returns success even if email doesn't exist
 * - Cryptographically secure token generation (32 bytes, URL-safe Base64)
 * - 1-hour token expiration
 * - One-time use tokens
 * - All refresh tokens revoked on successful password reset
 * - Automatic cleanup of expired/used tokens
 * - Email failures don't prevent token generation (graceful degradation)
 *
 * Flow:
 * 1. User requests reset via [requestReset] with their email
 * 2. System generates secure token, stores it, and sends email
 * 3. User clicks link in email and submits new password via [resetPassword]
 * 4. Password updated, token marked as used, all sessions invalidated
 */
class PasswordResetService(
    private val userService: UserService,
    private val refreshTokenService: RefreshTokenService,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(PasswordResetService::class.java)
    private val emailScope = CoroutineScope(Dispatchers.IO)

    /**
     * Request a password reset for the given email address.
     *
     * SECURITY: Always returns success, even if email doesn't exist.
     * This prevents attackers from using this endpoint to enumerate valid emails.
     *
     * @param email The email address to send password reset to
     * @return Result indicating success (always succeeds)
     */
    suspend fun requestReset(email: String): Result<Unit> {
        return try {
            // IMPORTANT: Always return success even if email doesn't exist (security)
            // This prevents email enumeration attacks

            val token = generateSecureToken()

            val userFound = dbQuery {
                val user = UsersTable
                    .selectAll()
                    .where { UsersTable.email eq email }
                    .singleOrNull()

                if (user != null) {
                    val userId = user[UsersTable.id].value
                    val expiresAt = now() + 1.hours

                    // Store reset token
                    PasswordResetTokensTable.insert {
                        it[PasswordResetTokensTable.userId] = userId
                        it[PasswordResetTokensTable.token] = token
                        it[PasswordResetTokensTable.expiresAt] = expiresAt.toLocalDateTime(TimeZone.UTC)
                    }

                    logger.info("Password reset requested for user: $userId")
                    logger.debug("Password reset token generated: ${token.take(10)}... (expires in 1 hour)")
                    true
                } else {
                    // Email doesn't exist, but we still log and return success
                    logger.debug("Password reset requested for non-existent email (returning success for security)")
                    false
                }
            }

            // Send email with reset link outside the transaction (async)
            // Note: Email failures don't prevent the flow (graceful degradation)
            if (userFound) {
                emailScope.launch {
                    emailService.sendPasswordResetEmail(email, token, expirationHours = 1)
                        .onSuccess {
                            logger.debug("Password reset email sent successfully to ${email.take(3)}***")
                        }
                        .onFailure { error ->
                            logger.error("Failed to send password reset email, but token was created", error)
                        }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Password reset request failed", e)
            Result.failure(DokusException.InternalError("Password reset request failed"))
        }
    }

    /**
     * Reset password using a valid token from email.
     *
     * Validates token, updates password, marks token as used, and revokes all sessions.
     *
     * @param token The password reset token from email
     * @param newPassword The new password to set
     * @return Result indicating success or specific error
     */
    suspend fun resetPassword(token: String, newPassword: String): Result<Unit> {
        return try {
            val userId = dbQuery {
                // Find token
                val resetRow = PasswordResetTokensTable
                    .selectAll()
                    .where {
                        (PasswordResetTokensTable.token eq token) and
                        (PasswordResetTokensTable.isUsed eq false)
                    }
                    .singleOrNull()
                    ?: throw DokusException.PasswordResetTokenInvalid()

                val tokenId = resetRow[PasswordResetTokensTable.id].value
                val userId = resetRow[PasswordResetTokensTable.userId].value.toString()
                val expiresAt = resetRow[PasswordResetTokensTable.expiresAt]

                // Check expiration
                val expiresAtInstant = expiresAt.toKotlinxInstant()
                if (expiresAtInstant < now()) {
                    throw DokusException.PasswordResetTokenExpired()
                }

                // Mark token as used
                PasswordResetTokensTable.update({ PasswordResetTokensTable.id eq tokenId }) {
                    it[isUsed] = true
                }

                userId
            }

            // Update password (suspend function, must be called outside dbQuery)
            userService.updatePassword(UserId(userId), newPassword)

            // Revoke all refresh tokens (force re-login everywhere)
            refreshTokenService.revokeAllUserTokens(UserId(userId))

            logger.info("Password reset successful for user: $userId (all sessions revoked)")
            Result.success(Unit)
        } catch (e: DokusException) {
            logger.warn("Password reset failed: ${e.errorCode}")
            Result.failure(e)
        } catch (e: Exception) {
            logger.error("Password reset error", e)
            Result.failure(DokusException.InternalError("Password reset failed"))
        }
    }

    /**
     * Clean up expired and used password reset tokens.
     *
     * Should be called periodically (e.g., daily cron job) to maintain database hygiene.
     *
     * @return Result containing count of deleted tokens
     */
    suspend fun cleanupExpiredTokens(): Result<Int> {
        return try {
            val count = dbQuery {
                val currentTime = now().toLocalDateTime(TimeZone.UTC)
                PasswordResetTokensTable.deleteWhere {
                    (expiresAt less currentTime) or (isUsed eq true)
                }
            }
            logger.info("Cleaned up $count expired/used password reset tokens")
            Result.success(count)
        } catch (e: Exception) {
            logger.error("Token cleanup failed", e)
            Result.failure(e)
        }
    }

    /**
     * Generate a cryptographically secure random token.
     *
     * Uses SecureRandom with 32 bytes (256 bits) of entropy,
     * encoded as URL-safe Base64 (43 characters).
     *
     * @return URL-safe Base64 encoded random token
     */
    private fun generateSecureToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
