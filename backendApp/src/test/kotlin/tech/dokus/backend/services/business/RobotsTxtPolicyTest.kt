package tech.dokus.backend.services.business

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RobotsTxtPolicyTest {
    @Test
    fun `disallow rule blocks path`() {
        val policy = RobotsTxtParser.parse(
            """
            User-agent: *
            Disallow: /private
            """.trimIndent()
        )

        assertFalse(policy.isAllowed("/private/docs"))
        assertTrue(policy.isAllowed("/public"))
    }

    @Test
    fun `allow with longer prefix overrides disallow`() {
        val policy = RobotsTxtParser.parse(
            """
            User-agent: *
            Disallow: /assets
            Allow: /assets/logo
            """.trimIndent()
        )

        assertTrue(policy.isAllowed("/assets/logo.svg"))
        assertFalse(policy.isAllowed("/assets/css/main.css"))
    }
}
