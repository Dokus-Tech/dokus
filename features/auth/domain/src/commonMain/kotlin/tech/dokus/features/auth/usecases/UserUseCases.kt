package tech.dokus.features.auth.usecases

import tech.dokus.domain.Name
import tech.dokus.domain.model.User

/**
 * Use case for retrieving current user.
 */
interface GetCurrentUserUseCase {
    suspend operator fun invoke(): Result<User>
}

/**
 * Use case for updating user profile.
 */
interface UpdateProfileUseCase {
    suspend operator fun invoke(firstName: Name?, lastName: Name?): Result<User>
}
