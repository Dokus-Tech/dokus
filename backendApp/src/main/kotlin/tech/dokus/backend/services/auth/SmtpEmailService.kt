@file:OptIn(DelicateCoroutinesApi::class)
@file:Suppress("MaxLineLength") // Long email template lines

package tech.dokus.backend.services.auth

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.dokus.foundation.backend.utils.loggerFor
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart

/**
 * SMTP-based email service implementation using JavaMail API.
 *
 * Features:
 * - Asynchronous email sending using coroutines
 * - HTML email templates with fallback text
 * - TLS/SSL support
 * - Connection pooling via JavaMail session
 * - Comprehensive error handling and logging
 * - Graceful degradation (failures don't crash the app)
 *
 * Security:
 * - Always uses TLS when enabled
 * - Credentials from environment variables
 * - No PII in error messages
 * - All operations logged for audit
 */
class SmtpEmailService(
    private val config: EmailConfig
) : EmailService {
    private val logger = loggerFor()
    private val session: Session by lazy { createSession() }

    init {
        logger.info(
            "SMTP Email Service initialized (host: ${config.smtp.host}, port: ${config.smtp.port}, TLS: ${config.smtp.enableTls})"
        )
    }

    override suspend fun sendPasswordResetEmail(
        recipientEmail: String,
        resetToken: String,
        expirationHours: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val resetUrl = "${config.templates.baseUrl}/reset-password?token=$resetToken"
            val subject = "Reset Your Dokus Password"

            val htmlContent = buildPasswordResetHtml(resetUrl, expirationHours)
            val textContent = buildPasswordResetText(resetUrl, expirationHours)

            sendEmail(
                to = recipientEmail,
                subject = subject,
                htmlContent = htmlContent,
                textContent = textContent
            )

            logger.info("Password reset email queued for: ${maskEmail(recipientEmail)}")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to send password reset email to ${maskEmail(recipientEmail)}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendEmailVerificationEmail(
        recipientEmail: String,
        verificationToken: String,
        expirationHours: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val verificationUrl =
                "${config.templates.baseUrl}/verify-email?token=$verificationToken"
            val subject = "Verify Your Dokus Email Address"

            val htmlContent = buildEmailVerificationHtml(verificationUrl, expirationHours)
            val textContent = buildEmailVerificationText(verificationUrl, expirationHours)

            sendEmail(
                to = recipientEmail,
                subject = subject,
                htmlContent = htmlContent,
                textContent = textContent
            )

            logger.info("Email verification email queued for: ${maskEmail(recipientEmail)}")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to send verification email to ${maskEmail(recipientEmail)}", e)
            Result.failure(e)
        }
    }

    override suspend fun sendWelcomeEmail(
        recipientEmail: String,
        userName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val subject = "Welcome to Dokus!"

            val htmlContent = buildWelcomeEmailHtml(userName)
            val textContent = buildWelcomeEmailText(userName)

            sendEmail(
                to = recipientEmail,
                subject = subject,
                htmlContent = htmlContent,
                textContent = textContent
            )

            logger.info("Welcome email queued for: ${maskEmail(recipientEmail)}")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to send welcome email to ${maskEmail(recipientEmail)}", e)
            Result.failure(e)
        }
    }

    override suspend fun healthCheck(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val transport = session.getTransport("smtp")
            transport.connect(
                config.smtp.host,
                config.smtp.port,
                config.smtp.username,
                config.smtp.password
            )
            transport.close()
            logger.debug("SMTP health check successful")
            Result.success(Unit)
        } catch (e: Exception) {
            logger.warn("SMTP health check failed", e)
            Result.failure(e)
        }
    }

    /**
     * Send an email using JavaMail API.
     * Uses both HTML and plain text for maximum compatibility.
     */
    private fun sendEmail(
        to: String,
        subject: String,
        htmlContent: String,
        textContent: String
    ) {
        val message = MimeMessage(session).apply {
            setFrom(InternetAddress(config.from.email, config.from.name))
            setRecipients(Message.RecipientType.TO, InternetAddress.parse(to))
            setSubject(subject, "UTF-8")

            // Add Reply-To if configured
            config.replyTo?.let {
                replyTo = arrayOf(InternetAddress(it.email, it.name))
            }

            // Create multipart content with both HTML and text
            val multipart = MimeMultipart("alternative").apply {
                // Add text version first (fallback)
                addBodyPart(
                    MimeBodyPart().apply {
                        setText(textContent, "UTF-8")
                    }
                )

                // Add HTML version second (preferred)
                addBodyPart(
                    MimeBodyPart().apply {
                        setContent(htmlContent, "text/html; charset=UTF-8")
                    }
                )
            }

            setContent(multipart)
        }

        Transport.send(message)
    }

    /**
     * Create JavaMail session with SMTP configuration.
     */
    private fun createSession(): Session {
        val props = Properties().apply {
            put("mail.smtp.host", config.smtp.host)
            put("mail.smtp.port", config.smtp.port.toString())
            put("mail.smtp.auth", config.smtp.enableAuth.toString())
            put("mail.smtp.starttls.enable", config.smtp.enableTls.toString())
            put("mail.smtp.starttls.required", config.smtp.enableTls.toString())
            put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
            put("mail.smtp.connectiontimeout", config.smtp.connectionTimeout.toString())
            put("mail.smtp.timeout", config.smtp.timeout.toString())
            put("mail.debug", "false")
        }

        return if (config.smtp.enableAuth) {
            Session.getInstance(
                props,
                object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(config.smtp.username, config.smtp.password)
                    }
                }
            )
        } else {
            Session.getInstance(props)
        }
    }

    /**
     * Mask email address for logging (privacy protection).
     * Example: john.doe@example.com -> j***@example.com
     */
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return "***"
        val local = parts[0]
        val domain = parts[1]
        val maskedLocal = if (local.length > 1) {
            "${local.first()}${"*".repeat(minOf(3, local.length - 1))}"
        } else {
            "*"
        }
        return "$maskedLocal@$domain"
    }

    // ===== HTML Email Templates =====

    private fun buildPasswordResetHtml(resetUrl: String, expirationHours: Int): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Reset Your Password</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }
                .container { max-width: 600px; margin: 40px auto; background: #ffffff; padding: 40px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .logo { text-align: center; margin-bottom: 30px; font-size: 32px; font-weight: bold; color: #2563eb; }
                h1 { color: #1f2937; font-size: 24px; margin-bottom: 20px; }
                p { margin-bottom: 20px; color: #4b5563; }
                .button { display: inline-block; padding: 14px 28px; background-color: #2563eb; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: 600; margin: 20px 0; }
                .button:hover { background-color: #1d4ed8; }
                .expiry { background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px 16px; margin: 20px 0; border-radius: 4px; }
                .footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #e5e7eb; font-size: 14px; color: #6b7280; text-align: center; }
                .security-note { background-color: #f3f4f6; padding: 12px 16px; margin: 20px 0; border-radius: 4px; font-size: 14px; color: #6b7280; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="logo">Dokus</div>
                <h1>Reset Your Password</h1>
                <p>We received a request to reset your password. Click the button below to choose a new password:</p>
                <div style="text-align: center;">
                    <a href="$resetUrl" class="button">Reset Password</a>
                </div>
                <div class="expiry">
                    <strong>⏰ Important:</strong> This link will expire in $expirationHours hour${if (expirationHours != 1) "s" else ""}.
                </div>
                <p>If you didn't request a password reset, you can safely ignore this email. Your password will remain unchanged.</p>
                <div class="security-note">
                    <strong>Security tip:</strong> Never share this link with anyone. Dokus will never ask you for your password via email.
                </div>
                <div class="footer">
                    <p>Need help? Contact us at <a href="mailto:${config.templates.supportEmail}">${config.templates.supportEmail}</a></p>
                    <p>&copy; ${java.time.Year.now().value} Dokus. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()

    private fun buildPasswordResetText(resetUrl: String, expirationHours: Int): String = """
        Reset Your Password

        We received a request to reset your password. Click the link below to choose a new password:

        $resetUrl

        This link will expire in $expirationHours hour${if (expirationHours != 1) "s" else ""}.

        If you didn't request a password reset, you can safely ignore this email. Your password will remain unchanged.

        Security tip: Never share this link with anyone. Dokus will never ask you for your password via email.

        Need help? Contact us at ${config.templates.supportEmail}

        © ${java.time.Year.now().value} Dokus. All rights reserved.
    """.trimIndent()

    private fun buildEmailVerificationHtml(verificationUrl: String, expirationHours: Int): String =
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Verify Your Email</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }
                .container { max-width: 600px; margin: 40px auto; background: #ffffff; padding: 40px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .logo { text-align: center; margin-bottom: 30px; font-size: 32px; font-weight: bold; color: #2563eb; }
                h1 { color: #1f2937; font-size: 24px; margin-bottom: 20px; }
                p { margin-bottom: 20px; color: #4b5563; }
                .button { display: inline-block; padding: 14px 28px; background-color: #16a34a; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: 600; margin: 20px 0; }
                .button:hover { background-color: #15803d; }
                .expiry { background-color: #fef3c7; border-left: 4px solid #f59e0b; padding: 12px 16px; margin: 20px 0; border-radius: 4px; }
                .footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #e5e7eb; font-size: 14px; color: #6b7280; text-align: center; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="logo">Dokus</div>
                <h1>Verify Your Email Address</h1>
                <p>Thank you for signing up with Dokus! Please verify your email address by clicking the button below:</p>
                <div style="text-align: center;">
                    <a href="$verificationUrl" class="button">Verify Email</a>
                </div>
                <div class="expiry">
                    <strong>⏰ Note:</strong> This link will expire in $expirationHours hour${if (expirationHours != 1) "s" else ""}.
                </div>
                <p>If you didn't create an account with Dokus, you can safely ignore this email.</p>
                <div class="footer">
                    <p>Need help? Contact us at <a href="mailto:${config.templates.supportEmail}">${config.templates.supportEmail}</a></p>
                    <p>&copy; ${java.time.Year.now().value} Dokus. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()

    private fun buildEmailVerificationText(verificationUrl: String, expirationHours: Int): String =
        """
        Verify Your Email Address

        Thank you for signing up with Dokus! Please verify your email address by clicking the link below:

        $verificationUrl

        This link will expire in $expirationHours hour${if (expirationHours != 1) "s" else ""}.

        If you didn't create an account with Dokus, you can safely ignore this email.

        Need help? Contact us at ${config.templates.supportEmail}

        © ${java.time.Year.now().value} Dokus. All rights reserved.
        """.trimIndent()

    private fun buildWelcomeEmailHtml(userName: String): String = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Welcome to Dokus</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f4f4f4; }
                .container { max-width: 600px; margin: 40px auto; background: #ffffff; padding: 40px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
                .logo { text-align: center; margin-bottom: 30px; font-size: 32px; font-weight: bold; color: #2563eb; }
                h1 { color: #1f2937; font-size: 24px; margin-bottom: 20px; }
                p { margin-bottom: 20px; color: #4b5563; }
                .button { display: inline-block; padding: 14px 28px; background-color: #2563eb; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: 600; margin: 20px 0; }
                .button:hover { background-color: #1d4ed8; }
                .features { background-color: #f9fafb; padding: 20px; border-radius: 6px; margin: 20px 0; }
                .features ul { margin: 0; padding-left: 20px; }
                .features li { margin-bottom: 10px; }
                .footer { margin-top: 40px; padding-top: 20px; border-top: 1px solid #e5e7eb; font-size: 14px; color: #6b7280; text-align: center; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="logo">Dokus</div>
                <h1>Welcome to Dokus, $userName! 🎉</h1>
                <p>We're excited to have you on board. Dokus is your all-in-one financial management platform designed specifically for European freelancers and developers.</p>
                <div class="features">
                    <h2 style="margin-top: 0; font-size: 18px; color: #1f2937;">Here's what you can do with Dokus:</h2>
                    <ul>
                        <li><strong>Automated Invoicing:</strong> Create and send professional invoices in minutes</li>
                        <li><strong>Expense Tracking:</strong> Keep track of all your business expenses effortlessly</li>
                        <li><strong>Tax Compliance:</strong> Stay compliant with automatic VAT and tax calculations</li>
                        <li><strong>Financial Insights:</strong> Get real-time insights into your business finances</li>
                    </ul>
                </div>
                <div style="text-align: center;">
                    <a href="${config.templates.baseUrl}/dashboard" class="button">Get Started</a>
                </div>
                <p>If you have any questions or need assistance getting started, don't hesitate to reach out to our support team.</p>
                <div class="footer">
                    <p>Need help? Contact us at <a href="mailto:${config.templates.supportEmail}">${config.templates.supportEmail}</a></p>
                    <p>&copy; ${java.time.Year.now().value} Dokus. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()

    private fun buildWelcomeEmailText(userName: String): String = """
        Welcome to Dokus, $userName!

        We're excited to have you on board. Dokus is your all-in-one financial management platform designed specifically for European freelancers and developers.

        Here's what you can do with Dokus:

        - Automated Invoicing: Create and send professional invoices in minutes
        - Expense Tracking: Keep track of all your business expenses effortlessly
        - Tax Compliance: Stay compliant with automatic VAT and tax calculations
        - Financial Insights: Get real-time insights into your business finances

        Get started now: ${config.templates.baseUrl}/dashboard

        If you have any questions or need assistance getting started, don't hesitate to reach out to our support team.

        Need help? Contact us at ${config.templates.supportEmail}

        © ${java.time.Year.now().value} Dokus. All rights reserved.
    """.trimIndent()
}
