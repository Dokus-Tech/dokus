package ai.dokus.auth.backend.security

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Thread-local holder for authentication information.
 * This is used to pass authentication context from Ktor request handling to RPC service methods.
 */
object RequestAuthHolder {
    private val threadLocal = ThreadLocal<AuthenticationInfo?>()

    /**
     * Set the authentication info for the current thread/request.
     */
    fun set(authInfo: AuthenticationInfo?) {
        threadLocal.set(authInfo)
    }

    /**
     * Get the authentication info for the current thread/request.
     */
    fun get(): AuthenticationInfo? {
        return threadLocal.get()
    }

    /**
     * Clear the authentication info for the current thread/request.
     */
    fun clear() {
        threadLocal.remove()
    }
}

/**
 * Coroutine context element that propagates authentication info through coroutine context.
 */
class AuthContextElement(
    val authInfo: AuthenticationInfo? = RequestAuthHolder.get()
) : ThreadContextElement<AuthenticationInfo?> {
    companion object Key : CoroutineContext.Key<AuthContextElement>

    override val key: CoroutineContext.Key<*>
        get() = Key

    override fun updateThreadContext(context: CoroutineContext): AuthenticationInfo? {
        val oldValue = RequestAuthHolder.get()
        RequestAuthHolder.set(authInfo)
        return oldValue
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: AuthenticationInfo?) {
        RequestAuthHolder.set(oldState)
    }
}
