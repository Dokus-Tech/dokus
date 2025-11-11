@file:OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)

package ai.dokus.auth.backend.services

import ai.dokus.auth.backend.database.tables.UsersTable
import ai.dokus.foundation.domain.UserId
import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.ktor.database.dbQuery
import ai.dokus.foundation.ktor.database.now
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.selectAll
import org.jetbrains.exposed.v1.core.update
import org.slf4j.LoggerFactory
import java.security.SecureRandom
import java.util.Base64
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
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

            dbQuery {
                val userUuid = java.util.UUID.fromString(userId.value)
                UsersTable.update({ UsersTable.id eq userUuid }) {
                    it[emailVerificationToken] = token
                    it[emailVerificationExpiry] = expiry.toLocalDateTime(TimeZone.UTC)
                }
            }

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
            dbQuery {
                val user = UsersTable
                    .selectAll()
                    .where {
                        (UsersTable.emailVerificationToken eq token) and
                        (UsersTable.emailVerified eq false)
                    }
                    .singleOrNull()
                    ?: throw DokusException.EmailVerificationTokenInvalid()

                val userId = user[UsersTable.id].value
                val expiry = user[UsersTable.emailVerificationExpiry]
                    ?: throw DokusException.EmailVerificationTokenInvalid()

                // Check expiration
                val expiryInstant = expiry.toInstant(TimeZone.UTC)
                if (expiryInstant < now()) {
                    throw DokusException.EmailVerificationTokenExpired()
                }

                // Mark as verified
                UsersTable.update({ UsersTable.id eq userId }) {
                    it[emailVerified] = true
                    it[emailVerificationToken] = null
                    it[emailVerificationExpiry] = null
                }

                logger.info("Email verified for user: $userId")
            }

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
            val user = dbQuery {
                val userUuid = java.util.UUID.fromString(userId.value)
                UsersTable
                    .selectAll()
                    .where { UsersTable.id eq userUuid }
                    .singleOrNull()
            } ?: return Result.failure(
                DokusException.InternalError("User not found")
            )

            if (user[UsersTable.emailVerified]) {
                throw DokusException.EmailAlreadyVerified()
            }

            val email = user[UsersTable.email]
            sendVerificationEmail(userId, email)
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
