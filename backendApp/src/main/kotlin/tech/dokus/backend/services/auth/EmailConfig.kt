package tech.dokus.backend.services.auth

import com.typesafe.config.Config

data class EmailConfig(
    val resendApiKey: String,
    val fromAddress: String,
    val baseUrl: String,
    val supportEmail: String,
    val notificationPreferencesPath: String,
) {
    companion object {
        private const val DefaultFromAddress = "Dokus <noreply@dokus.tech>"
        private const val DefaultPreferencesPath = "/settings/notifications"

        fun load(config: Config): EmailConfig {
            val emailConfig = if (config.hasPath("email")) config.getConfig("email") else config
            val templatesConfig = emailConfig.getConfig("templates")

            val resendApiKey = System.getenv("RESEND_API_KEY")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: error("RESEND_API_KEY must be set")

            val fromAddress = if (emailConfig.hasPath("fromAddress")) {
                emailConfig.getString("fromAddress")
            } else {
                DefaultFromAddress
            }

            val baseUrl = templatesConfig.getString("baseUrl").trim().trimEnd('/')
            val supportEmail = templatesConfig.getString("supportEmail").trim()
            val preferencesPath = if (templatesConfig.hasPath("notificationPreferencesPath")) {
                templatesConfig.getString("notificationPreferencesPath")
            } else {
                DefaultPreferencesPath
            }

            return EmailConfig(
                resendApiKey = resendApiKey,
                fromAddress = fromAddress,
                baseUrl = baseUrl,
                supportEmail = supportEmail,
                notificationPreferencesPath = preferencesPath
            )
        }
    }
}
