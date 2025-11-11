# Email Service Documentation

## Overview

The Dokus authentication system includes a production-ready email service for sending transactional emails. The service supports password reset emails, email verification emails, and welcome emails.

## Features

- **Multiple Providers**: SMTP implementation with support for any SMTP server (Gmail, SendGrid, AWS SES, etc.)
- **Graceful Degradation**: Email failures don't block critical flows (registration, password reset)
- **Async Processing**: All emails sent asynchronously using Kotlin coroutines
- **HTML Templates**: Beautiful, responsive HTML email templates with plain text fallbacks
- **Security**: TLS/SSL support, email masking in logs, secure token handling
- **Comprehensive Logging**: All email operations logged for audit trails
- **Development Mode**: Disabled email service for local development (logs only, no emails sent)

## Architecture

### Components

1. **EmailService** (interface): Defines the email service contract
2. **SmtpEmailService**: Production SMTP implementation using JavaMail API
3. **DisabledEmailService**: No-op implementation for development/testing
4. **EmailConfig**: Configuration data class for email settings

### Dependency Injection

The email service is automatically configured based on the `email.enabled` and `email.provider` settings:

```kotlin
single<EmailService> {
    val appConfig = get<AppBaseConfig>()
    val emailConfig = EmailConfig.load(appConfig)

    if (emailConfig.enabled && emailConfig.provider == "smtp") {
        SmtpEmailService(emailConfig)
    } else {
        DisabledEmailService()
    }
}
```

## Configuration

### Development (application.conf)

By default, email is disabled for local development:

```hocon
email {
    enabled = false
    provider = "disabled"

    templates {
        baseUrl = "http://localhost:8081"
        supportEmail = "support@dokus.ai"
    }
}
```

### Production (application-prod.conf or Environment Variables)

For production, configure SMTP settings via environment variables:

```bash
# Enable email service
export EMAIL_ENABLED=true
export EMAIL_PROVIDER=smtp

# SMTP Configuration
export SMTP_HOST=smtp.gmail.com
export SMTP_PORT=587
export SMTP_USERNAME=noreply@dokus.ai
export SMTP_PASSWORD=your-app-specific-password
export SMTP_ENABLE_TLS=true
export SMTP_ENABLE_AUTH=true

# Sender Information
export EMAIL_FROM_ADDRESS=noreply@dokus.ai
export EMAIL_FROM_NAME=Dokus
export EMAIL_REPLY_TO_ADDRESS=support@dokus.ai
export EMAIL_REPLY_TO_NAME="Dokus Support"

# Email Templates
export EMAIL_BASE_URL=https://app.dokus.ai
export EMAIL_SUPPORT_ADDRESS=support@dokus.ai
```

### SMTP Provider Examples

#### Gmail (App-Specific Password Required)

```bash
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password  # Generate at https://myaccount.google.com/apppasswords
SMTP_ENABLE_TLS=true
SMTP_ENABLE_AUTH=true
```

#### SendGrid

```bash
SMTP_HOST=smtp.sendgrid.net
SMTP_PORT=587
SMTP_USERNAME=apikey
SMTP_PASSWORD=your-sendgrid-api-key
SMTP_ENABLE_TLS=true
SMTP_ENABLE_AUTH=true
```

#### AWS SES

```bash
SMTP_HOST=email-smtp.us-east-1.amazonaws.com
SMTP_PORT=587
SMTP_USERNAME=your-ses-smtp-username
SMTP_PASSWORD=your-ses-smtp-password
SMTP_ENABLE_TLS=true
SMTP_ENABLE_AUTH=true
```

#### Mailgun

```bash
SMTP_HOST=smtp.mailgun.org
SMTP_PORT=587
SMTP_USERNAME=postmaster@your-domain.mailgun.org
SMTP_PASSWORD=your-mailgun-smtp-password
SMTP_ENABLE_TLS=true
SMTP_ENABLE_AUTH=true
```

## Usage

### Password Reset Email

```kotlin
class PasswordResetService(
    private val emailService: EmailService
) {
    suspend fun requestReset(email: String): Result<Unit> {
        val token = generateSecureToken()

        // Send email (failures are logged but don't block the flow)
        emailService.sendPasswordResetEmail(email, token, expirationHours = 1)
            .onSuccess {
                logger.info("Password reset email sent")
            }
            .onFailure { error ->
                logger.error("Failed to send email, but token was created", error)
            }

        return Result.success(Unit)
    }
}
```

### Email Verification

```kotlin
class EmailVerificationService(
    private val emailService: EmailService
) {
    suspend fun sendVerification(userId: UserId, email: String): Result<Unit> {
        val token = generateSecureToken()

        // Send verification email
        emailService.sendEmailVerificationEmail(email, token, expirationHours = 24)
            .onSuccess {
                logger.info("Verification email sent")
            }
            .onFailure { error ->
                logger.error("Failed to send verification email", error)
            }

        return Result.success(Unit)
    }
}
```

## Email Templates

All emails include both HTML and plain text versions for maximum compatibility. Templates are built using inline styles for consistent rendering across email clients.

### Password Reset Email

- **Subject**: Reset Your Dokus Password
- **Content**:
  - Clear call-to-action button
  - Token expiration warning
  - Security tips
  - Support contact information

### Email Verification Email

- **Subject**: Verify Your Dokus Email Address
- **Content**:
  - Verification button
  - Expiration notice
  - Help contact information

### Welcome Email

- **Subject**: Welcome to Dokus!
- **Content**:
  - Personalized greeting
  - Platform features overview
  - Get started button
  - Support information

## Security Considerations

### Email Enumeration Protection

The password reset flow always returns success, even if the email doesn't exist. This prevents attackers from using the endpoint to discover valid email addresses:

```kotlin
suspend fun requestReset(email: String): Result<Unit> {
    // Always return success (security)
    if (userExists) {
        // Generate token and send email
    } else {
        // Log but still return success
        logger.debug("Password reset requested for non-existent email")
    }
    return Result.success(Unit)
}
```

### Email Masking in Logs

Email addresses are masked in logs to protect user privacy:

```kotlin
private fun maskEmail(email: String): String {
    // john.doe@example.com -> j***@example.com
}
```

### TLS/SSL

All SMTP connections use TLS encryption when enabled:

```kotlin
props.put("mail.smtp.starttls.enable", "true")
props.put("mail.smtp.starttls.required", "true")
props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3")
```

### Secure Token Handling

Tokens are generated using `SecureRandom` with 32 bytes (256 bits) of entropy and URL-safe Base64 encoding.

## Monitoring and Logging

### Log Levels

- **INFO**: Email sent successfully, important state changes
- **DEBUG**: Token generation, link details (development only)
- **WARN**: Email service health check failures
- **ERROR**: Failed email sends, SMTP connection errors

### Example Logs

```
INFO  - SMTP Email Service initialized (host: smtp.gmail.com, port: 587, TLS: true)
INFO  - Password reset requested for user: 550e8400-e29b-41d4-a716-446655440000
DEBUG - Password reset email sent successfully to joh***@example.com
ERROR - Failed to send password reset email, but token was created
```

## Health Checks

The email service includes a health check endpoint:

```kotlin
suspend fun healthCheck(): Result<Unit> {
    try {
        val transport = session.getTransport("smtp")
        transport.connect(host, port, username, password)
        transport.close()
        return Result.success(Unit)
    } catch (e: Exception) {
        return Result.failure(e)
    }
}
```

## Error Handling

### Graceful Degradation

Email failures never block critical user flows. If an email fails to send:

1. The error is logged with full stack trace
2. The operation (registration, password reset) continues successfully
3. The user can still use the token (if it was created)
4. Monitoring alerts can be triggered based on error logs

### Common Error Scenarios

1. **SMTP Connection Timeout**: Check firewall rules, verify host/port
2. **Authentication Failed**: Verify username/password, check app-specific passwords
3. **TLS Handshake Failed**: Ensure TLS is enabled, check SSL protocols
4. **Message Rejected**: Verify sender email is authorized, check SPF/DKIM records

## Testing

### Local Development

In development, use `DisabledEmailService` which logs what would be sent:

```
[EMAIL DISABLED] Would send password reset email:
  To: user@example.com
  Token: AaB1CcD2EeF3...
  Expiration: 1 hours
  Link: (would be generated based on config)
```

### Integration Testing

For integration tests, you can:

1. Use a test SMTP server (e.g., MailHog, smtp4dev)
2. Use the `DisabledEmailService` and verify tokens are created
3. Mock the `EmailService` interface in unit tests

## Best Practices

1. **Always use environment variables** for sensitive credentials (never commit passwords)
2. **Enable TLS/SSL** for production SMTP connections
3. **Use app-specific passwords** for Gmail and similar providers
4. **Monitor email delivery rates** and set up alerts for failures
5. **Test email rendering** across multiple email clients
6. **Keep templates updated** with current branding and copy
7. **Implement email rate limiting** to prevent abuse
8. **Use dedicated email service** (SendGrid, AWS SES) for production instead of Gmail

## Troubleshooting

### Emails Not Sending

1. Check `EMAIL_ENABLED=true` and `EMAIL_PROVIDER=smtp`
2. Verify SMTP credentials and host/port
3. Check firewall rules allow outbound SMTP connections
4. Review logs for authentication errors
5. Test SMTP connection using `telnet` or `openssl s_client`

### Emails Going to Spam

1. Configure SPF, DKIM, and DMARC records
2. Use a reputable email service provider
3. Avoid spam trigger words in subject/body
4. Include proper unsubscribe links (if applicable)
5. Maintain good sender reputation

### HTML Rendering Issues

1. Use inline CSS styles (not `<style>` tags)
2. Test across major email clients (Gmail, Outlook, Apple Mail)
3. Provide plain text fallback
4. Use tables for layout (email clients have limited CSS support)
5. Keep images optimized and hosted reliably

## Future Enhancements

- [ ] Email template system with customizable designs
- [ ] Multi-language support for email content
- [ ] Email queue for retry on transient failures
- [ ] Analytics tracking (open rates, click rates)
- [ ] Template versioning and A/B testing
- [ ] Support for additional providers (SendGrid API, AWS SES API)
- [ ] Unsubscribe management system
- [ ] Email preview/testing endpoints
