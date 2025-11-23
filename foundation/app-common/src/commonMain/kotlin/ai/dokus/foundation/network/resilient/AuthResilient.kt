package ai.dokus.foundation.network.resilient

/**
 * Marker for services that wrap an authenticated resilient delegate.
 * Provides shorthand helpers to get the current service or execute an auth-aware call.
 */
interface AuthResilient<T : Any> {
    val authDelegate: AuthenticatedResilientDelegate<T>
}

fun <T : Any> AuthResilient<T>.service(): T = authDelegate.get()

suspend fun <T : Any, R> AuthResilient<T>.authCall(block: suspend (T) -> R): R = authDelegate.withAuthRetry(block)
