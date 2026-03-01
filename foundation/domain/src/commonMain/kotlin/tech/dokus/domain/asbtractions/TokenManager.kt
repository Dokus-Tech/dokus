package tech.dokus.domain.asbtractions

import kotlinx.coroutines.flow.StateFlow
import tech.dokus.domain.ids.TenantId

interface TokenManager {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun getValidAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun getSelectedTenantId(): TenantId?
    suspend fun refreshToken(force: Boolean = false): String?
    suspend fun onAuthenticationFailed()
}
