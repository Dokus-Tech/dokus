package tech.dokus.backend.auth

import org.junit.jupiter.api.Test
import tech.dokus.backend.services.auth.EmailTemplateRenderer
import tech.dokus.domain.enums.Language
import tech.dokus.foundation.backend.config.EmailConfig
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EmailTemplateRendererWelcomeLocalizationTest {
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
    fun `welcome templates are localized for all app languages`() {
        Language.entries.forEach { language ->
            val connected = renderer.renderWelcomeWorkspaceActive(
                userName = "Jan",
                tenantName = "Janssens BV",
                peppolConnected = true,
                language = language
            )
            val notConnected = renderer.renderWelcomeWorkspaceActive(
                userName = "Jan",
                tenantName = "Janssens BV",
                peppolConnected = false,
                language = language
            )

            assertTrue(connected.subject.isNotBlank(), "Connected subject is blank for $language")
            assertEquals(connected.subject, notConnected.subject, "Subject must be the same across variants for $language")
            assertContains(connected.subject, "Janssens BV")

            assertContains(connected.htmlBody, "https://dokus.tech/guide")
            assertContains(notConnected.htmlBody, "https://dokus.tech/guide")
            assertContains(connected.textBody, "https://dokus.tech/guide")
            assertContains(notConnected.textBody, "https://dokus.tech/guide")
            assertContains(connected.htmlBody, "https://dokus.tech/team/artem.png")

            assertNotEquals(connected.textBody, notConnected.textBody, "Connected and not-connected bodies must differ for $language")
        }
    }
}
