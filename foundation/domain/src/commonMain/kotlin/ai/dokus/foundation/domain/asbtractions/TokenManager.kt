package ai.dokus.foundation.domain.asbtractions

import kotlinx.coroutines.flow.StateFlow

interface TokenManager {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun getValidAccessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun onAuthenticationFailed()
}