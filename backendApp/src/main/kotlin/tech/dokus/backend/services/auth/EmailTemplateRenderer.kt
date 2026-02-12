package tech.dokus.backend.services.auth

import tech.dokus.domain.enums.NotificationType
import tech.dokus.foundation.backend.config.EmailConfig

data class EmailTemplate(
    val subject: String,
    val htmlBody: String,
    val textBody: String
)

class EmailTemplateRenderer(
    private val config: EmailConfig
) {

    fun renderWelcome(): EmailTemplate {
        val subject = "Dokus is ready"
        val details = listOf(
            "Your account is active. Documents can now flow in via upload, email forward, or PEPPOL.",
            "If nothing needs your attention, you will not hear from us."
        )
        return renderTemplate(
            subject = subject,
            details = details,
            ctaText = "Open in Dokus",
            ctaUrl = absoluteUrl("/")
        )
    }

    fun renderPasswordReset(resetToken: String, expirationHours: Int): EmailTemplate {
        val subject = "Reset your password"
        val resetUrl = absoluteUrl("/reset-password?token=$resetToken")
        val details = listOf(
            "A password reset was requested for your account.",
            "This link expires in $expirationHours hour(s)."
        )
        return renderTemplate(
            subject = subject,
            details = details,
            ctaText = "Open in Dokus",
            ctaUrl = resetUrl
        )
    }

    fun renderEmailVerification(verificationToken: String, expirationHours: Int): EmailTemplate {
        val subject = "Verify your email address"
        val verificationUrl = absoluteUrl("/verify-email?token=$verificationToken")
        val details = listOf(
            "Confirm this email address to continue using Dokus.",
            "This link expires in $expirationHours hour(s)."
        )
        return renderTemplate(
            subject = subject,
            details = details,
            ctaText = "Open in Dokus",
            ctaUrl = verificationUrl
        )
    }

    fun renderNotification(
        type: NotificationType,
        title: String,
        details: List<String>,
        openPath: String
    ): EmailTemplate {
        val filteredDetails = details.map { it.trim() }.filter { it.isNotBlank() }
        val fallbackDetails = when (type) {
            NotificationType.PeppolSendFailed -> listOf("A PEPPOL transmission failed and needs attention.")
            NotificationType.ComplianceBlocker -> listOf("A compliance blocker is preventing export.")
            NotificationType.PaymentFailed -> listOf("Payment could not be processed and needs action.")
            NotificationType.PeppolReceived -> listOf("A new PEPPOL document was received.")
            NotificationType.PeppolSendConfirmed -> listOf("A PEPPOL transmission was confirmed.")
            NotificationType.VatWarning -> listOf("A VAT warning was detected.")
            NotificationType.PaymentConfirmed -> listOf("Payment has been confirmed.")
            NotificationType.SubscriptionChanged -> listOf("Your subscription has changed.")
        }

        return renderTemplate(
            subject = title,
            details = if (filteredDetails.isEmpty()) fallbackDetails else filteredDetails,
            ctaText = "Open in Dokus",
            ctaUrl = absoluteUrl(openPath)
        )
    }

    private fun renderTemplate(
        subject: String,
        details: List<String>,
        ctaText: String,
        ctaUrl: String
    ): EmailTemplate {
        val safeSubject = escapeHtml(subject)
        val safeDetails = details.map(::escapeHtml)
        val detailsHtml = safeDetails.joinToString(separator = "<br><br>")
        val preferencesUrl = absoluteUrl(config.notificationPreferencesPath)

        val html = """
            <!doctype html>
            <html lang="en">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>$safeSubject</title>
            </head>
            <body style="margin:0;padding:0;background:#0f1115;color:#e7eaf0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
              <table role="presentation" width="100%" cellspacing="0" cellpadding="0" style="padding:24px 0;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" cellspacing="0" cellpadding="0" style="max-width:600px;width:100%;background:#151922;border:1px solid #252b38;border-radius:10px;overflow:hidden;">
                      <tr>
                        <td style="padding:24px 28px;font-size:15px;line-height:1.6;">
                          <div style="font-size:18px;font-weight:600;letter-spacing:0.2px;margin-bottom:20px;">Dokus</div>
                          <div style="font-size:20px;line-height:1.35;font-weight:600;margin-bottom:16px;">$safeSubject</div>
                          <div style="color:#c8ceda;margin-bottom:24px;">$detailsHtml</div>
                          <a href="$ctaUrl" style="display:inline-block;background:#dce7ff;color:#101522;text-decoration:none;padding:10px 16px;border-radius:8px;font-weight:600;">$ctaText</a>
                          <div style="margin-top:28px;padding-top:14px;border-top:1px solid #252b38;color:#9aa3b2;font-size:12px;">
                            dokus.tech - <a href="$preferencesUrl" style="color:#9fb3ff;text-decoration:none;">Notification preferences</a>
                          </div>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
        """.trimIndent()

        val text = buildString {
            appendLine(subject)
            appendLine()
            details.forEachIndexed { index, line ->
                appendLine(line)
                if (index != details.lastIndex) {
                    appendLine()
                }
            }
            appendLine()
            appendLine("Open in Dokus: $ctaUrl")
            appendLine("Notification preferences: $preferencesUrl")
            appendLine("dokus.tech")
        }

        return EmailTemplate(
            subject = subject,
            htmlBody = html,
            textBody = text
        )
    }

    private fun absoluteUrl(path: String): String {
        val base = config.baseUrl.trimEnd('/')
        if (path.isBlank() || path == "/") {
            return base
        }
        val normalizedPath = if (path.startsWith('/')) path else "/$path"
        return "$base$normalizedPath"
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
