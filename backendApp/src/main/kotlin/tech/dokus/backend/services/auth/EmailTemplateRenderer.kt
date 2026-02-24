package tech.dokus.backend.services.auth

import tech.dokus.domain.enums.Language
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
        peppolConnected: Boolean,
        language: Language = Language.En
    ): EmailTemplate {
        val normalizedUserName = userName.trim().ifBlank { "there" }
        val normalizedTenantName = tenantName.trim().ifBlank { "Dokus" }
        val copy = welcomeEmailCopy(language)
        val subject = "$normalizedTenantName — ${copy.workspaceActiveSuffix}"
        val openingLine = copy.openingLine(
            userName = normalizedUserName,
            tenantName = normalizedTenantName
        )
        val rawParagraphs = if (peppolConnected) {
            copy.connectedParagraphs
        } else {
            copy.notConnectedParagraphs
        }

        val guideAnchor = """<a href="$GuideUrl" style="color:#111111; text-decoration:underline;">dokus.tech/guide</a>"""
        val htmlParagraphs = rawParagraphs.map { paragraph ->
            replaceGuidePlaceholderWithAnchor(
                text = escapeHtml(paragraph),
                anchorHtml = guideAnchor
            )
        }
        val textParagraphs = rawParagraphs.map { replaceGuidePlaceholder(it, GuideUrl) }

        val html = renderWelcomeHtml(
            openingLine = openingLine,
            bodyParagraphsHtml = htmlParagraphs,
            signatureRole = copy.signatureRole
        )
        val text = buildString {
            appendLine(openingLine)
            appendLine()
            textParagraphs.forEachIndexed { index, paragraph ->
                appendLine(paragraph)
                if (index != textParagraphs.lastIndex) {
                    appendLine()
                }
            }
            appendLine()
            appendLine("Artem")
            appendLine(copy.signatureRole)
        }

        return EmailTemplate(
            subject = subject,
            htmlBody = html,
            textBody = text
        )
    }

    fun renderPasswordReset(resetToken: String, expirationHours: Int): EmailTemplate {
        val subject = "Reset your password"
        val resetUrl = absoluteUrl("/auth/reset-password?token=$resetToken")
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
        val subject = "Confirm your email address"
        val verificationUrl = absoluteUrl("/auth/verify-email?token=$verificationToken")
        val safeVerificationUrl = escapeHtml(verificationUrl)

        val html = """
            <!DOCTYPE html>
            <html>
              <body style="margin:0; padding:0; background-color:#ffffff;">
                <table width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#ffffff;">
                  <tr>
                    <td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" border="0"
                             style="padding:40px 20px; font-family:-apple-system,BlinkMacSystemFont,'Helvetica Neue',Helvetica,Arial,sans-serif; color:#111111;">

                        <!-- Title -->
                        <tr>
                          <td style="font-size:20px; font-weight:600; padding-bottom:24px;">
                            Confirm your email address
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="font-size:16px; line-height:1.6;">
                            To activate your Dokus workspace, please confirm your email address.
                            <br/><br/>
                            Click the button below to continue.
                          </td>
                        </tr>

                        <!-- Spacing -->
                        <tr>
                          <td height="32"></td>
                        </tr>

                        <!-- Button -->
                        <tr>
                          <td align="left">
                            <a href="$safeVerificationUrl"
                               style="
                                 display:inline-block;
                                 padding:12px 20px;
                                 font-size:14px;
                                 font-weight:500;
                                 text-decoration:none;
                                 color:#ffffff;
                                 background-color:#111111;
                                 border-radius:6px;
                               ">
                              Confirm email
                            </a>
                          </td>
                        </tr>

                        <!-- Spacing -->
                        <tr>
                          <td height="32"></td>
                        </tr>

                        <!-- Fallback -->
                        <tr>
                          <td style="font-size:13px; line-height:1.6; color:#555555;">
                            This link expires in $expirationHours hour(s).
                            <br/><br/>
                            If the button does not work, copy and paste the link below into your browser:
                            <br/><br/>
                            <span style="word-break:break-all;">
                              $safeVerificationUrl
                            </span>
                          </td>
                        </tr>

                        <!-- Footer spacing -->
                        <tr>
                          <td height="48"></td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="font-size:13px; color:#777777;">
                            Dokus<br/>
                            <a href="https://dokus.tech" style="color:#777777; text-decoration:none;">
                              dokus.tech
                            </a>
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
            appendLine("To activate your Dokus workspace, please confirm your email address.")
            appendLine()
            appendLine("Confirm your email address by opening the following link:")
            appendLine(verificationUrl)
            appendLine()
            appendLine("This link expires in $expirationHours hour(s).")
            appendLine()
            appendLine("Dokus")
            appendLine("dokus.tech")
        }

        return EmailTemplate(
            subject = subject,
            htmlBody = html,
            textBody = text
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
        val safeCtaUrl = escapeHtml(ctaUrl)
        val safeCtaText = escapeHtml(ctaText)
        val safePreferencesUrl = escapeHtml(preferencesUrl)

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
                          <a href="$safeCtaUrl" style="display:inline-block;background:#dce7ff;color:#101522;text-decoration:none;padding:10px 16px;border-radius:8px;font-weight:600;">$safeCtaText</a>
                          <div style="margin-top:28px;padding-top:14px;border-top:1px solid #252b38;color:#9aa3b2;font-size:12px;">
                            dokus.tech - <a href="$safePreferencesUrl" style="color:#9fb3ff;text-decoration:none;">Notification preferences</a>
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

    private fun renderWelcomeHtml(
        openingLine: String,
        bodyParagraphsHtml: List<String>,
        signatureRole: String
    ): String {
        val detailsHtml = buildString {
            append(escapeHtml(openingLine))
            append("<br/><br/>")
            bodyParagraphsHtml.forEachIndexed { index, paragraph ->
                append(paragraph)
                if (index != bodyParagraphsHtml.lastIndex) {
                    append("<br/><br/>")
                }
            }
        }
        val safeSignatureRole = escapeHtml(signatureRole)

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
                            $detailsHtml
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
                                  $safeSignatureRole<br/>
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
