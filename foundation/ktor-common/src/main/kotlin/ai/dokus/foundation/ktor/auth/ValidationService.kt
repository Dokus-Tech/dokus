package ai.dokus.foundation.ktor.auth

import ai.dokus.foundation.domain.enums.UserRole
import ai.dokus.foundation.domain.enums.UserStatus
import ai.dokus.foundation.domain.model.UserDto
import ai.dokus.foundation.domain.rpc.AuthValidationRemoteService
import ai.dokus.foundation.ktor.utils.extractClientIpAddress
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * WithUser - Main authentication extension function for HTTP routes.
 *
 * This function validates the JWT token by calling the Auth service via RPC
 * and executes the block with the authenticated user context.
 *
 * Note: This is for HTTP routes only. RPC services should use AuthInfoProvider instead.
 *
 * Example usage:
 * ```kotlin
 * post("/invoices") {
 *     withUser(validationService) { user ->
 *         // user.tenantId is available for multi-tenant queries
 *         val invoice = invoiceRepository.createInvoice(request, user.tenantId)
 *         call.respond(HttpStatusCode.Created, invoice)
 *     }
 * }
 * ```
 *
 * @param validationService RPC client for Auth service validation
 * @param sourceModule Name of the calling module (e.g., "Cashflow", "Payment")
 * @param allowedUserRoles List of user roles that are allowed (default: all roles)
 * @param block Lambda to execute with authenticated user
 * @return Result of the block execution
 */
internal suspend inline fun <reified T> RoutingContext.withUser(
    validationService: AuthValidationRemoteService,
    sourceModule: String = "Unknown",
    allowedUserRoles: List<UserRole> = UserRole.all,
    crossinline block: suspend (requester: UserDto.Full) -> T
): T {
    // Extract JWT token from Authorization header
    val token = call.extractJwtToken()

    // Extract client information for audit logging
    val ipAddress = call.extractClientIpAddress()
    val userAgent = call.request.headers["User-Agent"]

    // Create request context
    val context = AuthValidationRemoteService.Context(
        sourceModule = sourceModule,
        ipAddress = ipAddress,
        userAgent = userAgent
    )

    // Call Auth service to validate session and get user data
    val user = try {
        validationService.validateSession(
            token = token,
            requestContext = context,
            allowedUserRoles = allowedUserRoles
        )
    } catch (e: Exception) {
        // Re-throw authentication errors with proper HTTP status
        when (e) {
            is io.ktor.client.plugins.ClientRequestException -> {
                when (e.response.status) {
                    HttpStatusCode.Unauthorized -> {
                        call.respond(HttpStatusCode.Unauthorized, mapOf(
                            "error" to "Unauthorized",
                            "message" to "Invalid or expired token"
                        ))
                        throw e
                    }
                    HttpStatusCode.Forbidden -> {
                        call.respond(HttpStatusCode.Forbidden, mapOf(
                            "error" to "Forbidden",
                            "message" to "Insufficient permissions"
                        ))
                        throw e
                    }
                    else -> throw e
                }
            }
            else -> {
                // Log error and return 500
                call.respond(HttpStatusCode.InternalServerError, mapOf(
                    "error" to "InternalServerError",
                    "message" to "Authentication service unavailable"
                ))
                throw e
            }
        }
    }

    // Execute block with authenticated user
    return block(user)
}

/**
 * Extract JWT token from Authorization header.
 *
 * Expects header format: "Authorization: Bearer <token>"
 *
 * @return JWT token without "Bearer " prefix
 * @throws IllegalArgumentException if Authorization header is missing or malformed
 */
internal fun ApplicationCall.extractJwtToken(): String {
    val authHeader = request.headers["Authorization"]
        ?: throw IllegalArgumentException("Missing Authorization header")

    if (!authHeader.startsWith("Bearer ", ignoreCase = true)) {
        throw IllegalArgumentException("Authorization header must start with 'Bearer '")
    }

    val token = authHeader.substring(7).trim()
    if (token.isBlank()) {
        throw IllegalArgumentException("JWT token is empty")
    }

    return token
}

/**
 * Extract JWT token or return null if missing.
 * Useful for endpoints that support both authenticated and anonymous access.
 */
internal fun ApplicationCall.extractJwtTokenOrNull(): String? {
    return try {
        extractJwtToken()
    } catch (e: IllegalArgumentException) {
        null
    }
}

/**
 * Check if request has JWT token.
 */
internal fun ApplicationCall.hasJwtToken(): Boolean {
    return extractJwtTokenOrNull() != null
}
