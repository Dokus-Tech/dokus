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

/**
 * Use case for requesting password reset email.
 */
interface RequestPasswordResetUseCase {
    suspend operator fun invoke(email: Email): Result<Unit>
}

/**
 * Use case for completing reset password with a token.
 */
interface ResetPasswordUseCase {
    suspend operator fun invoke(resetToken: String, newPassword: Password): Result<Unit>
}

/**
 * Use case for email verification with token.
 */
interface VerifyEmailUseCase {
    suspend operator fun invoke(token: String): Result<Unit>
}

/**
 * Use case for resending verification email to current user.
 */
interface ResendVerificationEmailUseCase {
    suspend operator fun invoke(): Result<Unit>
}
