package tech.dokus.domain.model.auth

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber

/**
 * Request to create a new firm (accounting practice).
 *
 * When [prefillTenantId] is set, [name] and [vatNumber] fall back to the
 * tenant's values on the server side. When [prefillTenantId] is null,
 * both [name] and [vatNumber] are required.
 */
@Serializable
data class CreateFirmRequest(
    val prefillTenantId: TenantId? = null,
    val name: DisplayName? = null,
    val vatNumber: VatNumber? = null,
)

@Serializable
data class CreateFirmResponse(
    val firm: FirmWorkspaceSummary,
)

@Serializable
data class GenerateFirmInviteLinkResponse(
    val token: String,
    val expiresAt: LocalDateTime,
)

@Serializable
data class ResolveFirmInviteResponse(
    val firmId: FirmId,
    val firmName: DisplayName,
    val firmVatNumber: VatNumber,
    val expiresAt: LocalDateTime,
)

@Serializable
data class AcceptFirmInviteRequest(
    val token: String,
)

@Serializable
data class AcceptFirmInviteResponse(
    val firmId: FirmId,
    val tenantId: TenantId,
    val activated: Boolean,
)
