package ai.dokus.auth.backend.services

import ai.dokus.auth.backend.database.repository.PasswordResetTokenRepository
import ai.dokus.auth.backend.database.repository.RefreshTokenRepository
import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ktor.database.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Duration.Companion.hours

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
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
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

            // Check if user exists
            val user = userRepository.findByEmail(email)

            if (user != null) {
                val userId = UserId(user.id.value.toString())
                val expiresAt = now() + 1.hours

                // Store reset token via repository
                passwordResetTokenRepository.createToken(userId, token, expiresAt)
                    .onFailure { error ->
                        logger.error("Failed to create password reset token", error)
                        throw error
                    }

                logger.info("Password reset requested for user: ${userId.value}")
                logger.debug("Password reset token generated: ${token.take(10)}... (expires in 1 hour)")

                // Send email with reset link outside the transaction (async)
                // Note: Email failures don't prevent the flow (graceful degradation)
                emailScope.launch {
                    emailService.sendPasswordResetEmail(email, token, expirationHours = 1)
                        .onSuccess {
                            logger.debug("Password reset email sent successfully to ${email.take(3)}***")
                        }
                        .onFailure { error ->
                            logger.error("Failed to send password reset email, but token was created", error)
                        }
                }
            } else {
                // Email doesn't exist, but we still log and return success
                logger.debug("Password reset requested for non-existent email (returning success for security)")
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
            // Find and validate token
            val tokenInfo = passwordResetTokenRepository.findByToken(token)
                ?: return Result.failure(DokusException.PasswordResetTokenInvalid())

            // Check if already used
            if (tokenInfo.isUsed) {
                return Result.failure(DokusException.PasswordResetTokenInvalid())
            }

            // Check expiration
            if (tokenInfo.expiresAt < now()) {
                return Result.failure(DokusException.PasswordResetTokenExpired())
            }

            // Mark token as used
            passwordResetTokenRepository.markAsUsed(tokenInfo.tokenId)
                .onFailure { error ->
                    logger.error("Failed to mark password reset token as used", error)
                    throw error
                }

            // Update password via repository
            userRepository.updatePassword(tokenInfo.userId, newPassword)

            // Revoke all refresh tokens (force re-login everywhere)
            refreshTokenRepository.revokeAllUserTokens(tokenInfo.userId)

            logger.info("Password reset successful for user: ${tokenInfo.userId.value} (all sessions revoked)")
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
        return passwordResetTokenRepository.cleanupExpiredTokens()
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
