package tech.dokus.domain.model.auth

import kotlinx.serialization.Serializable
import tech.dokus.domain.DeviceType
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.current
import tech.dokus.domain.ids.TenantId

@Serializable
data class LoginRequest(
    val email: Email,
    val password: Password,
    val rememberMe: Boolean = true,
    val deviceType: DeviceType = DeviceType.current,
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
    val deviceType: DeviceType = DeviceType.current,
    val tenantId: TenantId? = null,
)

@Serializable
data class LogoutRequest(
    val sessionToken: String,
    val refreshToken: String? = null
)

@Serializable
data class ResetPasswordRequest(
    val newPassword: String
)

@Serializable
data class RegisterRequest(
    val email: Email,
    val password: Password,
    val firstName: Name,
    val lastName: Name,
)

@Serializable
data class RegisterResponse(
    val userId: String,
    val message: String
)

@Serializable
data class DeactivateUserRequest(
    val reason: String
)

/**
 * Request to update user profile (first name, last name)
 */
@Serializable
data class UpdateProfileRequest(
    val firstName: Name? = null,
    val lastName: Name? = null
)

/**
 * Request to select/switch to a different tenant
 */
@Serializable
data class SelectTenantRequest(
    val tenantId: TenantId
)
