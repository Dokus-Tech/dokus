package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import tech.dokus.domain.Money
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolLookupSource
import tech.dokus.domain.enums.PeppolLookupStatus
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.ContactId
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
 * Credentials are managed via environment variables, not stored per-tenant.
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
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

// ============================================================================
// PEPPOL CONNECTION
// ============================================================================

/**
 * Request to connect to Peppol by auto-discovering company via Recommand.
 * The company is matched by tenant VAT number using the provided address.
 */
@Serializable
data class PeppolConnectRequest(
    val companyAddress: Address
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
    /** Reference to the local invoice entity (inbound or outbound). */
    val invoiceId: InvoiceId? = null,
    /** Recommand document ID */
    val externalDocumentId: String? = null,
    /** Recipient Peppol ID (for outbound) */
    val recipientPeppolId: PeppolId? = null,
    /** Sender Peppol ID (for inbound) */
    val senderPeppolId: PeppolId? = null,
    /** Error message if failed */
    val errorMessage: String? = null,
    /** Raw request sent to Recommand (internal only, excluded from API serialization) */
    @Transient
    val rawRequest: String? = null,
    /** Raw response from Recommand (internal only, excluded from API serialization) */
    @Transient
    val rawResponse: String? = null,
    /** Worker attempt count for outbound retries */
    val attemptCount: Int = 0,
    /** Next retry timestamp for retryable outbound errors */
    val nextRetryAt: LocalDateTime? = null,
    /** Last attempt timestamp for outbound sends */
    val lastAttemptAt: LocalDateTime? = null,
    /** Stable provider error code for diagnostics */
    val providerErrorCode: String? = null,
    /** Provider-facing error message (not raw payload) */
    val providerErrorMessage: String? = null,
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

// ============================================================================
// PEPPOL DIRECTORY CACHE MODELS
// ============================================================================

/**
 * Cache-row model for directory lookup results.
 * Only stored values, no UNKNOWN status (that's API-only).
 * Timestamps are non-null since they're always set on insert/update.
 */
@Serializable
data class PeppolResolution(
    val contactId: ContactId,
    val status: PeppolLookupStatus,
    val participantId: String? = null,
    val scheme: String? = null,
    val supportedDocTypes: List<String> = emptyList(),
    val source: PeppolLookupSource,
    val vatNumberSnapshot: String? = null,
    val companyNumberSnapshot: String? = null,
    val lastCheckedAt: LocalDateTime,
    val expiresAt: LocalDateTime? = null,
    val errorMessage: String? = null
)

/**
 * API response for PEPPOL status endpoint.
 * Can return "unknown" status when no cache entry exists.
 */
@Serializable
data class PeppolStatusResponse(
    /** "found" | "not_found" | "error" | "unknown" */
    val status: String,
    val participantId: String? = null,
    val supportedDocTypes: List<String> = emptyList(),
    /** "directory" | "manual" | null (if unknown) */
    val source: String? = null,
    val lastCheckedAt: LocalDateTime? = null,
    val expiresAt: LocalDateTime? = null,
    /** true if fetched via ?refresh=true this request */
    val refreshed: Boolean,
    val errorMessage: String? = null
)
