package ai.dokus.app.auth.repository

import ai.dokus.app.auth.manager.TokenManagerMutable
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for authentication operations.
 * Manages login, logout, token refresh, and authentication state.
 */
class AuthRepository(
    private val tokenManager: TokenManagerMutable,
) {
    // Expose authentication state from TokenManager
    val isAuthenticated: StateFlow<Boolean> = tokenManager.isAuthenticated
}