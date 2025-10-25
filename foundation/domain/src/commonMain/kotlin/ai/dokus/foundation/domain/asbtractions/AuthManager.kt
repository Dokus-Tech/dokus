package ai.dokus.foundation.domain.asbtractions

import ai.dokus.foundation.domain.model.AuthEvent
import kotlinx.coroutines.flow.SharedFlow

interface AuthManager {
    val authenticationEvents: SharedFlow<AuthEvent>
    suspend fun onAuthenticationFailed()
}