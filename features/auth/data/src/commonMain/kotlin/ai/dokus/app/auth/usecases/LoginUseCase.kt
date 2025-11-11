package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.model.auth.LoginRequest
import ai.dokus.foundation.platform.Logger

/**
 * Login with email and password.
 *
 * Validates the credentials format first, then hits the backend. If login works,
 * the user gets saved to local storage so the app can work offline.
 */
class LoginUseCase(
    private val authRepository: AuthRepository
) {
    private val logger = Logger.forClass<LoginUseCase>()

    suspend operator fun invoke(email: Email, password: Password): Result<Unit> {
        logger.d { "Executing login use case" }

        // Validate using built-in validation
        if (!email.isValid) {
            logger.w { "Invalid email format" }
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        if (!password.isValid) {
            logger.w { "Invalid password format (minimum 8 characters)" }
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }

        // Create login request
        val request = LoginRequest(
            email = email,
            password = password,
            rememberMe = true
        )

        // Perform login
        return authRepository.login(request)
    }
}
