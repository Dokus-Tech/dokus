package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.Money
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
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
    /** Last time a full sync was performed (used for first connection and weekly sync) */
    val lastFullSyncAt: LocalDateTime? = null,
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
