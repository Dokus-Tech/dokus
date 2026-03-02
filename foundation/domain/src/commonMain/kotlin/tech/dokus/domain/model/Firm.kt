package tech.dokus.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.enums.FirmAccessStatus
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber

@Serializable
data class Firm(
    val id: FirmId,
    val name: DisplayName,
    val vatNumber: VatNumber,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@Serializable
data class FirmMembership(
    val userId: UserId,
    val firmId: FirmId,
    val role: FirmRole,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

@Serializable
data class FirmMember(
    val userId: UserId,
    val email: Email,
    val firstName: Name?,
    val lastName: Name?,
    val role: FirmRole,
    val joinedAt: LocalDateTime,
    val membershipActive: Boolean,
)

@Serializable
data class FirmAccess(
    val firmId: FirmId,
    val tenantId: TenantId,
    val status: FirmAccessStatus,
    val grantedByUserId: UserId,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)
