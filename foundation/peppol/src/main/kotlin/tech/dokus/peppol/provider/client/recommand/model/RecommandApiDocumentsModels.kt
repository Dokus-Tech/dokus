package tech.dokus.peppol.provider.client.recommand.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Document direction.
 *
 * Used in:
 * - `GET /api/v1/documents` response field `documents[].direction`
 * - `GET /api/v1/documents/{documentId}` response field `document.direction` (path params: `documentId`)
 * - `GET /api/v1/inbox` response field `documents[].direction`
 * - `GET /api/v1/documents` query param `direction`
 */
@Serializable
enum class RecommandDocumentDirection {
    @SerialName("incoming")
    Incoming,

    @SerialName("outgoing")
    Outgoing,
}

/**
 * Document type as returned by the API and used when sending documents.
 *
 * Used in:
 * - `POST /api/v1/{companyId}/send` request body field `documentType` (path params: `companyId`)
 * - `GET /api/v1/documents` response field `documents[].type`
 * - `GET /api/v1/documents/{documentId}` response field `document.type` (path params: `documentId`)
 * - `GET /api/v1/inbox` response field `documents[].type`
 */
@Serializable
enum class RecommandDocumentType {
    @SerialName("invoice")
    Invoice,

    @SerialName("creditNote")
    CreditNote,

    @SerialName("selfBillingInvoice")
    SelfBillingInvoice,

    @SerialName("selfBillingCreditNote")
    SelfBillingCreditNote,

    @SerialName("messageLevelResponse")
    MessageLevelResponse,

    @SerialName("xml")
    Xml,
}

/**
 * Document type filter used in `GET /api/v1/documents` query param `type`.
 */
@Serializable
enum class RecommandDocumentsTypeFilter {
    @SerialName("invoice")
    Invoice,

    @SerialName("creditNote")
    CreditNote,

    @SerialName("selfBillingInvoice")
    SelfBillingInvoice,

    @SerialName("selfBillingCreditNote")
    SelfBillingCreditNote,

    @SerialName("messageLevelResponse")
    MessageLevelResponse,

    @SerialName("unknown")
    Unknown,
}

/**
 * Document validation result.
 *
 * Used in:
 * - `GET /api/v1/documents` response field `documents[].validation.result`
 * - `GET /api/v1/documents/{documentId}` response field `document.validation.result` (path params: `documentId`)
 * - `GET /api/v1/inbox` response field `documents[].validation.result`
 */
@Serializable
enum class RecommandDocumentValidationResult {
    @SerialName("valid")
    Valid,

    @SerialName("invalid")
    Invalid,

    @SerialName("not_supported")
    NotSupported,

    @SerialName("error")
    Error,
}

/**
 * Validation error entry.
 *
 * Used in:
 * - `GET /api/v1/documents` response field `documents[].validation.errors[]`
 * - `GET /api/v1/documents/{documentId}` response field `document.validation.errors[]` (path params: `documentId`)
 * - `GET /api/v1/inbox` response field `documents[].validation.errors[]`
 */
@Serializable
data class RecommandDocumentValidationError(
    val ruleCode: String,
    val errorMessage: String,
    val errorLevel: String,
    val fieldName: String,
)

/**
 * Validation block for a document.
 */
@Serializable
data class RecommandDocumentValidation(
    val result: RecommandDocumentValidationResult,
    val errors: List<RecommandDocumentValidationError> = emptyList(),
)

/**
 * Label representation attached to documents/suppliers.
 *
 * Used in:
 * - `GET /api/v1/documents` response field `documents[].labels[]`
 * - `GET /api/v1/documents/{documentId}` response field `document.labels[]` (path params: `documentId`)
 * - `GET /api/v1/suppliers` response field `suppliers[].labels[]`
 * - `GET /api/v1/suppliers/{supplierId}` response field `supplier.labels[]` (path params: `supplierId`)
 */
@Serializable
data class RecommandDocumentLabel(
    val id: String,
    val externalId: String? = null,
    val name: String,
    val colorHex: String,
)

/**
 * Document summary returned in `GET /api/v1/documents`.
 */
@Serializable
data class RecommandDocumentSummary(
    val id: String,
    val teamId: String,
    val companyId: String,
    val direction: RecommandDocumentDirection,
    val senderId: String,
    val receiverId: String,
    val docTypeId: String,
    val processId: String,
    val countryC1: String,
    val type: RecommandDocumentType,
    val readAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    /**
     * One of:
     * - `RecommandInvoice`
     * - `RecommandCreditNote`
     * - `RecommandSelfBillingInvoice`
     * - `RecommandSelfBillingCreditNote`
     * - `null`
     */
    val parsed: JsonElement? = null,
    val validation: RecommandDocumentValidation,
    val sentOverPeppol: Boolean,
    val sentOverEmail: Boolean,
    val emailRecipients: List<String> = emptyList(),
    val labels: List<RecommandDocumentLabel> = emptyList(),
    val peppolMessageId: String? = null,
    val peppolConversationId: String? = null,
    val receivedPeppolSignalMessage: String? = null,
    val envelopeId: String? = null,
)

/**
 * Document detail returned in `GET /api/v1/documents/{documentId}`.
 */
@Serializable
data class RecommandDocumentDetail(
    val id: String,
    val teamId: String,
    val companyId: String,
    val direction: RecommandDocumentDirection,
    val senderId: String,
    val receiverId: String,
    val docTypeId: String,
    val processId: String,
    val countryC1: String,
    val type: RecommandDocumentType,
    val readAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val xml: String,
    /**
     * One of:
     * - `RecommandInvoice`
     * - `RecommandCreditNote`
     * - `RecommandSelfBillingInvoice`
     * - `RecommandSelfBillingCreditNote`
     * - `null`
     */
    val parsed: JsonElement? = null,
    val validation: RecommandDocumentValidation,
    val sentOverPeppol: Boolean,
    val sentOverEmail: Boolean,
    val emailRecipients: List<String> = emptyList(),
    val labels: List<RecommandDocumentLabel> = emptyList(),
    val peppolMessageId: String? = null,
    val peppolConversationId: String? = null,
    val receivedPeppolSignalMessage: String? = null,
    val envelopeId: String? = null,
)

/**
 * Wrapper response for `GET /api/v1/documents`.
 */
@Serializable
data class RecommandGetDocumentsResponse(
    val success: Boolean,
    val documents: List<RecommandDocumentSummary>,
    val pagination: RecommandApiPagination,
)

/**
 * Query parameters for `GET /api/v1/documents`.
 */
@Serializable
data class RecommandGetDocumentsRequest(
    val page: Int = 1,
    val limit: Int = 10,
    /**
     * The OpenAPI spec allows `companyId` as either a single string or an array of strings.
     * Represent it as a list and use a single-item list for a single company.
     */
    val companyId: List<String>? = null,
    val direction: RecommandDocumentDirection? = null,
    val search: String? = null,
    val type: RecommandDocumentsTypeFilter? = null,
    val from: String? = null,
    val to: String? = null,
    val isUnread: Boolean? = null,
    val envelopeId: String? = null,
)

/**
 * Wrapper response for `GET /api/v1/documents/{documentId}`.
 */
@Serializable
data class RecommandGetDocumentResponse(
    val success: Boolean,
    val document: RecommandDocumentDetail,
)

/**
 * Path parameters for `GET /api/v1/documents/{documentId}`.
 */
@Serializable
data class RecommandGetDocumentRequest(
    val documentId: String,
)

/**
 * Path parameters for `DELETE /api/v1/documents/{documentId}`.
 */
@Serializable
data class RecommandDeleteDocumentRequest(
    val documentId: String,
)

/**
 * Wrapper response for `DELETE /api/v1/documents/{documentId}`.
 */
@Serializable
data class RecommandDeleteDocumentResponse(
    val success: Boolean,
)

/**
 * Document entry returned in `GET /api/v1/inbox`.
 */
@Serializable
data class RecommandInboxDocument(
    val id: String,
    val teamId: String,
    val companyId: String,
    val direction: RecommandDocumentDirection,
    val senderId: String,
    val receiverId: String,
    val docTypeId: String,
    val processId: String,
    val countryC1: String,
    val type: RecommandDocumentType,
    val readAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val validation: RecommandDocumentValidation,
    val sentOverPeppol: Boolean,
    val sentOverEmail: Boolean,
    val emailRecipients: List<String> = emptyList(),
    val labels: List<RecommandDocumentLabel> = emptyList(),
    val peppolMessageId: String? = null,
    val peppolConversationId: String? = null,
    val receivedPeppolSignalMessage: String? = null,
    val envelopeId: String? = null,
)

/**
 * Wrapper response for `GET /api/v1/inbox`.
 */
@Serializable
data class RecommandGetInboxResponse(
    val success: Boolean,
    val documents: List<RecommandInboxDocument>,
)

/**
 * Query parameters for `GET /api/v1/inbox`.
 */
@Serializable
data class RecommandGetInboxRequest(
    val companyId: String? = null,
)

/**
 * Request body for `POST /api/v1/documents/{documentId}/mark-as-read`.
 */
@Serializable
data class RecommandMarkAsReadRequest(
    val read: Boolean = true,
)

/**
 * Path parameters for `POST /api/v1/documents/{documentId}/mark-as-read`.
 */
@Serializable
data class RecommandMarkAsReadPath(
    val documentId: String,
)

/**
 * Response for `POST /api/v1/documents/{documentId}/mark-as-read`.
 */
@Serializable
data class RecommandMarkAsReadResponse(
    val success: Boolean,
)

/**
 * When to include the autogenerated PDF in the downloaded document package.
 *
 * Used in:
 * - `GET /api/v1/documents/{documentId}/download-package` query param `generatePdf`
 */
@Serializable
enum class RecommandGeneratePdf {
    @SerialName("never")
    Never,

    @SerialName("always")
    Always,

    @SerialName("when_no_pdf_attachment")
    WhenNoPdfAttachment,
}

/**
 * Request parameters for `GET /api/v1/documents/{documentId}/download-package`.
 *
 * Response body is `application/zip`.
 */
@Serializable
data class RecommandDownloadPackageRequest(
    val documentId: String,
    val generatePdf: RecommandGeneratePdf = RecommandGeneratePdf.Never,
)

/**
 * Response body for `GET /api/v1/documents/{documentId}/download-package`.
 *
 * Content-Type: `application/zip`.
 */
data class RecommandDownloadPackageResponse(
    val bytes: ByteArray,
)

/**
 * Path parameters for assigning a label to a document.
 *
 * Used in:
 * - `POST /api/v1/documents/{documentId}/labels/{labelId}` (path params: `documentId`, `labelId`)
 */
@Serializable
data class RecommandAssignLabelToDocumentRequest(
    val documentId: String,
    val labelId: String,
)

/**
 * Response for assigning a label to a document.
 *
 * Used in:
 * - `POST /api/v1/documents/{documentId}/labels/{labelId}` (path params: `documentId`, `labelId`)
 */
@Serializable
data class RecommandAssignLabelToDocumentResponse(
    val success: Boolean,
)

/**
 * Path parameters for unassigning a label from a document.
 *
 * Used in:
 * - `DELETE /api/v1/documents/{documentId}/labels/{labelId}` (path params: `documentId`, `labelId`)
 */
@Serializable
data class RecommandUnassignLabelFromDocumentRequest(
    val documentId: String,
    val labelId: String,
)

/**
 * Response for unassigning a label from a document.
 *
 * Used in:
 * - `DELETE /api/v1/documents/{documentId}/labels/{labelId}` (path params: `documentId`, `labelId`)
 */
@Serializable
data class RecommandUnassignLabelFromDocumentResponse(
    val success: Boolean,
)

/**
 * Render type used by the document render endpoint.
 *
 * Used in:
 * - `GET /api/v1/documents/{documentId}/render/{type}` (path params: `documentId`, `type`)
 */
@Serializable
enum class RecommandRenderType {
    @SerialName("html")
    Html,

    @SerialName("pdf")
    Pdf,
}

/**
 * Request parameters for rendering a document.
 *
 * Response body is:
 * - `text/html` when `type=html`
 * - `application/pdf` when `type=pdf`
 */
@Serializable
data class RecommandRenderDocumentRequest(
    val documentId: String,
    val type: RecommandRenderType,
)

/**
 * Response body for `GET /api/v1/documents/{documentId}/render/html`.
 *
 * Content-Type: `text/html`.
 */
data class RecommandRenderDocumentHtmlResponse(
    val html: String,
)

/**
 * Response body for `GET /api/v1/documents/{documentId}/render/pdf`.
 *
 * Content-Type: `application/pdf`.
 */
data class RecommandRenderDocumentPdfResponse(
    val bytes: ByteArray,
)
