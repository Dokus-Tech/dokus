package tech.dokus.domain.asbtractions

import tech.dokus.domain.model.AuthEvent
import kotlinx.coroutines.flow.SharedFlow

interface AuthManager {
    val authenticationEvents: SharedFlow<AuthEvent>
    suspend fun onAuthenticationFailed()
}