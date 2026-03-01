package tech.dokus.domain.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.model.User

@Serializable
enum class AppSurface {
    @SerialName("WORKSPACE")
    Workspace,

    @SerialName("CONSOLE")
    Console
}

@Serializable
data class SurfaceAvailability(
    val canWorkspace: Boolean,
    val canConsole: Boolean,
    val defaultSurface: AppSurface
)

@Serializable
data class AccountMeResponse(
    val user: User,
    val surface: SurfaceAvailability
)
