package tech.dokus.features.auth

import tech.dokus.features.auth.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Initializer for the authentication system.
 * Should be called on app startup to load stored tokens.
 */
class AuthInitializer : KoinComponent {
    private val authRepository: AuthRepository by inject()

    /**
     * Initializes the authentication system.
     * Loads stored tokens and validates authentication state.
     */
    suspend fun initialize() {
        // Initialize the auth repository which will:
        // 1. Load stored tokens from SecureStorage
        // 2. Validate token expiry
        // 3. Update authentication state
        authRepository.initialize()
    }

    /**
     * Checks if the user is currently authenticated.
     */
    fun isAuthenticated(): Boolean {
        return authRepository.isAuthenticated.value
    }

    /**
     * Gets the authentication state as a flow.
     */
    val isAuthenticatedFlow: StateFlow<Boolean> = authRepository.isAuthenticated
}