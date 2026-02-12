package tech.dokus.backend.services.auth

interface EmailService {
    suspend fun send(
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String
    ): Result<Unit>
}
