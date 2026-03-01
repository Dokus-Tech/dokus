package tech.dokus.features.auth.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.auth.LoginResponse
import tech.dokus.domain.model.auth.TokenStatus
import tech.dokus.features.auth.storage.TokenStorage
import tech.dokus.features.auth.utils.JwtDecoder
import tech.dokus.foundation.app.network.isNetworkException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

interface TokenManagerMutable : TokenManager {
    var onTokenRefreshNeeded: (suspend (refreshToken: String, tenantId: TenantId?) -> LoginResponse?)?
    suspend fun initialize()
    suspend fun saveTokens(loginResponse: LoginResponse)
}

/**
 * Manages JWT tokens and authentication state.
 * Handles token storage, validation, and refresh with thread-safe operations.
 */
@OptIn(ExperimentalTime::class)
class TokenManagerImpl(
    private val tokenStorage: TokenStorage,
    private val jwtDecoder: JwtDecoder = JwtDecoder()
) : TokenManager, TokenManagerMutable {

    companion object {
        /** Maximum time an expired token is accepted during offline bootstrap. */
        private val MAX_OFFLINE_STALENESS = 7.days
    }
    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    // Mutex to prevent concurrent token refresh
    private val refreshMutex = Mutex()

    // Callback for token refresh (to be set by the repository)
    override var onTokenRefreshNeeded: (suspend (refreshToken: String, tenantId: TenantId?) -> LoginResponse?)? =
        null

    /**
     * Initializes the token manager by checking local state only (no network calls).
     *
     * A user is considered authenticated if both tokens exist and the access token is
     * a parseable JWT — even if it is expired (up to [MAX_OFFLINE_STALENESS]).
     * This allows the app to start in authenticated UI while offline. Actual token
     * validity is enforced lazily by [getValidAccessToken] before every API call, which
     * will require a successful refresh for expired tokens or trigger logout if the
     * server rejects them.
     *
     * Tokens expired longer than [MAX_OFFLINE_STALENESS] are treated as invalid
     * to bound the window for stale sessions after account deactivation or password change.
     */
    override suspend fun initialize() {
        val accessToken = tokenStorage.getAccessToken()
        val refreshToken = tokenStorage.getRefreshToken()
        if (accessToken == null || refreshToken == null) {
            updateAuthenticationState(false)
            return
        }

        // A user is considered authenticated if both tokens exist and the access token
        // is a parseable JWT — even if it is expired. This allows the app to start in
        // authenticated UI while offline. Actual token validity is enforced lazily by
        // getValidAccessToken() before every API call.
        when (jwtDecoder.validateToken(accessToken)) {
            TokenStatus.VALID, TokenStatus.REFRESH_NEEDED -> updateAuthenticationState(true)
            TokenStatus.EXPIRED -> {
                val claims = jwtDecoder.decode(accessToken)
                val now = Clock.System.now().epochSeconds
                val expiredDuration = (now - (claims?.exp ?: 0)).seconds
                val isStale = claims == null || expiredDuration > MAX_OFFLINE_STALENESS
                updateAuthenticationState(!isStale)
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
        val selectedTenantId = loginResponse.selectedTenantId
        if (selectedTenantId != null) {
            tokenStorage.saveLastSelectedTenantId(selectedTenantId)
        } else {
            tokenStorage.clearLastSelectedTenantId()
        }
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
            TokenStatus.REFRESH_NEEDED -> {
                val refreshedToken = refreshToken(force = false)
                if (refreshedToken != null) {
                    refreshedToken
                } else {
                    // Refresh failed (network error). Fall back to the current token
                    // if it hasn't been cleared by a concurrent onAuthenticationFailed().
                    val tokenStillStored = tokenStorage.getAccessToken() == currentToken
                    if (tokenStillStored) currentToken else null
                }
            }

            TokenStatus.EXPIRED -> {
                refreshToken(force = false)
            }

            TokenStatus.INVALID -> null
        }
    }

    override suspend fun getRefreshToken(): String? {
        return tokenStorage.getRefreshToken()
    }

    override suspend fun getSelectedTenantId(): TenantId? {
        return tokenStorage.getLastSelectedTenantId()
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
            val selectedTenantId = tokenStorage.getLastSelectedTenantId()
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
     * Validates a freshly-saved token and updates authentication state.
     *
     * Unlike [initialize] (which accepts EXPIRED tokens for offline bootstrap),
     * this only treats VALID and REFRESH_NEEDED as authenticated because it is
     * called from [saveTokens] where we just received a fresh token from the server —
     * an expired token here would indicate a server-side issue.
     */
    private fun validateAndUpdateState(token: String) {
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
}
