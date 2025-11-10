package ai.dokus.auth.backend.services

/**
 * Email service interface for sending transactional emails.
 *
 * Implementations should handle:
 * - Asynchronous email sending
 * - Error handling and logging
 * - Template rendering
 * - Provider-specific configuration
 *
 * Security considerations:
 * - Never expose email service failures to users (prevents information disclosure)
 * - Always log email sending attempts for audit trails
 * - Use TLS/SSL for SMTP connections
 * - Validate email addresses before sending
 */
interface EmailService {
    /**
     * Send a password reset email with a secure token link.
     *
     * @param recipientEmail Email address to send the reset link to
     * @param resetToken Secure token for password reset (URL-safe)
     * @param expirationHours Number of hours until the token expires
     * @return Result indicating success or failure (failures are logged, not thrown)
     */
    suspend fun sendPasswordResetEmail(
        recipientEmail: String,
        resetToken: String,
        expirationHours: Int = 1
    ): Result<Unit>

    /**
     * Send an email verification email with a secure token link.
     *
     * @param recipientEmail Email address to verify
     * @param verificationToken Secure token for email verification (URL-safe)
     * @param expirationHours Number of hours until the token expires
     * @return Result indicating success or failure (failures are logged, not thrown)
     */
    suspend fun sendEmailVerificationEmail(
        recipientEmail: String,
        verificationToken: String,
        expirationHours: Int = 24
    ): Result<Unit>

    /**
     * Send a welcome email after successful registration.
     *
     * @param recipientEmail New user's email address
     * @param userName User's display name
     * @return Result indicating success or failure (failures are logged, not thrown)
     */
    suspend fun sendWelcomeEmail(
        recipientEmail: String,
        userName: String
    ): Result<Unit>

    /**
     * Health check for email service connectivity.
     *
     * @return Result indicating if the email service is accessible
     */
    suspend fun healthCheck(): Result<Unit>
}
