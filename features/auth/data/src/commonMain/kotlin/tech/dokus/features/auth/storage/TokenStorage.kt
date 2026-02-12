package tech.dokus.features.auth.storage

import kotlinx.coroutines.flow.Flow
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.sstorage.SecureStorage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Token storage implementation using SecureStorage.
 * Provides secure storage for JWT tokens across all platforms.
 */
class TokenStorage(
    private val secureStorage: SecureStorage
) {
    companion object {
        private const val KEY_ACCESS_TOKEN = "auth.access_token"
        private const val KEY_REFRESH_TOKEN = "auth.refresh_token"
        private const val KEY_TOKEN_EXPIRY = "auth.token_expiry"
        private const val KEY_LAST_SELECTED_TENANT_ID = "auth.last_selected_tenant_id"
    }

    /**
     * Stores the access token securely.
     */
    suspend fun saveAccessToken(token: String) {
        secureStorage.set(KEY_ACCESS_TOKEN, token)
    }

    /**
     * Retrieves the stored access token.
     */
    suspend fun getAccessToken(): String? {
        return secureStorage.get<String>(KEY_ACCESS_TOKEN)
    }

    /**
     * Observes changes to the access token.
     */
    fun observeAccessToken(): Flow<String?> {
        return secureStorage.subscribe<String>(KEY_ACCESS_TOKEN)
    }

    /**
     * Stores the refresh token securely.
     */
    suspend fun saveRefreshToken(token: String) {
        secureStorage.set(KEY_REFRESH_TOKEN, token)
    }

    /**
     * Retrieves the stored refresh token.
     */
    suspend fun getRefreshToken(): String? {
        return secureStorage.get<String>(KEY_REFRESH_TOKEN)
    }

    /**
     * Observes changes to the refresh token.
     */
    fun observeRefreshToken(): Flow<String?> {
        return secureStorage.subscribe<String>(KEY_REFRESH_TOKEN)
    }

    /**
     * Stores the token expiry time.
     */
    suspend fun saveTokenExpiry(expiryTime: Long) {
        secureStorage.set(KEY_TOKEN_EXPIRY, expiryTime)
    }

    /**
     * Retrieves the token expiry time.
     */
    suspend fun getTokenExpiry(): Long? {
        return secureStorage.get<Long>(KEY_TOKEN_EXPIRY)
    }

    /**
     * Clears all stored tokens.
     */
    suspend fun clearTokens() {
        secureStorage.remove(KEY_ACCESS_TOKEN)
        secureStorage.remove(KEY_REFRESH_TOKEN)
        secureStorage.remove(KEY_TOKEN_EXPIRY)
        secureStorage.remove(KEY_LAST_SELECTED_TENANT_ID)
    }

    /**
     * Checks if tokens are stored.
     */
    suspend fun hasTokens(): Boolean {
        return getAccessToken() != null && getRefreshToken() != null
    }

    /**
     * Stores all tokens from a login response.
     */
    @OptIn(ExperimentalTime::class)
    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresIn: Long) {
        saveAccessToken(accessToken)
        saveRefreshToken(refreshToken)

        // Calculate absolute expiry time
        val currentTime = Clock.System.now().epochSeconds
        val expiryTime = currentTime + expiresIn
        saveTokenExpiry(expiryTime)
    }

    suspend fun saveLastSelectedTenantId(tenantId: TenantId) {
        secureStorage.set(KEY_LAST_SELECTED_TENANT_ID, tenantId.toString())
    }

    suspend fun getLastSelectedTenantId(): TenantId? {
        val rawValue = secureStorage.get<String>(KEY_LAST_SELECTED_TENANT_ID)
            ?: return null
        return runCatching { TenantId.parse(rawValue) }.getOrNull()
    }
}
