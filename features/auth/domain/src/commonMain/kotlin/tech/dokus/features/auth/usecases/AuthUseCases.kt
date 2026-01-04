package tech.dokus.features.auth.usecases

import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.Password

/**
 * Use case for logging in.
 */
interface LoginUseCase {
    suspend operator fun invoke(email: Email, password: Password): Result<Unit>
}

/**
 * Use case for registering and logging in.
 */
interface RegisterAndLoginUseCase {
    suspend operator fun invoke(
        email: Email,
        password: Password,
        firstName: Name,
        lastName: Name
    ): Result<Unit>
}

/**
 * Use case for logging out.
 */
interface LogoutUseCase {
    suspend operator fun invoke(): Result<Unit>
}
