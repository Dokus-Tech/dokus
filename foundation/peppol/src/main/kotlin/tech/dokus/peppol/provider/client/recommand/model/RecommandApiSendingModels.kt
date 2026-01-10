package tech.dokus.peppol.provider.client.recommand.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Request body for sending a document.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`)
 */
@Serializable
data class RecommandSendDocumentRequest(
    val recipient: String,
    val email: RecommandEmail? = null,
    val pdfGeneration: RecommandPdfGeneration? = null,
    val documentType: RecommandDocumentType,
    /**
     * One of:
     * - `RecommandSendInvoice`
     * - `RecommandSendCreditNote`
     * - `RecommandSendSelfBillingInvoice`
     * - `RecommandSendSelfBillingCreditNote`
     * - `RecommandSendMessageLevelResponse`
     * - `RecommandXmlDocument`
     */
    val document: JsonElement,
    val doctypeId: String? = null,
    val processId: String? = null,
)

/**
 * Success response for sending a document.
 *
 * Returned by:
 * - `POST /api/v1/{companyId}/send` (path params: `companyId`) with HTTP 200
 */
@Serializable
data class RecommandSendDocumentResponse(
    val success: Boolean,
    val sentOverPeppol: Boolean,
    val sentOverEmail: Boolean,
    val emailRecipients: List<String>,
    val teamId: String,
    val companyId: String,
    val id: String,
    val peppolMessageId: String? = null,
    val envelopeId: String? = null,
)

