package tech.dokus.foundation.ktor.security

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.UserId
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext

/**
 * Authentication method constants
 */
object AuthMethod {
    const val JWT = "auth-jwt"
    const val API_KEY = "auth-key"
}

/**
 * Wrap routes with JWT authentication.
 * Similar to Pulse's authenticateJwt helper.
 *
 * Usage:
 * ```
 * route("/api/v1/resource") {
 *     authenticateJwt {
 *         get {
 *             val principal = dokusPrincipal
 *             // ... use principal
 *         }
 *     }
 * }
 * ```
 */
fun Route.authenticateJwt(
    build: Route.() -> Unit
): Route {
    return authenticate(AuthMethod.JWT, optional = false) {
        build()
    }
}

/**
 * Wrap routes with optional JWT authentication.
 * The principal may be null if no token is provided.
 */
fun Route.authenticateJwtOptional(
    build: Route.() -> Unit
): Route {
    return authenticate(AuthMethod.JWT, optional = true) {
        build()
    }
}

/**
 * Wrap routes with API key authentication.
 * For service-to-service communication.
 */
fun Route.authenticateApiKey(
    build: Route.() -> Unit
): Route {
    return authenticate(AuthMethod.API_KEY, optional = false) {
        build()
    }
}

/**
 * Extension property to get the DokusPrincipal from authenticated routes.
 * Throws if no principal is available (route not authenticated or token invalid).
 *
 * Usage:
 * ```
 * get("/me") {
 *     val userId = dokusPrincipal.userId
 *     val tenantId = dokusPrincipal.requireTenantId()
 *     // ...
 * }
 * ```
 */
val RoutingContext.dokusPrincipal: DokusPrincipal
    get() = call.principal<DokusPrincipal>()
        ?: throw DokusException.NotAuthenticated("Authentication required")

/**
 * Extension property to get the DokusPrincipal if available, or null.
 * Useful for optional authentication scenarios.
 */
val RoutingContext.dokusPrincipalOrNull: DokusPrincipal?
    get() = call.principal<DokusPrincipal>()

/**
 * Convenience extension to get the authenticated user's ID.
 */
val RoutingContext.authenticatedUserId: UserId
    get() = dokusPrincipal.userId

/**
 * Convenience extension to get the authenticated user's tenant ID.
 * Throws if no tenant is selected.
 */
val RoutingContext.authenticatedTenantId: TenantId
    get() = dokusPrincipal.requireTenantId()

/**
 * Execute a block with the authenticated principal.
 * This is a convenience method similar to Pulse's withJwt pattern.
 *
 * Usage:
 * ```
 * get("/users") {
 *     withPrincipal { principal ->
 *         val users = userService.findByTenant(principal.requireTenantId())
 *         call.respond(users)
 *     }
 * }
 * ```
 */
suspend inline fun <T> RoutingContext.withPrincipal(
    block: suspend (DokusPrincipal) -> T
): T {
    return block(dokusPrincipal)
}

/**
 * Execute a block with the authenticated principal's tenant ID.
 * Throws if no tenant is selected.
 *
 * Usage:
 * ```
 * get("/invoices") {
 *     withTenant { tenantId ->
 *         val invoices = invoiceService.findByTenant(tenantId)
 *         call.respond(invoices)
 *     }
 * }
 * ```
 */
suspend inline fun <T> RoutingContext.withTenant(
    block: suspend (TenantId) -> T
): T {
    return block(dokusPrincipal.requireTenantId())
}
