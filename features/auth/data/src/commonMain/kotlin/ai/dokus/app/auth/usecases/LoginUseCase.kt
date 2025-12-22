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

    /**
     * Authenticates the user with the provided email and password.
     *
     * Performs client-side validation of credential formats before attempting authentication
     * with the backend. On successful login, the user's authentication tokens are stored
     * locally, enabling offline access and automatic session restoration.
     *
     * @param email The user's email address wrapped in an [Email] value object.
     *              Must pass [Email.isValid] validation (proper email format).
     * @param password The user's password wrapped in a [Password] value object.
     *                 Must pass [Password.isValid] validation (minimum 8 characters).
     * @return [Result.success] with [Unit] if authentication succeeded and tokens were stored.
     *         [Result.failure] with:
     *         - [IllegalArgumentException] if email format is invalid
     *         - [IllegalArgumentException] if password is less than 8 characters
     *         - Network or server errors if the authentication request fails
     *         - Authentication errors if credentials are rejected by the server
     */
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
