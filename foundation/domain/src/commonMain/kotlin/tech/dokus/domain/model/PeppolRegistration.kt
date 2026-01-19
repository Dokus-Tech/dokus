package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.PeppolRegistrationStatus
import tech.dokus.domain.ids.PeppolRegistrationId
import tech.dokus.domain.ids.TenantId

/**
 * PEPPOL registration state for a tenant.
 *
 * Represents the current state in the PEPPOL registration lifecycle.
 * One registration per tenant.
 */
@Serializable
data class PeppolRegistrationDto(
    val id: PeppolRegistrationId,
    val tenantId: TenantId,
    /** Tenant's PEPPOL participant ID (format: "0208:BE0123456789") */
    val peppolId: String,
    /** Recommand company ID (null until registered) */
    val recommandCompanyId: String?,
    /** Current registration status */
    val status: PeppolRegistrationStatus,
    /** Whether tenant can receive PEPPOL documents */
    val canReceive: Boolean,
    /** Whether tenant can send PEPPOL documents */
    val canSend: Boolean,
    /** Whether using test mode */
    val testMode: Boolean,
    /** When the transfer wait started (for WAITING_TRANSFER status) */
    val waitingSince: LocalDateTime?,
    /** Last time we polled for transfer status */
    val lastPolledAt: LocalDateTime?,
    /** Error message if status is FAILED */
    val errorMessage: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

/**
 * Result of verifying a PEPPOL ID before registration.
 */
@Serializable
data class PeppolIdVerificationResult(
    /** The PEPPOL ID that was verified */
    val peppolId: String,
    /** Whether this ID is already registered with another provider */
    val isBlocked: Boolean,
    /** Name of the blocking provider (if blocked) */
    val blockedBy: String?,
    /** Whether registration can proceed */
    val canProceed: Boolean
)

/**
 * Response after a PEPPOL registration action.
 */
@Serializable
data class PeppolRegistrationResponse(
    val registration: PeppolRegistrationDto,
    val nextAction: PeppolNextAction?
)

/**
 * Suggested next action after a PEPPOL registration operation.
 */
@Serializable
enum class PeppolNextAction {
    /** No action needed - registration complete */
    NONE,
    /** User should wait for transfer from another provider */
    WAIT_FOR_TRANSFER,
    /** User should contact support */
    CONTACT_SUPPORT,
    /** User can retry the operation */
    RETRY
}
