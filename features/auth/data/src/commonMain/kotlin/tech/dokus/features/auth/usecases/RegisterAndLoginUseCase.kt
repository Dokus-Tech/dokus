@file:Suppress("ReturnCount") // Multi-step registration flow requires validation returns

package tech.dokus.features.auth.usecases

import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.model.auth.RegisterRequest
import tech.dokus.features.auth.repository.AuthRepository
import tech.dokus.foundation.platform.Logger

/**
 * Handles registration followed by automatic login.
 *
 * When someone registers, we validate their info, create their account, then
 * log them in automatically so they don't have to enter their password again.
 * Much better user experience than making them login manually after signup.
 */
class RegisterAndLoginUseCase(
    private val authRepository: AuthRepository
) {
    private val logger = Logger.forClass<RegisterAndLoginUseCase>()

    /**
     * Registers a new user account and automatically logs them in.
     *
     * Performs client-side validation of all registration fields before sending the request
     * to the backend. On successful registration, the user is automatically authenticated
     * and their session tokens are stored locally, providing immediate access without
     * requiring a separate login step.
     *
     * @param email The user's email address wrapped in an [Email] value object.
     *              Must pass [Email.isValid] validation (proper email format).
     *              Will be used as the account identifier for future logins.
     * @param password The user's chosen password wrapped in a [Password] value object.
     *                 Must pass [Password.isValid] validation (minimum 8 characters).
     * @param firstName The user's first name wrapped in a [Name] value object.
     *                  Must pass [Name.isValid] validation (non-empty).
     * @param lastName The user's last name wrapped in a [Name] value object.
     *                 Must pass [Name.isValid] validation (non-empty).
     * @return [Result.success] with [Unit] if registration and auto-login succeeded.
     *         [Result.failure] with:
     *         - [IllegalArgumentException] if email format is invalid
     *         - [IllegalArgumentException] if password is less than 8 characters
     *         - [IllegalArgumentException] if first name is empty/invalid
     *         - [IllegalArgumentException] if last name is empty/invalid
     *         - Network or server errors if the registration request fails
     *         - Registration errors if the email is already in use or rejected by the server
     */
    suspend operator fun invoke(
        email: Email,
        password: Password,
        firstName: Name,
        lastName: Name
    ): Result<Unit> {
        logger.d { "Executing register and login use case" }

        // Validate using built-in validation
        if (!email.isValid) {
            logger.w { "Invalid email format" }
            return Result.failure(IllegalArgumentException("Invalid email format"))
        }

        if (!password.isValid) {
            logger.w { "Invalid password format (minimum 8 characters)" }
            return Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
        }

        if (!firstName.isValid) {
            logger.w { "Invalid first name" }
            return Result.failure(IllegalArgumentException("First name is required"))
        }

        if (!lastName.isValid) {
            logger.w { "Invalid last name" }
            return Result.failure(IllegalArgumentException("Last name is required"))
        }

        // Create registration request
        val request = RegisterRequest(
            email = email,
            password = password,
            firstName = firstName,
            lastName = lastName
        )

        // Perform registration (auto-login handled by repository)
        return authRepository.register(request)
    }
}
