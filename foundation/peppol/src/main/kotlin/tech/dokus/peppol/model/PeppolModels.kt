package tech.dokus.peppol.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/**
 * Provider-agnostic Peppol models.
 *
 * These models are used by PeppolService and converted to/from
 * provider-specific formats by each provider implementation.
 */

// ============================================================================
// ENUMS
// ============================================================================

enum class PeppolDirection {
    INBOUND,
    OUTBOUND
}

enum class PeppolDocumentType {
    INVOICE,
    CREDIT_NOTE,
    DEBIT_NOTE,
    ORDER
}

// ============================================================================
// SEND REQUEST MODELS
// ============================================================================

/**
 * Provider-agnostic send request.
 */
@Serializable
data class PeppolSendRequest(
    val recipientPeppolId: String,
    val documentType: PeppolDocumentType,
    val invoice: PeppolInvoiceData
)

@Serializable
data class PeppolInvoiceData(
    val invoiceNumber: String,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val seller: PeppolParty,
    val buyer: PeppolParty,
    val lineItems: List<PeppolLineItem>,
    val currencyCode: String = "EUR",
    val note: String? = null,
    val paymentInfo: PeppolPaymentInfo? = null
)

@Serializable
data class PeppolParty(
    val name: String,
    val vatNumber: String? = null,
    val streetName: String? = null,
    val cityName: String? = null,
    val postalZone: String? = null,
    val countryCode: String? = null,
    val contactEmail: String? = null,
    val contactName: String? = null,
    val companyNumber: String? = null
)

@Serializable
data class PeppolLineItem(
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
data class PeppolPaymentInfo(
    val iban: String?,
    val bic: String?,
    val paymentMeansCode: String = "30",  // 30 = Credit transfer
    val paymentId: String? = null  // Structured reference
)

// ============================================================================
// SEND RESPONSE MODELS
// ============================================================================

@Serializable
data class PeppolSendResponse(
    val success: Boolean,
    val externalDocumentId: String? = null,
    val errorMessage: String? = null,
    val errors: List<PeppolError> = emptyList()
)

@Serializable
data class PeppolError(
    val code: String? = null,
    val message: String,
    val field: String? = null
)

// ============================================================================
// VERIFY MODELS
// ============================================================================

@Serializable
data class PeppolVerifyResponse(
    val registered: Boolean,
    val participantId: String? = null,
    val name: String? = null,
    val documentTypes: List<String> = emptyList()
)

// ============================================================================
// INBOX MODELS
// ============================================================================

@Serializable
data class PeppolInboxItem(
    val id: String,
    val documentType: String,
    val senderPeppolId: String,
    val receiverPeppolId: String,
    val receivedAt: String,
    val isRead: Boolean
)

@Serializable
data class PeppolReceivedDocument(
    val id: String,
    val documentType: String,
    val senderPeppolId: String,
    val invoiceNumber: String?,
    val issueDate: String?,
    val dueDate: String?,
    val seller: PeppolParty?,
    val buyer: PeppolParty?,
    val lineItems: List<PeppolReceivedLineItem>?,
    val totals: PeppolMonetaryTotals?,
    val taxTotal: PeppolTaxTotal?,
    val note: String?,
    val currencyCode: String?
)

@Serializable
data class PeppolReceivedLineItem(
    val id: String?,
    val name: String?,
    val description: String?,
    val quantity: Double?,
    val unitCode: String?,
    val unitPrice: Double?,
    val lineTotal: Double?,
    val taxCategory: String?,
    val taxPercent: Double?
)

@Serializable
data class PeppolMonetaryTotals(
    val lineExtensionAmount: Double?,
    val taxExclusiveAmount: Double?,
    val taxInclusiveAmount: Double?,
    val payableAmount: Double?
)

@Serializable
data class PeppolTaxTotal(
    val taxAmount: Double?,
    val taxSubtotals: List<PeppolTaxSubtotal>?
)

@Serializable
data class PeppolTaxSubtotal(
    val taxableAmount: Double?,
    val taxAmount: Double?,
    val taxCategory: String?,
    val taxPercent: Double?
)

// ============================================================================
// LIST MODELS
// ============================================================================

@Serializable
data class PeppolDocumentList(
    val documents: List<PeppolDocumentSummary>,
    val total: Int,
    val hasMore: Boolean
)

@Serializable
data class PeppolDocumentSummary(
    val id: String,
    val documentType: String,
    val direction: PeppolDirection,
    val counterpartyPeppolId: String,
    val status: String,
    val createdAt: String,
    val invoiceNumber: String?,
    val totalAmount: Double?,
    val currency: String?
)
