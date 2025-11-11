package ai.dokus.app.auth.usecases

import ai.dokus.app.auth.repository.AuthRepository
import ai.dokus.foundation.platform.Logger

/**
 * Logs out the current user.
 *
 * Clears everything local first (database, tokens) so logout always works even
 * if the network is down. We try to tell the server too, but that's best-effort.
 * The important part is getting them logged out on this device.
 */
class LogoutUseCase(
    private val authRepository: AuthRepository
) {
    private val logger = Logger.forClass<LogoutUseCase>()

    suspend operator fun invoke() {
        logger.d { "Executing logout use case" }
        authRepository.logout()
    }
}
