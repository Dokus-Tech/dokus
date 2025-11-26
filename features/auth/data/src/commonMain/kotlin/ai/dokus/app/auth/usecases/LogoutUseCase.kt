package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.app.core.database.LocalDatabaseCleaner
import ai.dokus.foundation.platform.Logger

/**
 * Logs out the current user.
 *
 * Clears every local cache/database before notifying the backend so logout
 * always wipes the device even if network calls fail.
 */
class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val localDatabaseCleaner: LocalDatabaseCleaner,
) {
    private val logger = Logger.forClass<LogoutUseCase>()

    suspend operator fun invoke(): Result<Unit> {
        logger.d { "Executing logout use case" }

        val clearResult = localDatabaseCleaner.clearAll()
        val logoutResult = runCatching { authRepository.logout() }

        val clearException = clearResult.exceptionOrNull()
        val logoutException = logoutResult.exceptionOrNull()

        return when {
            clearException != null && logoutException != null -> {
                clearException.addSuppressed(logoutException)
                Result.failure(clearException)
            }

            clearException != null -> Result.failure(clearException)
            logoutException != null -> Result.failure(logoutException)
            else -> Result.success(Unit)
        }
    }
}
