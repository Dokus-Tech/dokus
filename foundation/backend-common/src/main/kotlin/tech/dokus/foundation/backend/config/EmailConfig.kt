package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

data class EmailConfig(
    val resend: ResendConfig,
    val fromAddress: String,
    val templates: TemplatesConfig,
) {
    data class ResendConfig(
        val apiKey: String,
    )

    data class TemplatesConfig(
        val baseUrl: String,
        val supportEmail: String,
        val notificationPreferencesPath: String,
    )

    val resendApiKey: String get() = resend.apiKey
    val baseUrl: String get() = templates.baseUrl
    val supportEmail: String get() = templates.supportEmail
    val notificationPreferencesPath: String get() = templates.notificationPreferencesPath

    companion object {
        fun fromConfig(config: Config): EmailConfig {
            val resendConfig = config.getConfig("resend")
            val templatesConfig = config.getConfig("templates")

            val apiKey = resendConfig.getString("apiKey")
                .trim()
                .takeIf { it.isNotBlank() }
                ?: error("email.resend.apiKey is blank. Set RESEND_API_KEY environment variable.")

            return EmailConfig(
                resend = ResendConfig(apiKey = apiKey),
                fromAddress = config.getString("fromAddress"),
                templates = TemplatesConfig(
                    baseUrl = templatesConfig.getString("baseUrl").trim().trimEnd('/'),
                    supportEmail = templatesConfig.getString("supportEmail").trim(),
                    notificationPreferencesPath = templatesConfig.getString("notificationPreferencesPath")
                )
            )
        }
    }
}
