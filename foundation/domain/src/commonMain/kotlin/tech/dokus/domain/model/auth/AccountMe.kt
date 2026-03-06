package tech.dokus.domain.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.User

@Serializable
enum class AppSurface {
    @SerialName("WORKSPACE")
    CompanyManager,

    @SerialName("CONSOLE")
    BookkeeperConsole,
}

@Serializable
data class SurfaceAvailability(
    val canCompanyManager: Boolean,
    val canBookkeeperConsole: Boolean,
    val defaultSurface: AppSurface,
) {
    val canWorkspace: Boolean get() = canCompanyManager
    val canConsole: Boolean get() = canBookkeeperConsole
}

@Serializable
data class TenantWorkspaceSummary(
    val id: TenantId,
    val name: DisplayName,
    val vatNumber: VatNumber,
    val role: UserRole,
    val type: TenantType,
)

@Serializable
data class FirmWorkspaceSummary(
    val id: FirmId,
    val name: DisplayName,
    val vatNumber: VatNumber,
    val role: FirmRole,
    val clientCount: Int,
)

@Serializable
data class AccountMeResponse(
    val user: User,
    val surface: SurfaceAvailability,
    val tenants: List<TenantWorkspaceSummary> = emptyList(),
    val firms: List<FirmWorkspaceSummary> = emptyList(),
)
