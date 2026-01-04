package tech.dokus.features.auth.usecases

import kotlinx.coroutines.flow.StateFlow

/**
 * Use case for initializing auth and observing session state.
 */
interface AuthSessionUseCase {
    val isAuthenticated: StateFlow<Boolean>

    suspend fun initialize()
}
