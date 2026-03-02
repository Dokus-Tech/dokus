package tech.dokus.domain.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
)

@Serializable
data class AccountMeResponse(
    val user: User,
    val surface: SurfaceAvailability
)
