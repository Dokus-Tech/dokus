package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.PaymentMeansCode
import tech.dokus.domain.enums.PeppolCurrency
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.enums.RecommandDirection
import tech.dokus.domain.enums.RecommandDocumentStatus
import tech.dokus.domain.enums.UnitCode
import tech.dokus.domain.ids.BillId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PeppolId
import tech.dokus.domain.ids.PeppolSettingsId
import tech.dokus.domain.ids.PeppolTransmissionId
import tech.dokus.domain.ids.TenantId

// ============================================================================
// PEPPOL PROVIDERS
// ============================================================================

/**
 * Supported Peppol Access Point providers.
 * Currently only Recommand is supported.
 */
@Serializable
enum class PeppolProvider(val displayName: String) {
    Recommand("Recommand");

    companion object {
        fun fromName(name: String): PeppolProvider? =
            entries.find { it.name.equals(name, ignoreCase = true) }
    }
}

// ============================================================================
// PEPPOL SETTINGS
// ============================================================================

/**
 * Peppol settings for a tenant.
 *
 * For cloud deployments: credentials are managed by Dokus (not stored per-tenant)
 * For self-hosted: credentials are stored encrypted per-tenant
 */
@Serializable
data class PeppolSettingsDto(
    val id: PeppolSettingsId,
    val tenantId: TenantId,
    /** Recommand company ID */
    val companyId: String,
    /** Tenant's Peppol participant ID (format: scheme:identifier, e.g., "0208:BE0123456789") */
    val peppolId: PeppolId,
    /** Whether Peppol is enabled for this tenant */
    val isEnabled: Boolean = false,
    /** Whether to use test mode (doesn't send to real Peppol network) */
    val testMode: Boolean = true,
    /** Token for webhook authentication (generated on creation) */
    val webhookToken: String? = null,
    /**
     * Whether credentials are managed by Dokus (cloud deployment).
     * If true: user cannot configure credentials, Peppol is automatic.
     * If false: user must provide API credentials (self-hosted deployment).
     */
    val isManagedCredentials: Boolean = false,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Request to create or update Peppol settings.
 * API key and secret are stored securely and never returned in responses.
 */
@Serializable
data class SavePeppolSettingsRequest(
    val companyId: String,
    val apiKey: String,
    val apiSecret: String,
    val peppolId: String,
    val isEnabled: Boolean = false,
    val testMode: Boolean = true
)

// ============================================================================
// PEPPOL CONNECTION (RECOMMAND DISCOVERY)
// ============================================================================

@Serializable
data class PeppolConnectRequest(
    val apiKey: String,
    val apiSecret: String,
    val isEnabled: Boolean = false,
    val testMode: Boolean = true,
    /**
     * Optional company ID to select when multiple matches exist.
     * If omitted and multiple matches are found, the backend returns candidates without saving credentials.
     */
    val companyId: String? = null,
    /**
     * When true and no matching company is found, create a new company on Recommand.
     * When false and no matching company is found, return NoCompanyFound status for user confirmation.
     */
    val createCompanyIfMissing: Boolean = false,
)

@Serializable
enum class PeppolConnectStatus {
    /** Settings saved and tenant is connected to a Recommand company. */
    Connected,

    /** Multiple Recommand companies match the tenant VAT; user must select one. */
    MultipleMatches,

    /** No matching company found; user can confirm to create one. */
    NoCompanyFound,

    /** No VAT configured for tenant. */
    MissingVatNumber,

    /** Tenant address is missing or cannot be used to create a Recommand company. */
    MissingCompanyAddress,

    /** Recommand rejected the provided credentials. */
    InvalidCredentials,
}

@Serializable
data class RecommandCompanySummary(
    val id: String,
    val name: String,
    val vatNumber: String,
    val enterpriseNumber: String,
)

@Serializable
data class PeppolConnectResponse(
    val status: PeppolConnectStatus,
    val settings: PeppolSettingsDto? = null,
    val company: RecommandCompanySummary? = null,
    val candidates: List<RecommandCompanySummary> = emptyList(),
    val createdCompany: Boolean = false,
)

// ============================================================================
// PEPPOL TRANSMISSIONS
// ============================================================================

/**
 * Record of a Peppol document transmission (sent or received).
 */
@Serializable
data class PeppolTransmissionDto(
    val id: PeppolTransmissionId,
    val tenantId: TenantId,
    val direction: PeppolTransmissionDirection,
    val documentType: PeppolDocumentType,
    val status: PeppolStatus,
    /** Reference to the local invoice (for outbound) */
    val invoiceId: InvoiceId? = null,
    /** Reference to the local bill (for inbound) */
    val billId: BillId? = null,
    /** Recommand document ID */
    val externalDocumentId: String? = null,
    /** Recipient Peppol ID (for outbound) */
    val recipientPeppolId: PeppolId? = null,
    /** Sender Peppol ID (for inbound) */
    val senderPeppolId: PeppolId? = null,
    /** Error message if failed */
    val errorMessage: String? = null,
    /** Raw request sent to Recommand */
    val rawRequest: String? = null,
    /** Raw response from Recommand */
    val rawResponse: String? = null,
    val transmittedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// ============================================================================
// RECOMMAND API MODELS
// ============================================================================

/**
 * Recommand API: Send document request.
 */
@Serializable
data class RecommandSendRequest(
    /** Peppol ID format: "scheme:identifier" (e.g., "0208:BE0123456789") */
    val recipient: String,
    /** Document type to send */
    val documentType: RecommandSendDocumentType,
    /** Invoice document in simplified JSON format */
    val document: RecommandInvoiceDocument? = null,
    /** Required when documentType is XML - the UBL document type ID */
    val doctypeId: String? = null
)

/**
 * Document types for sending via Recommand API.
 */
@Serializable
enum class RecommandSendDocumentType {
    @SerialName("invoice")
    Invoice,

    @SerialName("creditNote")
    CreditNote,

    @SerialName("selfBillingInvoice")
    SelfBillingInvoice,

    @SerialName("selfBillingCreditNote")
    SelfBillingCreditNote,

    @SerialName("xml")
    Xml
}

/**
 * Recommand API: Invoice document in simplified JSON format.
 * Recommand automatically converts this to UBL XML.
 */
@Serializable
data class RecommandInvoiceDocument(
    val invoiceNumber: String,
    /** ISO format: YYYY-MM-DD */
    val issueDate: String,
    /** ISO format: YYYY-MM-DD */
    val dueDate: String,
    val buyer: RecommandParty,
    /** Optional - uses company profile if not provided */
    val seller: RecommandParty? = null,
    val lineItems: List<RecommandLineItem>,
    val note: String? = null,
    /** Purchase order reference or buyer reference */
    val buyerReference: String? = null,
    val paymentMeans: RecommandPaymentMeans? = null,
    /** ISO 4217 currency code */
    val documentCurrencyCode: String = "EUR"
)

@Serializable
data class RecommandParty(
    val vatNumber: String? = null,
    val name: String,
    val streetName: String? = null,
    val cityName: String? = null,
    val postalZone: String? = null,
    /** ISO 3166-1 alpha-2 country code */
    val countryCode: String? = null,
    val contactEmail: String? = null,
    val contactName: String? = null
)

@Serializable
data class RecommandLineItem(
    /** Line item ID (usually sequential: "1", "2", etc.) */
    val id: String,
    val name: String,
    val description: String? = null,
    val quantity: Double,
    /** UNCL5305 unit code (default: C62 = "unit") */
    val unitCode: String = UnitCode.Each.code,
    val unitPrice: Double,
    val lineTotal: Double,
    /** UNCL5305 tax category code (S, Z, E, AE, K, G, O, etc.) */
    val taxCategory: String,
    /** VAT percentage (e.g., 21.0 for 21%) */
    val taxPercent: Double
)

@Serializable
data class RecommandPaymentMeans(
    val iban: String? = null,
    val bic: String? = null,
    /** UNCL4461 payment means code (default: 30 = Credit transfer) */
    val paymentMeansCode: String = PaymentMeansCode.CreditTransfer.code,
    /** Structured payment reference (e.g., Belgian OGM/VCS) */
    val paymentId: String? = null
)

/**
 * Recommand API: Send document response.
 */
@Serializable
data class RecommandSendResponse(
    val success: Boolean,
    val documentId: String? = null,
    val message: String? = null,
    val errors: List<RecommandValidationError>? = null
)

@Serializable
data class RecommandValidationError(
    val code: String? = null,
    val message: String,
    val field: String? = null,
    val rule: String? = null
)

/**
 * Recommand API: Validation result for a document.
 */
@Serializable
data class RecommandValidation(
    val result: String, // "valid", "invalid", "not_supported", "error"
    val errors: List<RecommandValidationError> = emptyList()
)

/**
 * Recommand API: Label attached to a document.
 */
@Serializable
data class RecommandLabel(
    val id: String,
    val externalId: String? = null,
    val name: String,
    val colorHex: String
)

/**
 * Recommand API: Inbox document (received from Peppol network).
 * This is the structure returned by /api/v1/inbox - NO parsed field.
 * Field names match the actual Recommand API schema.
 */
@Serializable
data class RecommandInboxDocument(
    val id: String,
    val teamId: String,
    val companyId: String,
    val direction: String, // "incoming" or "outgoing"
    val senderId: String, // Peppol ID of sender
    val receiverId: String, // Peppol ID of receiver
    val docTypeId: String,
    val processId: String,
    val countryC1: String,
    val type: String, // "invoice", "creditNote", etc.
    val readAt: String? = null, // ISO timestamp, null if unread
    val createdAt: String, // ISO timestamp
    val updatedAt: String,
    val validation: RecommandValidation,
    val sentOverPeppol: Boolean,
    val sentOverEmail: Boolean,
    val emailRecipients: List<String> = emptyList(),
    val labels: List<RecommandLabel> = emptyList(),
    val peppolMessageId: String? = null,
    val peppolConversationId: String? = null,
    val receivedPeppolSignalMessage: String? = null,
    val envelopeId: String? = null
) {
    /** Convenience property: true if document is unread (readAt is null) */
    val isUnread: Boolean get() = readAt == null
}

/**
 * Recommand API: Inbox response wrapper.
 */
@Serializable
data class RecommandInboxResponse(
    val success: Boolean,
    val documents: List<RecommandInboxDocument> = emptyList()
)

/**
 * Recommand API: Full document detail (returned by /api/v1/documents/{id}).
 * Includes all inbox fields PLUS xml and parsed content.
 */
@Serializable
data class RecommandDocumentDetail(
    val id: String,
    val teamId: String,
    val companyId: String,
    val direction: String,
    val senderId: String,
    val receiverId: String,
    val docTypeId: String,
    val processId: String,
    val countryC1: String,
    val type: String,
    val readAt: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val validation: RecommandValidation,
    val sentOverPeppol: Boolean,
    val sentOverEmail: Boolean,
    val emailRecipients: List<String> = emptyList(),
    val labels: List<RecommandLabel> = emptyList(),
    val peppolMessageId: String? = null,
    val peppolConversationId: String? = null,
    val receivedPeppolSignalMessage: String? = null,
    val envelopeId: String? = null,
    // Additional fields only in single document response
    val xml: String? = null,
    val parsed: RecommandParsedDocument? = null
) {
    val isUnread: Boolean get() = readAt == null
}

/**
 * Recommand API: Single document response wrapper.
 */
@Serializable
data class RecommandDocumentResponse(
    val success: Boolean,
    val document: RecommandDocumentDetail? = null
)

/**
 * Recommand API: Parsed document content (Invoice or CreditNote).
 * This is the structure inside the "parsed" field of a document detail.
 */
@Serializable
data class RecommandParsedDocument(
    val invoiceNumber: String? = null,
    val creditNoteNumber: String? = null,
    val issueDate: String? = null,
    val dueDate: String? = null,
    val note: String? = null,
    val buyerReference: String? = null,
    val purchaseOrderReference: String? = null,
    val salesOrderReference: String? = null,
    val despatchReference: String? = null,
    val seller: RecommandParsedParty? = null,
    val buyer: RecommandParsedParty? = null,
    val lines: List<RecommandParsedLine>? = null,
    val paymentMeans: List<RecommandParsedPaymentMeans>? = null,
    val totals: RecommandParsedTotals? = null,
    val vat: RecommandParsedVat? = null,
    val currency: String? = null,
    val attachments: List<RecommandParsedAttachment>? = null
)

/**
 * Recommand API: Parsed party (seller/buyer) from document.
 */
@Serializable
data class RecommandParsedParty(
    val vatNumber: String? = null,
    val enterpriseNumber: String? = null,
    val name: String,
    val street: String? = null,
    val street2: String? = null,
    val city: String? = null,
    val postalZone: String? = null,
    val country: String? = null,
    val email: String? = null,
    val phone: String? = null
)

/**
 * Recommand API: Parsed line item from document.
 */
@Serializable
data class RecommandParsedLine(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val quantity: String? = null,
    val unitCode: String? = null,
    val netPriceAmount: String? = null,
    val netAmount: String? = null,
    val vat: RecommandParsedLineVat? = null
)

@Serializable
data class RecommandParsedLineVat(
    val category: String? = null,
    val percentage: String? = null
)

@Serializable
data class RecommandParsedPaymentMeans(
    val name: String? = null,
    val paymentMethod: String? = null,
    val reference: String? = null,
    val iban: String? = null,
    val financialInstitutionBranch: String? = null
)

@Serializable
data class RecommandParsedTotals(
    val linesAmount: String? = null,
    val discountAmount: String? = null,
    val surchargeAmount: String? = null,
    val taxExclusiveAmount: String? = null,
    val taxInclusiveAmount: String? = null,
    val payableAmount: String? = null,
    val paidAmount: String? = null
)

@Serializable
data class RecommandParsedVat(
    val totalVatAmount: String? = null,
    val subtotals: List<RecommandParsedVatSubtotal>? = null
)

@Serializable
data class RecommandParsedVatSubtotal(
    val taxableAmount: String? = null,
    val vatAmount: String? = null,
    val category: String? = null,
    val percentage: String? = null
)

/**
 * Recommand API: Parsed attachment from document.
 * PDF invoices are embedded as base64 in the embeddedDocument field.
 */
@Serializable
data class RecommandParsedAttachment(
    val id: String? = null,
    val url: String? = null,
    val filename: String? = null,
    val mimeCode: String? = null,
    val description: String? = null,
    val embeddedDocument: String? = null  // base64-encoded content
)

/**
 * Recommand API: Received document content.
 */
@Serializable
data class RecommandReceivedDocument(
    val invoiceNumber: String? = null,
    val issueDate: String? = null,
    val dueDate: String? = null,
    val seller: RecommandParty? = null,
    val buyer: RecommandParty? = null,
    val lineItems: List<RecommandReceivedLineItem>? = null,
    val legalMonetaryTotal: RecommandMonetaryTotal? = null,
    val taxTotal: RecommandTaxTotal? = null,
    val note: String? = null,
    val documentCurrencyCode: String? = null
)

@Serializable
data class RecommandReceivedLineItem(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val quantity: Double? = null,
    val unitCode: String? = null,
    val unitPrice: Double? = null,
    val lineExtensionAmount: Double? = null,
    val taxCategory: String? = null,
    val taxPercent: Double? = null
)

@Serializable
data class RecommandMonetaryTotal(
    val lineExtensionAmount: Double? = null,
    val taxExclusiveAmount: Double? = null,
    val taxInclusiveAmount: Double? = null,
    val payableAmount: Double? = null
)

@Serializable
data class RecommandTaxTotal(
    val taxAmount: Double? = null,
    val taxSubtotals: List<RecommandTaxSubtotal>? = null
)

@Serializable
data class RecommandTaxSubtotal(
    val taxableAmount: Double? = null,
    val taxAmount: Double? = null,
    val taxCategory: String? = null,
    val taxPercent: Double? = null
)

/**
 * Recommand API: Verify recipient on Peppol network.
 */
@Serializable
data class RecommandVerifyRequest(
    val participantId: String
)

@Serializable
data class RecommandVerifyResponse(
    val registered: Boolean,
    val participantId: String? = null,
    val name: String? = null,
    val documentTypes: List<String>? = null
)

/**
 * Recommand API: Mark document as read request.
 */
@Serializable
data class RecommandMarkAsReadRequest(
    val read: Boolean
)

/**
 * Recommand API: List documents response.
 */
@Serializable
data class RecommandDocumentsResponse(
    val data: List<RecommandDocumentSummary> = emptyList(),
    val total: Int = 0,
    @SerialName("has_more")
    val hasMore: Boolean = false
)

@Serializable
data class RecommandDocumentSummary(
    val id: String,
    @SerialName("documentType")
    val documentType: PeppolDocumentType,
    val direction: RecommandDirection,
    val counterparty: String, // Peppol ID
    val status: RecommandDocumentStatus,
    val createdAt: String, // ISO timestamp
    val invoiceNumber: String? = null,
    val totalAmount: Double? = null,
    val currency: PeppolCurrency? = null
)

// ============================================================================
// VERIFICATION MODELS
// ============================================================================

/**
 * Response when verifying a Peppol recipient.
 * Provider-agnostic model for recipient verification.
 */
@Serializable
data class PeppolVerifyResponse(
    /** Whether the recipient is registered on the Peppol network */
    val registered: Boolean,
    /** The verified Peppol participant ID */
    val participantId: String? = null,
    /** Organization/company name if available */
    val name: String? = null,
    /** Supported document types (e.g., "invoice", "creditnote") */
    val documentTypes: List<String> = emptyList()
)

// ============================================================================
// VALIDATION MODELS
// ============================================================================

/**
 * Result of Peppol invoice validation.
 */
@Serializable
data class PeppolValidationResult(
    val isValid: Boolean,
    val errors: List<PeppolValidationError> = emptyList(),
    val warnings: List<PeppolValidationWarning> = emptyList()
)

@Serializable
data class PeppolValidationError(
    val code: String,
    val message: String,
    val field: String? = null
)

@Serializable
data class PeppolValidationWarning(
    val code: String,
    val message: String,
    val field: String? = null
)

// ============================================================================
// SERVICE REQUEST/RESPONSE MODELS
// ============================================================================

/**
 * Request to send an invoice via Peppol.
 */
@Serializable
data class SendInvoiceViaPeppolRequest(
    val invoiceId: InvoiceId
)

/**
 * Response after sending an invoice via Peppol.
 */
@Serializable
data class SendInvoiceViaPeppolResponse(
    val transmissionId: PeppolTransmissionId,
    val status: PeppolStatus,
    val externalDocumentId: String? = null,
    val errorMessage: String? = null
)

/**
 * Response when polling the Peppol inbox.
 */
@Serializable
data class PeppolInboxPollResponse(
    val newDocuments: Int,
    val processedDocuments: List<ProcessedPeppolDocument>
)

@Serializable
data class ProcessedPeppolDocument(
    val transmissionId: PeppolTransmissionId,
    val documentId: DocumentId,
    val senderPeppolId: PeppolId,
    val invoiceNumber: String?,
    val totalAmount: Money?,
    val receivedAt: LocalDateTime
)
