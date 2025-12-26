package ai.dokus.backend.services.auth

import org.slf4j.LoggerFactory

/**
 * No-op email service for development and testing.
 *
 * This service logs email operations but doesn't actually send emails.
 * Useful for:
 * - Local development without SMTP server
 * - Testing environments
 * - Scenarios where email notifications are disabled
 *
 * All operations return success and log what would have been sent.
 */
class DisabledEmailService : EmailService {
    private val logger = LoggerFactory.getLogger(DisabledEmailService::class.java)

    init {
        logger.warn("Email service is DISABLED - emails will be logged but not sent")
    }

    override suspend fun sendPasswordResetEmail(
        recipientEmail: String,
        resetToken: String,
        expirationHours: Int
    ): Result<Unit> {
        logger.info(
            """
            [EMAIL DISABLED] Would send password reset email:
              To: $recipientEmail
              Token: ${resetToken.take(20)}...
              Expiration: $expirationHours hours
              Link: (would be generated based on config)
            """.trimIndent()
        )
        return Result.success(Unit)
    }

    override suspend fun sendEmailVerificationEmail(
        recipientEmail: String,
        verificationToken: String,
        expirationHours: Int
    ): Result<Unit> {
        logger.info(
            """
            [EMAIL DISABLED] Would send email verification:
              To: $recipientEmail
              Token: ${verificationToken.take(20)}...
              Expiration: $expirationHours hours
              Link: (would be generated based on config)
            """.trimIndent()
        )
        return Result.success(Unit)
    }

    override suspend fun sendWelcomeEmail(
        recipientEmail: String,
        userName: String
    ): Result<Unit> {
        logger.info(
            """
            [EMAIL DISABLED] Would send welcome email:
              To: $recipientEmail
              User: $userName
            """.trimIndent()
        )
        return Result.success(Unit)
    }

    override suspend fun healthCheck(): Result<Unit> {
        logger.debug("[EMAIL DISABLED] Health check - service is disabled")
        return Result.success(Unit)
    }
}
