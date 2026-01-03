package tech.dokus.backend.services.auth

import com.typesafe.config.Config
import tech.dokus.foundation.backend.config.AppBaseConfig

/**
 * Configuration for email service.
 *
 * Supports environment variable overrides for sensitive values like passwords.
 */
data class EmailConfig(
    val enabled: Boolean,
    val provider: String, // "smtp" or "disabled"
    val smtp: SmtpConfig,
    val from: EmailAddress,
    val replyTo: EmailAddress?,
    val templates: EmailTemplateConfig
) {
    data class SmtpConfig(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val enableTls: Boolean,
        val enableAuth: Boolean,
        val connectionTimeout: Long,
        val timeout: Long
    )

    data class EmailAddress(
        val email: String,
        val name: String
    )

    data class EmailTemplateConfig(
        val baseUrl: String, // Base URL for email links (e.g., "https://app.dokus.ai")
        val supportEmail: String
    )

    companion object {
        fun fromConfig(config: Config): EmailConfig {
            val smtpConfig = config.getConfig("smtp")
            val fromConfig = config.getConfig("from")
            val templatesConfig = config.getConfig("templates")

            return EmailConfig(
                enabled = config.getBoolean("enabled"),
                provider = config.getString("provider"),
                smtp = SmtpConfig(
                    host = smtpConfig.getString("host"),
                    port = smtpConfig.getInt("port"),
                    username = smtpConfig.getString("username"),
                    password = smtpConfig.getString("password"),
                    enableTls = smtpConfig.getBoolean("enableTls"),
                    enableAuth = smtpConfig.getBoolean("enableAuth"),
                    connectionTimeout = if (smtpConfig.hasPath("connectionTimeout")) {
                        smtpConfig.getLong("connectionTimeout")
                    } else {
                        10000L
                    },
                    timeout = if (smtpConfig.hasPath("timeout")) {
                        smtpConfig.getLong("timeout")
                    } else {
                        10000L
                    }
                ),
                from = EmailAddress(
                    email = fromConfig.getString("email"),
                    name = fromConfig.getString("name")
                ),
                replyTo = if (config.hasPath("replyTo")) {
                    val replyToConfig = config.getConfig("replyTo")
                    EmailAddress(
                        email = replyToConfig.getString("email"),
                        name = replyToConfig.getString("name")
                    )
                } else {
                    null
                },
                templates = EmailTemplateConfig(
                    baseUrl = templatesConfig.getString("baseUrl"),
                    supportEmail = templatesConfig.getString("supportEmail")
                )
            )
        }

        fun load(baseConfig: AppBaseConfig): EmailConfig {
            // Reuse already loaded config from baseConfig
            return fromConfig(baseConfig.config.getConfig("email"))
        }
    }
}
