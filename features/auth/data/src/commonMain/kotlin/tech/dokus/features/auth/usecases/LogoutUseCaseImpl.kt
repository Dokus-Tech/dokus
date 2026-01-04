package tech.dokus.features.auth.usecases

import tech.dokus.features.auth.gateway.AuthGateway
import tech.dokus.foundation.app.database.LocalDatabaseCleaner
import tech.dokus.foundation.platform.Logger

/**
 * Logs out the current user.
 *
 * Clears every local cache/database before notifying the backend so logout
 * always wipes the device even if network calls fail.
 */
class LogoutUseCaseImpl(
    private val authGateway: AuthGateway,
    private val localDatabaseCleaner: LocalDatabaseCleaner,
) : LogoutUseCase {
    private val logger = Logger.forClass<LogoutUseCaseImpl>()

    /**
     * Executes the logout flow by clearing local data and notifying the backend.
     *
     * Local cleanup is performed **first** to ensure user data is wiped even if the
     * network call to the backend fails. This guarantees the device is always cleaned
     * when logout is requested, preventing stale or sensitive data from persisting.
     *
     * The operation continues to notify the backend after local cleanup regardless of
     * whether cleanup succeeded, ensuring best-effort server notification.
     *
     * @return [Result.success] with [Unit] if both local cleanup and backend logout succeeded.
     *         [Result.failure] with:
     *         - The local cleanup exception if only local cleanup failed
     *         - The backend logout exception if only backend logout failed
     *         - The local cleanup exception (with backend exception as suppressed) if both failed.
     *           Access the suppressed backend exception via [Throwable.suppressed].
     */
    override suspend operator fun invoke(): Result<Unit> {
        logger.d { "Executing logout use case" }

        val clearResult = localDatabaseCleaner.clearAll()
        val logoutResult = runCatching { authGateway.logout() }

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
