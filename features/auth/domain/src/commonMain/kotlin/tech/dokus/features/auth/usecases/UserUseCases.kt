package tech.dokus.features.auth.usecases

import kotlinx.coroutines.flow.Flow
import tech.dokus.domain.Name
import tech.dokus.domain.model.User

/**
 * Use case for retrieving current user.
 */
interface GetCurrentUserUseCase {
    suspend operator fun invoke(): Result<User>
}

/**
 * Reactive stream for the current user profile.
 */
interface WatchCurrentUserUseCase {
    operator fun invoke(): Flow<Result<User>>
    fun refresh()
}

/**
 * Use case for updating user profile.
 */
interface UpdateProfileUseCase {
    suspend operator fun invoke(firstName: Name?, lastName: Name?): Result<User>
}
