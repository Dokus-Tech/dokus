package tech.dokus.features.auth

import kotlinx.coroutines.flow.StateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.dokus.features.auth.usecases.AuthSessionUseCase

/**
 * Initializer for the authentication system.
 * Should be called on app startup to load stored tokens.
 */
class AuthInitializer : KoinComponent {
    private val authSessionUseCase: AuthSessionUseCase by inject()

    /**
     * Initializes the authentication system.
     * Loads stored tokens and validates authentication state.
     */
    suspend fun initialize() {
        // Initialize the auth session which will:
        // 1. Load stored tokens from SecureStorage
        // 2. Validate token expiry
        // 3. Update authentication state
        authSessionUseCase.initialize()
    }

    /**
     * Checks if the user is currently authenticated.
     */
    fun isAuthenticated(): Boolean {
        return authSessionUseCase.isAuthenticated.value
    }

    /**
     * Gets the authentication state as a flow.
     */
    val isAuthenticatedFlow: StateFlow<Boolean> = authSessionUseCase.isAuthenticated
}
