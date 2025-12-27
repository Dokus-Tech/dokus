package ai.dokus.foundation.domain.asbtractions

import tech.dokus.domain.model.auth.JwtClaims
import kotlinx.coroutines.flow.StateFlow

interface TokenManager {
    val isAuthenticated: StateFlow<Boolean>
    suspend fun getValidAccessToken(): String?
    suspend fun refreshToken(force: Boolean = false): String?
    suspend fun onAuthenticationFailed()
    suspend fun getCurrentClaims(): JwtClaims?
}
