package ai.dokus.app.auth.manager

import ai.dokus.app.auth.storage.TokenStorage
import ai.dokus.app.auth.utils.JwtDecoder
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.TokenStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.dokus.foundation.app.network.isNetworkException

interface TokenManagerMutable : TokenManager {
    var onTokenRefreshNeeded: (suspend (refreshToken: String, tenantId: TenantId?) -> LoginResponse?)?
    suspend fun initialize()
    suspend fun saveTokens(loginResponse: LoginResponse)
}

/**
 * Manages JWT tokens and authentication state.
 * Handles token storage, validation, and refresh with thread-safe operations.
 */
class TokenManagerImpl(
    private val tokenStorage: TokenStorage,
    private val jwtDecoder: JwtDecoder = JwtDecoder()
) : TokenManager, TokenManagerMutable {
    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Mutex to prevent concurrent token refresh
    private val refreshMutex = Mutex()

    // Callback for token refresh (to be set by the repository)
    override var onTokenRefreshNeeded: (suspend (refreshToken: String, tenantId: TenantId?) -> LoginResponse?)? =
        null

    /**
     * Initializes the token manager by loading stored tokens.
     */
    override suspend fun initialize() {
        val accessToken = tokenStorage.getAccessToken()
        if (accessToken == null) {
            updateAuthenticationState(false)
            return
        }

        when (jwtDecoder.validateToken(accessToken)) {
            TokenStatus.VALID -> updateAuthenticationState(true)
            TokenStatus.REFRESH_NEEDED, TokenStatus.EXPIRED -> {
                val refreshed = runCatching { refreshToken(force = false) }.getOrNull()
                updateAuthenticationState(refreshed != null)
            }

            TokenStatus.INVALID -> updateAuthenticationState(false)
        }
    }

    /**
     * Stores tokens from a login response.
     */
    override suspend fun saveTokens(loginResponse: LoginResponse) {
        tokenStorage.saveTokens(
            accessToken = loginResponse.accessToken,
            refreshToken = loginResponse.refreshToken,
            expiresIn = loginResponse.expiresIn
        )
        validateAndUpdateState(loginResponse.accessToken)
    }

    /**
     * Gets the current valid access token.
     * Attempts refresh if needed.
     */
    override suspend fun getValidAccessToken(): String? {
        val currentToken = tokenStorage.getAccessToken() ?: return null

        val status = jwtDecoder.validateToken(currentToken)

        return when (status) {
            TokenStatus.VALID -> currentToken
            TokenStatus.REFRESH_NEEDED, TokenStatus.EXPIRED -> {
                refreshToken(force = false)
            }

            TokenStatus.INVALID -> null
        }
    }

    /**
     * Refreshes the access token using the refresh token.
     *
     * Error handling:
     * - Auth failures (callback returns null) → logout user
     * - Network failures (callback throws network exception) → keep user logged in, return null
     * - Other failures → logout user (safer default)
     */
    override suspend fun refreshToken(force: Boolean): String? = refreshMutex.withLock {
        // Double-check token status after acquiring lock
        val currentToken = tokenStorage.getAccessToken() ?: return null
        val status = jwtDecoder.validateToken(currentToken)

        if (!force && status == TokenStatus.VALID) {
            return currentToken
        }

        val refreshToken = tokenStorage.getRefreshToken() ?: return null

        // Call the refresh callback
        val refreshCallback = onTokenRefreshNeeded ?: return null

        try {
            val selectedTenantId = jwtDecoder.decode(currentToken)?.tenant?.tenantId
            val response = refreshCallback(refreshToken, selectedTenantId)
            if (response != null) {
                saveTokens(response)
                return response.accessToken
            }
            // Null response means auth failure (401, invalid/expired tokens) → logout
            onAuthenticationFailed()
        } catch (e: Exception) {
            // Network errors should NOT trigger logout - user stays logged in
            if (isNetworkException(e)) {
                // Silently fail - tokens are still valid, just can't reach server
                return null
            }
            // Unknown errors - safer to logout
            onAuthenticationFailed()
        }

        return null
    }

    /**
     * Clears all stored tokens and updates state.
     */
    override suspend fun onAuthenticationFailed() {
        tokenStorage.clearTokens()
        updateAuthenticationState(false)
    }

    /**
     * Checks if the user is currently authenticated.
     */
    suspend fun isAuthenticated(): Boolean {
        val token = tokenStorage.getAccessToken() ?: return false
        val status = jwtDecoder.validateToken(token)
        return status == TokenStatus.VALID || status == TokenStatus.REFRESH_NEEDED
    }

    /**
     * Gets the current user's JWT claims.
     */
    override suspend fun getCurrentClaims(): JwtClaims? {
        val token = tokenStorage.getAccessToken() ?: return null
        return jwtDecoder.decode(token)
    }

    /**
     * Validates token and updates authentication state.
     */
    private suspend fun validateAndUpdateState(token: String) {
        val status = jwtDecoder.validateToken(token)
        val isAuth = status == TokenStatus.VALID || status == TokenStatus.REFRESH_NEEDED
        updateAuthenticationState(isAuth)
    }

    /**
     * Updates the authentication state flows.
     */
    private fun updateAuthenticationState(isAuthenticated: Boolean) {
        _isAuthenticated.value = isAuthenticated
    }

    /**
     * Checks if a token refresh is needed.
     */
    suspend fun needsRefresh(): Boolean {
        val token = tokenStorage.getAccessToken() ?: return true
        return jwtDecoder.needsRefresh(token)
    }

    /**
     * Gets the token expiry time.
     */
    suspend fun getTokenExpiryTime(): Long? {
        return tokenStorage.getTokenExpiry()
    }
}
