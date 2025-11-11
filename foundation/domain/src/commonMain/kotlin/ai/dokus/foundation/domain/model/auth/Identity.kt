package ai.dokus.foundation.domain.model.auth

import ai.dokus.foundation.domain.DeviceType
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.current
import kotlinx.serialization.Serializable

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