package tech.dokus.domain.model.auth

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber

@Serializable
data class BookkeeperFirmSearchItem(
    val firmId: FirmId,
    val name: DisplayName,
    val vatNumber: VatNumber,
    val alreadyConnected: Boolean,
)

@Serializable
data class TenantBookkeeperAccessItem(
    val firmId: FirmId,
    val firmName: DisplayName,
    val vatNumber: VatNumber,
    val grantedAt: LocalDateTime,
)

@Serializable
data class GrantBookkeeperAccessRequest(
    val firmId: FirmId,
)

@Serializable
data class GrantBookkeeperAccessResponse(
    val firmId: FirmId,
    val tenantId: TenantId,
    val activated: Boolean,
)
