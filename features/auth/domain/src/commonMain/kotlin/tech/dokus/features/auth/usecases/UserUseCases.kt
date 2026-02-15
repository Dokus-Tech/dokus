package tech.dokus.features.auth.usecases

import kotlinx.coroutines.flow.Flow
import tech.dokus.domain.Name
import tech.dokus.domain.Password
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.SessionDto

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

/**
 * Use case for changing current account password.
 */
interface ChangePasswordUseCase {
    suspend operator fun invoke(currentPassword: Password, newPassword: Password): Result<Unit>
}

/**
 * Use case for listing active user sessions.
 */
interface ListSessionsUseCase {
    suspend operator fun invoke(): Result<List<SessionDto>>
}

/**
 * Use case for revoking one specific session.
 */
interface RevokeSessionUseCase {
    suspend operator fun invoke(sessionId: SessionId): Result<Unit>
}

/**
 * Use case for revoking all sessions except current.
 */
interface RevokeOtherSessionsUseCase {
    suspend operator fun invoke(): Result<Unit>
}
