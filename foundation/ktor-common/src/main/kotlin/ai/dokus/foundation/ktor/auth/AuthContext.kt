package ai.dokus.foundation.ktor.auth

import ai.dokus.foundation.domain.BusinessUserId
import ai.dokus.foundation.domain.TenantId
import ai.dokus.foundation.domain.model.AuthenticationInfo
import kotlinx.coroutines.currentCoroutineContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

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
 * @return BusinessUserId of the authenticated user
 * @throws IllegalStateException if no authentication context is available
 */
suspend fun requireAuthenticatedUserId(): BusinessUserId {
    val authContext = currentCoroutineContext()[AuthContext]
        ?: throw IllegalStateException("Authentication required but no auth context found")
    return authContext.authInfo.userId
}

/**
 * Extension function to get the current authenticated tenant's ID from the coroutine context.
 *
 * @return TenantId of the authenticated user's tenant
 * @throws IllegalStateException if no authentication context is available
 */
suspend fun requireAuthenticatedTenantId(): TenantId {
    val authContext = currentCoroutineContext()[AuthContext]
        ?: throw IllegalStateException("Authentication required but no auth context found")
    return authContext.authInfo.tenantId
}

/**
 * Extension function to get the current authentication info from the coroutine context.
 *
 * @return AuthenticationInfo of the authenticated user
 * @throws IllegalStateException if no authentication context is available
 */
suspend fun requireAuthenticationInfo(): AuthenticationInfo {
    val authContext = currentCoroutineContext()[AuthContext]
        ?: throw IllegalStateException("Authentication required but no auth context found")
    return authContext.authInfo
}

/**
 * Extension function to get the current authentication info from the coroutine context, or null if not authenticated.
 *
 * @return AuthenticationInfo of the authenticated user, or null if not authenticated
 */
suspend fun getAuthenticationInfo(): AuthenticationInfo? {
    return currentCoroutineContext()[AuthContext]?.authInfo
}
