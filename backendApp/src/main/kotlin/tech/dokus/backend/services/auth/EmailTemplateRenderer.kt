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
    companion object {
        private const val GuideUrl = "https://dokus.tech/guide"
        private const val SignatureImageUrl = "https://dokus.tech/team/artem.png"
    }

    fun renderWelcomeWorkspaceActive(
        userName: String,
        tenantName: String,
        peppolConnected: Boolean
    ): EmailTemplate {
        val normalizedUserName = userName.trim().ifBlank { "there" }
        val normalizedTenantName = tenantName.trim().ifBlank { "your workspace" }
        val subject = "$normalizedTenantName — workspace active"

        val html = if (peppolConnected) {
            renderWelcomeConnectedHtml(
                userName = normalizedUserName,
                tenantName = normalizedTenantName
            )
        } else {
            renderWelcomeNotConnectedHtml(
                userName = normalizedUserName,
                tenantName = normalizedTenantName
            )
        }

        val text = if (peppolConnected) {
            """
            $normalizedUserName, the workspace for $normalizedTenantName is active.

            PEPPOL is connected. Invoices and bills from the network are already being processed — amounts, VAT, contacts, due dates. No manual entry. When Dokus is certain, everything is handled. When something needs your judgment, it will wait for you.

            For receipts, contracts, or anything outside PEPPOL — share it from any app on your phone or upload it directly. A short guide with practical tips is at $GuideUrl.

            I read every reply to this address. If something is wrong, missing, or unclear — tell me directly.

            Artem
            Founder, Dokus
            """.trimIndent()
        } else {
            """
            $normalizedUserName, the workspace for $normalizedTenantName is active.

            Share a document from any app on your phone, or upload it directly — an invoice, a bill, a receipt. Dokus reads it and handles amounts, VAT, contacts, and due dates. No manual entry. When it is certain, everything is processed. When something needs your judgment, it will wait for you.

            When you connect PEPPOL, documents will arrive from the network automatically. A short guide covering uploads, phone integration, and PEPPOL setup is at $GuideUrl.

            I read every reply to this address. If something is wrong, missing, or unclear — tell me directly.

            Artem
            Founder, Dokus
            """.trimIndent()
        }

        return EmailTemplate(
            subject = subject,
            htmlBody = html,
            textBody = text
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
            details = filteredDetails.ifEmpty { fallbackDetails },
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

    private fun renderWelcomeConnectedHtml(
        userName: String,
        tenantName: String
    ): String {
        val safeUserName = escapeHtml(userName)
        val safeTenantName = escapeHtml(tenantName)
        return """
            <!DOCTYPE html>
            <html>
              <body style="margin:0; padding:0; background-color:#ffffff;">
                <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#ffffff;">
                  <tr>
                    <td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" border="0" style="padding:48px 24px; font-family:-apple-system,BlinkMacSystemFont,'Helvetica Neue',Helvetica,Arial,sans-serif; color:#111111;">
                        <tr>
                          <td style="padding-bottom:40px;">
                            <span style="font-size:15px; font-weight:600; letter-spacing:0.5px; color:#111111;">DOKUS</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="font-size:15px; line-height:1.8; color:#111111;">
                            $safeUserName, the workspace for $safeTenantName is active.<br/><br/>
                            PEPPOL is connected. Invoices and bills from the network are already being processed &mdash; amounts, VAT, contacts, due dates. No manual entry. When Dokus is certain, everything is handled. When something needs your judgment, it will wait for you.<br/><br/>
                            For receipts, contracts, or anything outside PEPPOL &mdash; share it from any app on your phone or upload it directly. A short guide with practical tips is at <a href="$GuideUrl" style="color:#111111; text-decoration:underline;">dokus.tech/guide</a>.<br/><br/>
                            I read every reply to this address. If something is wrong, missing, or unclear &mdash; tell me directly.
                          </td>
                        </tr>
                        <tr>
                          <td height="48"></td>
                        </tr>
                        <tr>
                          <td>
                            <table cellpadding="0" cellspacing="0" border="0">
                              <tr>
                                <td style="vertical-align:top; padding-right:16px;">
                                  <img src="$SignatureImageUrl" width="72" height="72" style="display:block; border-radius:4px;" alt="Artem"/>
                                </td>
                                <td style="vertical-align:top; font-size:13px; line-height:1.6; color:#555555;">
                                  <strong style="font-weight:600; color:#111111;">Artem</strong><br/>
                                  Founder, Dokus<br/>
                                  <a href="https://dokus.tech" style="color:#555555; text-decoration:none;">dokus.tech</a>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()
    }

    private fun renderWelcomeNotConnectedHtml(
        userName: String,
        tenantName: String
    ): String {
        val safeUserName = escapeHtml(userName)
        val safeTenantName = escapeHtml(tenantName)
        return """
            <!DOCTYPE html>
            <html>
              <body style="margin:0; padding:0; background-color:#ffffff;">
                <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#ffffff;">
                  <tr>
                    <td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" border="0" style="padding:48px 24px; font-family:-apple-system,BlinkMacSystemFont,'Helvetica Neue',Helvetica,Arial,sans-serif; color:#111111;">
                        <tr>
                          <td style="padding-bottom:40px;">
                            <span style="font-size:15px; font-weight:600; letter-spacing:0.5px; color:#111111;">DOKUS</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="font-size:15px; line-height:1.8; color:#111111;">
                            $safeUserName, the workspace for $safeTenantName is active.<br/><br/>
                            Share a document from any app on your phone, or upload it directly &mdash; an invoice, a bill, a receipt. Dokus reads it and handles amounts, VAT, contacts, and due dates. No manual entry. When it is certain, everything is processed. When something needs your judgment, it will wait for you.<br/><br/>
                            When you connect PEPPOL, documents will arrive from the network automatically. A short guide covering uploads, phone integration, and PEPPOL setup is at <a href="$GuideUrl" style="color:#111111; text-decoration:underline;">dokus.tech/guide</a>.<br/><br/>
                            I read every reply to this address. If something is wrong, missing, or unclear &mdash; tell me directly.
                          </td>
                        </tr>
                        <tr>
                          <td height="48"></td>
                        </tr>
                        <tr>
                          <td>
                            <table cellpadding="0" cellspacing="0" border="0">
                              <tr>
                                <td style="vertical-align:top; padding-right:16px;">
                                  <img src="$SignatureImageUrl" width="72" height="72" style="display:block; border-radius:4px;" alt="Artem"/>
                                </td>
                                <td style="vertical-align:top; font-size:13px; line-height:1.6; color:#555555;">
                                  <strong style="font-weight:600; color:#111111;">Artem</strong><br/>
                                  Founder, Dokus<br/>
                                  <a href="https://dokus.tech" style="color:#555555; text-decoration:none;">dokus.tech</a>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
        """.trimIndent()
    }
}
