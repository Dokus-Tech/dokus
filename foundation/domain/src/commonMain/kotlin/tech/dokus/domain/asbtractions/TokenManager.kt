package tech.dokus.domain.asbtractions

import kotlinx.coroutines.flow.StateFlow
import tech.dokus.domain.model.auth.JwtClaims

interface TokenManager {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun getValidAccessToken(): String?
    suspend fun refreshToken(force: Boolean = false): String?
    suspend fun onAuthenticationFailed()
    suspend fun getCurrentClaims(): JwtClaims?
}
