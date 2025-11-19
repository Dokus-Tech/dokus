@file:OptIn(ExperimentalUuidApi::class)

package ai.dokus.auth.backend.services

import ai.dokus.auth.backend.database.repository.UserRepository
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ktor.database.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.ExperimentalUuidApi

/**
 * Service for managing email verification workflow.
 *
 * Features:
 * - Generates cryptographically secure verification tokens
 * - Handles email verification with token validation
 * - Supports resending verification emails
 * - Graceful degradation (email failures don't block registration)
 * - 24-hour token expiration
 *
 * Flow:
 * 1. User registers, system generates verification token
 * 2. Verification email sent (failure logged, doesn't block registration)
 * 3. User clicks verification link from email
 * 4. Token validated and email marked as verified
 */
class EmailVerificationService(
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {
    private val logger = LoggerFactory.getLogger(EmailVerificationService::class.java)
    private val emailScope = CoroutineScope(Dispatchers.IO)

    /**
     * Generates a verification token and updates the user record.
     *
     * @param userId User ID to generate verification token for
     * @param email User's email address (for sending verification email)
     * @return Result indicating success or failure
     */
    suspend fun sendVerificationEmail(userId: UserId, email: String): Result<Unit> {
        return try {
            val token = generateSecureToken()
            val expiry = now() + 24.hours

            userRepository.setEmailVerificationToken(userId, token, expiry)

            logger.info("Email verification token generated for user: ${userId.value}")
            logger.debug("Verification link: /auth/verify-email?token=$token")

            // Send verification email (async, outside transaction)
            // Note: Email failures don't prevent registration (graceful degradation)
            emailScope.launch {
                emailService.sendEmailVerificationEmail(email, token, expirationHours = 24)
                    .onSuccess {
                        logger.debug("Email verification sent successfully to ${email.take(3)}***")
                    }
                    .onFailure { error ->
                        logger.error("Failed to send verification email, but token was created", error)
                    }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to send verification email", e)
            Result.failure(DokusException.InternalError("Failed to send verification email"))
        }
    }

    /**
     * Verifies a user's email address using the verification token.
     *
     * @param token Verification token from the email link
     * @return Result indicating success or failure
     */
    suspend fun verifyEmail(token: String): Result<Unit> {
        return try {
            val userInfo = userRepository.findByVerificationToken(token)
                ?: return Result.failure(DokusException.EmailVerificationTokenInvalid())

            // Check expiration
            if (userInfo.expiresAt < now()) {
                return Result.failure(DokusException.EmailVerificationTokenExpired())
            }

            // Mark as verified
            userRepository.markEmailVerified(userInfo.userId)

            logger.info("Email verified for user: ${userInfo.userId.value}")
            Result.success(Unit)
        } catch (e: DokusException) {
            logger.warn("Email verification failed: ${e.errorCode}")
            Result.failure(e)
        } catch (e: Exception) {
            logger.error("Email verification error", e)
            Result.failure(DokusException.InternalError("Email verification failed"))
        }
    }

    /**
     * Resends verification email to a user who hasn't verified yet.
     *
     * @param userId User ID to resend verification email for
     * @return Result indicating success or failure
     */
    suspend fun resendVerificationEmail(userId: UserId): Result<Unit> {
        return try {
            val user = userRepository.findById(userId)
                ?: return Result.failure(DokusException.InternalError("User not found"))

            if (userRepository.isEmailVerified(userId)) {
                return Result.failure(DokusException.EmailAlreadyVerified())
            }

            sendVerificationEmail(userId, user.email.value)
        } catch (e: DokusException) {
            Result.failure(e)
        } catch (e: Exception) {
            logger.error("Failed to resend verification email", e)
            Result.failure(DokusException.InternalError("Failed to resend verification email"))
        }
    }

    /**
     * Generates a cryptographically secure random token for email verification.
     * Uses 32 bytes of randomness encoded as URL-safe Base64.
     *
     * @return URL-safe Base64 encoded token
     */
    private fun generateSecureToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
