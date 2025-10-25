package ai.dokus.app.auth.manager

import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.model.AuthEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface AuthManagerMutable : AuthManager {
    suspend fun onUserLogout()
    suspend fun onLoginSuccess()
}

/**
 * Manages authentication events and state across the application.
 * Provides a centralized way to handle authentication failures and force logouts.
 */
internal class AuthManagerImpl : AuthManager, AuthManagerMutable {
    private val _authenticationEvents = MutableSharedFlow<AuthEvent>()
    override val authenticationEvents: SharedFlow<AuthEvent> = _authenticationEvents.asSharedFlow()

    /**
     * Called when authentication fails (e.g., 401 response).
     * This will trigger a force logout across the application.
     */
    override suspend fun onAuthenticationFailed() {
        _authenticationEvents.emit(AuthEvent.ForceLogout)
    }

    /**
     * Called when user logs out voluntarily.
     */
    override suspend fun onUserLogout() {
        _authenticationEvents.emit(AuthEvent.UserLogout)
    }

    /**
     * Called when user successfully logs in.
     */
    override suspend fun onLoginSuccess() {
        _authenticationEvents.emit(AuthEvent.LoginSuccess)
    }
}