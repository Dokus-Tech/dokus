package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.enums.PeppolDocumentType
import ai.dokus.foundation.domain.enums.PeppolStatus
import ai.dokus.foundation.domain.enums.PeppolTransmissionDirection
import ai.dokus.foundation.domain.enums.PeppolVatCategory
import ai.dokus.foundation.domain.ids.BillId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.PeppolId
import ai.dokus.foundation.domain.ids.PeppolSettingsId
import ai.dokus.foundation.domain.ids.PeppolTransmissionId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================================================
// PEPPOL SETTINGS
// ============================================================================

/**
 * Peppol settings for a tenant - stores Recommand API credentials.
 * Each tenant must configure their own Peppol Access Point credentials.
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
    val recipient: String,  // Peppol ID format: "scheme:identifier"
    val documentType: String,  // "invoice" or "xml"
    val document: RecommandInvoiceDocument? = null,
    val doctypeId: String? = null  // Required when documentType is "xml"
)

/**
 * Recommand API: Invoice document in simplified JSON format.
 * Recommand automatically converts this to UBL XML.
 */
@Serializable
data class RecommandInvoiceDocument(
    val invoiceNumber: String,
    val issueDate: String,  // ISO format: YYYY-MM-DD
    val dueDate: String,
    val buyer: RecommandParty,
    val seller: RecommandParty? = null,  // Optional - uses company profile if not provided
    val lineItems: List<RecommandLineItem>,
    val note: String? = null,
    val buyerReference: String? = null,
    val paymentMeans: RecommandPaymentMeans? = null,
    val documentCurrencyCode: String = "EUR"
)

@Serializable
data class RecommandParty(
    val vatNumber: String? = null,
    val name: String,
    val streetName: String? = null,
    val cityName: String? = null,
    val postalZone: String? = null,
    val countryCode: String? = null,
    val contactEmail: String? = null,
    val contactName: String? = null
)

@Serializable
data class RecommandLineItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val quantity: Double,
    val unitCode: String = "C62",  // Default: "unit", HUR=hours, DAY=days
    val unitPrice: Double,
    val lineTotal: Double,
    val taxCategory: String,  // S, Z, E, AE, K, G, O
    val taxPercent: Double
)

@Serializable
data class RecommandPaymentMeans(
    val iban: String? = null,
    val bic: String? = null,
    val paymentMeansCode: String = "30",  // 30 = Credit transfer
    val paymentId: String? = null  // Structured reference
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
 * Recommand API: Inbox document (received from Peppol network).
 */
@Serializable
data class RecommandInboxDocument(
    val id: String,
    val documentType: String,
    val sender: String,  // Peppol ID
    val receiver: String,  // Peppol ID
    val receivedAt: String,  // ISO timestamp
    val isRead: Boolean,
    val document: RecommandReceivedDocument? = null
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
 * Recommand API: List documents response.
 */
@Serializable
data class RecommandDocumentsResponse(
    val documents: List<RecommandDocumentSummary>,
    val total: Int,
    val hasMore: Boolean
)

@Serializable
data class RecommandDocumentSummary(
    val id: String,
    val documentType: String,
    val direction: String,
    val counterparty: String,  // Peppol ID
    val status: String,
    val createdAt: String,
    val invoiceNumber: String? = null,
    val totalAmount: Double? = null,
    val currency: String? = null
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
    val billId: BillId,
    val senderPeppolId: PeppolId,
    val invoiceNumber: String?,
    val totalAmount: Money?,
    val receivedAt: LocalDateTime
)
