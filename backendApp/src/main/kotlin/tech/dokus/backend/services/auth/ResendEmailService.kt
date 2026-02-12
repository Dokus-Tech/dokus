package tech.dokus.backend.services.auth

import com.resend.Resend
import com.resend.services.emails.model.CreateEmailOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tech.dokus.foundation.backend.config.EmailConfig
import tech.dokus.foundation.backend.utils.loggerFor

class ResendEmailService(
    private val config: EmailConfig
) : EmailService {

    private val logger = loggerFor()
    private val resend = Resend(config.resendApiKey)

    override suspend fun send(
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val params = CreateEmailOptions.builder()
                .from(config.fromAddress)
                .to(to)
                .subject(subject)
                .html(htmlBody)
                .text(textBody)
                .build()

            resend.emails().send(params)
        }.onSuccess {
            logger.info("Email sent via Resend to {}", maskEmail(to))
        }.onFailure { error ->
            logger.error("Failed to send email via Resend to {}", maskEmail(to), error)
        }.map { Unit }
    }

    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 1) {
            return "***"
        }

        val name = email.substring(0, atIndex)
        val domain = email.substring(atIndex)
        return "${name.first()}***$domain"
    }
}
