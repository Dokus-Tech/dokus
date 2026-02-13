package tech.dokus.backend.services.auth

interface EmailService {
    suspend fun send(
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String,
        fromAddress: String? = null,
        replyToAddress: String? = null
    ): Result<Unit>
}
