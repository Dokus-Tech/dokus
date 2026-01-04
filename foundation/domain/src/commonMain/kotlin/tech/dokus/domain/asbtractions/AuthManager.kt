package tech.dokus.domain.asbtractions

import kotlinx.coroutines.flow.SharedFlow
import tech.dokus.domain.model.auth.AuthEvent

interface AuthManager {
    val authenticationEvents: SharedFlow<AuthEvent>
    suspend fun onAuthenticationFailed()
}
