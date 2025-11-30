package ai.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import ai.dokus.foundation.domain.model.AuthenticationInfo
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Coroutine context element that carries authentication information.
 * This allows services to access the authenticated user's information.
 *
 * Note: For REST routes, prefer using Ktor's Principal pattern via call.principal<DokusPrincipal>()
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
    return requireAuthenticationInfo().userId
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

suspend fun requireAuthenticatedTenantId(): TenantId {
    return requireAuthenticationInfo().tenantId
        ?: throw IllegalStateException("Tenant context required but not selected")
}

/**
 * Executes a block with the authentication context injected into the coroutine context.
 *
 * @param authInfo The authentication information to inject
 * @param block The block to execute with auth context
 * @return The result of the block
 */
suspend fun <T> withAuthContext(authInfo: AuthenticationInfo, block: suspend () -> T): T {
    return withContext(AuthContext(authInfo)) {
        block()
    }
}
