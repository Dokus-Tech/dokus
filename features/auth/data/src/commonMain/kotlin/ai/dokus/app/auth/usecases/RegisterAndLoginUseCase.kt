package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.model.auth.RegisterRequest
import ai.dokus.foundation.platform.Logger

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
