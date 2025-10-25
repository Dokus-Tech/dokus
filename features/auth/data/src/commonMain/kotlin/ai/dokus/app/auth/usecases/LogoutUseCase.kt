package ai.dokus.app.auth.usecases

/**
 * Logs out the current user.
 *
 * Clears everything local first (database, tokens) so logout always works even
 * if the network is down. We try to tell the server too, but that's best-effort.
 * The important part is getting them logged out on this device.
 */
class LogoutUseCase()
