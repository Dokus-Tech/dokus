package tech.dokus.backend.auth

import org.junit.jupiter.api.Test
import tech.dokus.backend.services.auth.EmailTemplateRenderer
import tech.dokus.foundation.backend.config.EmailConfig
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EmailTemplateRendererVerificationTemplateTest {
    private val renderer = EmailTemplateRenderer(
        config = EmailConfig(
            resend = EmailConfig.ResendConfig(apiKey = "re_test"),
            fromAddress = "Dokus <noreply@dokus.tech>",
            welcome = EmailConfig.WelcomeConfig(
                fromAddress = "Artem <artem@dokus.tech>",
                replyToAddress = "Artem <artem@dokus.tech>"
            ),
            templates = EmailConfig.TemplatesConfig(
                baseUrl = "https://app.dokus.tech",
                supportEmail = "support@dokus.tech",
                notificationPreferencesPath = "/account/notifications"
            )
        )
    )

    @Test
    fun `verification template uses provided html structure and copy`() {
        val template = renderer.renderEmailVerification(
            verificationToken = "verify-123",
            expirationHours = 24
        )
        val expectedUrl = "https://app.dokus.tech/auth/verify-email?token=verify-123"

        assertEquals("Confirm your email address", template.subject)
        assertContains(template.htmlBody, "<!DOCTYPE html>")
        assertContains(template.htmlBody, "Confirm your email address")
        assertContains(template.htmlBody, "To activate your Dokus workspace, please confirm your email address.")
        assertContains(template.htmlBody, "Click the button below to continue.")
        assertContains(template.htmlBody, "<a href=\"$expectedUrl\"")
        assertContains(template.htmlBody, "Confirm email")
        assertContains(template.htmlBody, "If the button does not work, copy and paste the link below into your browser:")
        assertContains(template.htmlBody, expectedUrl)
        assertContains(template.htmlBody, "dokus.tech")

        assertContains(template.textBody, "Confirm your email address")
        assertContains(template.textBody, "To activate your Dokus workspace, please confirm your email address.")
        assertContains(template.textBody, expectedUrl)

        assertFalse(template.htmlBody.contains("background:#0f1115"))
        assertFalse(template.htmlBody.contains("Open in Dokus"))
        assertFalse(template.htmlBody.contains("Notification preferences"))
    }

    @Test
    fun `verification template escapes injected url in html`() {
        val template = renderer.renderEmailVerification(
            verificationToken = "verify-\"<script>",
            expirationHours = 24
        )

        assertContains(template.htmlBody, "verify-&quot;&lt;script&gt;")
        assertFalse(template.htmlBody.contains("verify-\"<script>"))
    }
}
