package ai.dokus.auth.backend.security

import ai.dokus.foundation.domain.UserId
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * Coroutine context element that carries authentication information.
 * This allows RPC services to access the authenticated user's information.
 */
class AuthContext(
    val authInfo: AuthenticationInfo
) : AbstractCoroutineContextElement(AuthContext) {
    companion object Key : CoroutineContext.Key<AuthContext>
}

/**
 * Extension function to get the current authenticated user's ID from the coroutine context.
 *
 * @return UserId of the authenticated user
 * @throws IllegalStateException if no authentication context is available
 */
suspend fun requireAuthenticatedUserId(): UserId {
    val authContext = coroutineContext[AuthContext]
        ?: throw IllegalStateException("Authentication required but no auth context found")
    return authContext.authInfo.userId
}

/**
 * Extension function to get the current authentication info from the coroutine context.
 *
 * @return AuthenticationInfo of the authenticated user
 * @throws IllegalStateException if no authentication context is available
 */
suspend fun requireAuthenticationInfo(): AuthenticationInfo {
    val authContext = coroutineContext[AuthContext]
        ?: throw IllegalStateException("Authentication required but no auth context found")
    return authContext.authInfo
}

/**
 * Extension function to get the current authentication info from the coroutine context, or null if not authenticated.
 *
 * @return AuthenticationInfo of the authenticated user, or null if not authenticated
 */
suspend fun getAuthenticationInfo(): AuthenticationInfo? {
    return coroutineContext[AuthContext]?.authInfo
}
