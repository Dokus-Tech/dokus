package tech.dokus.foundation.ktor.security

import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import tech.dokus.domain.exceptions.DokusException

/**
 * Authentication method constants
 */
object AuthMethod {
    const val JWT = "auth-jwt"
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
