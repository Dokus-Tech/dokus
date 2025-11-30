package ai.dokus.app.auth.manager

import ai.dokus.app.auth.storage.TokenStorage
import ai.dokus.app.auth.utils.JwtDecoder
import ai.dokus.foundation.domain.asbtractions.TokenManager
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.model.auth.JwtClaims
import ai.dokus.foundation.domain.model.auth.LoginResponse
import ai.dokus.foundation.domain.model.auth.TokenStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
                val refreshed = refreshToken()
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
                refreshToken()
            }

            TokenStatus.INVALID -> null
        }
    }

    /**
     * Refreshes the access token using the refresh token.
     */
    override suspend fun refreshToken(): String? = refreshMutex.withLock {
        // Double-check token status after acquiring lock
        val currentToken = tokenStorage.getAccessToken() ?: return null
        val status = jwtDecoder.validateToken(currentToken)

        if (status == TokenStatus.VALID) {
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
            // Treat null response as an auth failure
            onAuthenticationFailed()
        } catch (e: Exception) {
            // Refresh failed, clear tokens
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
