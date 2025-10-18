package ai.dokus.foundation.database.services

import org.slf4j.LoggerFactory

/**
 * Email service interface
 * Supports sending transactional emails via SendGrid, AWS SES, or other providers
 */
interface EmailService {
    /**
     * Send an email
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlBody Email body in HTML format
     * @param textBody Email body in plain text format (fallback)
     * @param from Sender email address (optional, uses default if not provided)
     * @param replyTo Reply-to email address (optional)
     * @param attachments List of attachments (optional)
     */
    suspend fun sendEmail(
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String? = null,
        from: String? = null,
        replyTo: String? = null,
        attachments: List<EmailAttachment> = emptyList()
    )

    /**
     * Send an email to multiple recipients
     */
    suspend fun sendBulkEmail(
        recipients: List<String>,
        subject: String,
        htmlBody: String,
        textBody: String? = null,
        from: String? = null,
        replyTo: String? = null
    )
}

/**
 * Email attachment data
 */
data class EmailAttachment(
    val filename: String,
    val content: ByteArray,
    val contentType: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EmailAttachment

        if (filename != other.filename) return false
        if (!content.contentEquals(other.content)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = filename.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}

/**
 * SendGrid email service implementation
 */
class SendGridEmailService(
    private val apiKey: String,
    private val defaultFromEmail: String,
    private val defaultFromName: String = "Dokus"
) : EmailService {

    private val logger = LoggerFactory.getLogger(SendGridEmailService::class.java)

    override suspend fun sendEmail(
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String?,
        from: String?,
        replyTo: String?,
        attachments: List<EmailAttachment>
    ) {
        logger.info("Sending email via SendGrid: to=$to, subject=$subject")

        // TODO: Implement SendGrid integration
        // val sendGridEmail = Mail()
        // sendGridEmail.setFrom(Email(from ?: defaultFromEmail, defaultFromName))
        // sendGridEmail.addPersonalization(
        //     Personalization().apply {
        //         addTo(Email(to))
        //         replyTo?.let { setReplyTo(Email(it)) }
        //     }
        // )
        // sendGridEmail.setSubject(subject)
        // sendGridEmail.addContent(Content("text/html", htmlBody))
        // textBody?.let { sendGridEmail.addContent(Content("text/plain", it)) }
        //
        // attachments.forEach { attachment ->
        //     sendGridEmail.addAttachments(
        //         Attachments().apply {
        //             setContent(Base64.getEncoder().encodeToString(attachment.content))
        //             setType(attachment.contentType)
        //             setFilename(attachment.filename)
        //             setDisposition("attachment")
        //         }
        //     )
        // }
        //
        // val sg = SendGrid(apiKey)
        // val request = Request()
        // request.method = Method.POST
        // request.endpoint = "mail/send"
        // request.body = sendGridEmail.build()
        // val response = sg.api(request)
        //
        // if (response.statusCode !in 200..299) {
        //     throw RuntimeException("SendGrid API error: ${response.statusCode} - ${response.body}")
        // }

        throw NotImplementedError("SendGrid email service not yet configured - add SendGrid dependency to build.gradle.kts")
    }

    override suspend fun sendBulkEmail(
        recipients: List<String>,
        subject: String,
        htmlBody: String,
        textBody: String?,
        from: String?,
        replyTo: String?
    ) {
        logger.info("Sending bulk email via SendGrid: recipients=${recipients.size}, subject=$subject")

        // For simplicity, send individual emails
        // In production, use SendGrid's batch sending API for better performance
        recipients.forEach { recipient ->
            sendEmail(
                to = recipient,
                subject = subject,
                htmlBody = htmlBody,
                textBody = textBody,
                from = from,
                replyTo = replyTo
            )
        }
    }
}

/**
 * AWS SES email service implementation
 */
class AwsSesEmailService(
    private val accessKey: String,
    private val secretKey: String,
    private val region: String,
    private val defaultFromEmail: String,
    private val defaultFromName: String = "Dokus"
) : EmailService {

    private val logger = LoggerFactory.getLogger(AwsSesEmailService::class.java)

    override suspend fun sendEmail(
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String?,
        from: String?,
        replyTo: String?,
        attachments: List<EmailAttachment>
    ) {
        logger.info("Sending email via AWS SES: to=$to, subject=$subject")

        // TODO: Implement AWS SES integration
        // val sesClient = SesClient.builder()
        //     .region(Region.of(region))
        //     .credentialsProvider(
        //         StaticCredentialsProvider.create(
        //             AwsBasicCredentials.create(accessKey, secretKey)
        //         )
        //     )
        //     .build()
        //
        // val message = Message.builder()
        //     .subject(Content.builder().data(subject).build())
        //     .body(
        //         Body.builder()
        //             .html(Content.builder().data(htmlBody).build())
        //             .apply { textBody?.let { text(Content.builder().data(it).build()) } }
        //             .build()
        //     )
        //     .build()
        //
        // val emailRequest = SendEmailRequest.builder()
        //     .source(from ?: "$defaultFromName <$defaultFromEmail>")
        //     .destination(Destination.builder().toAddresses(to).build())
        //     .message(message)
        //     .apply { replyTo?.let { replyToAddresses(it) } }
        //     .build()
        //
        // sesClient.sendEmail(emailRequest)

        throw NotImplementedError("AWS SES email service not yet configured - add AWS SDK dependency to build.gradle.kts")
    }

    override suspend fun sendBulkEmail(
        recipients: List<String>,
        subject: String,
        htmlBody: String,
        textBody: String?,
        from: String?,
        replyTo: String?
    ) {
        logger.info("Sending bulk email via AWS SES: recipients=${recipients.size}, subject=$subject")

        // Send individual emails
        // In production, use SES batch sending API for better performance
        recipients.forEach { recipient ->
            sendEmail(
                to = recipient,
                subject = subject,
                htmlBody = htmlBody,
                textBody = textBody,
                from = from,
                replyTo = replyTo
            )
        }
    }
}

/**
 * Mock email service for testing
 * Logs emails instead of sending them
 */
class MockEmailService : EmailService {
    private val logger = LoggerFactory.getLogger(MockEmailService::class.java)
    private val sentEmails = mutableListOf<SentEmail>()

    data class SentEmail(
        val to: String,
        val subject: String,
        val htmlBody: String,
        val textBody: String?,
        val from: String?,
        val replyTo: String?,
        val attachments: List<EmailAttachment>
    )

    override suspend fun sendEmail(
        to: String,
        subject: String,
        htmlBody: String,
        textBody: String?,
        from: String?,
        replyTo: String?,
        attachments: List<EmailAttachment>
    ) {
        logger.info("MOCK EMAIL SENT: to=$to, subject=$subject, from=$from")
        logger.debug("Email body: ${htmlBody.take(200)}...")

        sentEmails.add(
            SentEmail(
                to = to,
                subject = subject,
                htmlBody = htmlBody,
                textBody = textBody,
                from = from,
                replyTo = replyTo,
                attachments = attachments
            )
        )
    }

    override suspend fun sendBulkEmail(
        recipients: List<String>,
        subject: String,
        htmlBody: String,
        textBody: String?,
        from: String?,
        replyTo: String?
    ) {
        logger.info("MOCK BULK EMAIL SENT: recipients=${recipients.size}, subject=$subject")

        recipients.forEach { recipient ->
            sendEmail(
                to = recipient,
                subject = subject,
                htmlBody = htmlBody,
                textBody = textBody,
                from = from,
                replyTo = replyTo
            )
        }
    }

    /**
     * Get all sent emails (for testing)
     */
    fun getSentEmails(): List<SentEmail> = sentEmails.toList()

    /**
     * Clear sent emails history (for testing)
     */
    fun clearSentEmails() = sentEmails.clear()
}

/**
 * Invoice email templates
 */
object InvoiceEmailTemplates {
    fun generateInvoiceEmail(
        clientName: String,
        invoiceNumber: String,
        totalAmount: String,
        dueDate: String,
        pdfUrl: String
    ): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                .content { padding: 20px; background-color: #f9f9f9; }
                .button { display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; text-decoration: none; border-radius: 5px; margin-top: 20px; }
                .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>New Invoice: $invoiceNumber</h1>
                </div>
                <div class="content">
                    <p>Dear $clientName,</p>
                    <p>Thank you for your business. Please find attached your invoice with the following details:</p>
                    <ul>
                        <li><strong>Invoice Number:</strong> $invoiceNumber</li>
                        <li><strong>Total Amount:</strong> $totalAmount</li>
                        <li><strong>Due Date:</strong> $dueDate</li>
                    </ul>
                    <p>You can download your invoice using the link below:</p>
                    <a href="$pdfUrl" class="button">Download Invoice PDF</a>
                </div>
                <div class="footer">
                    <p>This is an automated email. Please do not reply to this message.</p>
                    <p>&copy; 2025 Dokus. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()

    fun generatePaymentConfirmationEmail(
        clientName: String,
        invoiceNumber: String,
        amountPaid: String,
        paymentDate: String
    ): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { background-color: #2196F3; color: white; padding: 20px; text-align: center; }
                .content { padding: 20px; background-color: #f9f9f9; }
                .checkmark { font-size: 48px; color: #4CAF50; text-align: center; }
                .footer { text-align: center; padding: 20px; font-size: 12px; color: #666; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Payment Received</h1>
                </div>
                <div class="content">
                    <div class="checkmark">✓</div>
                    <p>Dear $clientName,</p>
                    <p>We have received your payment. Thank you!</p>
                    <ul>
                        <li><strong>Invoice Number:</strong> $invoiceNumber</li>
                        <li><strong>Amount Paid:</strong> $amountPaid</li>
                        <li><strong>Payment Date:</strong> $paymentDate</li>
                    </ul>
                    <p>Your payment has been successfully processed.</p>
                </div>
                <div class="footer">
                    <p>This is an automated email. Please do not reply to this message.</p>
                    <p>&copy; 2025 Dokus. All rights reserved.</p>
                </div>
            </div>
        </body>
        </html>
    """.trimIndent()
}
