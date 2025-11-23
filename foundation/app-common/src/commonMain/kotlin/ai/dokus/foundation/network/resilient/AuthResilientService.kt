package ai.dokus.foundation.network.resilient

import ai.dokus.foundation.domain.asbtractions.AuthManager
import ai.dokus.foundation.domain.asbtractions.TokenManager

/**
 * Base class to add auth-aware retry (refresh + retry once, logout on failure) for RPC services.
 */
abstract class AuthResilientService<T : Any>(
    serviceProvider: () -> T,
    tokenManager: TokenManager,
    authManager: AuthManager
) {
    private val authDelegate = AuthenticatedResilientDelegate(
        serviceProvider = serviceProvider,
        tokenManager = tokenManager,
        authManager = authManager
    )

    protected fun service(): T = authDelegate.get()

    protected suspend fun <R> authCall(block: suspend (T) -> R): R =
        authDelegate.withAuthRetry(block)
}
