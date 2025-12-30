package tech.dokus.foundation.backend.middleware

import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import tech.dokus.foundation.backend.utils.extractClientIpAddress

val RateLimitPlugin = createRouteScopedPlugin(
    name = "RateLimitPlugin",
    createConfiguration = ::RateLimitConfiguration
) {
    // TODO: Implement rate limiting logic using Redis or in-memory store
    // This plugin will:
    // 1. Check if the client has exceeded rate limits (by IP address or user ID)
    // 2. Return 429 Too Many Requests if rate limit exceeded
    // 3. Use a sliding window or token bucket algorithm
}

class RateLimitConfiguration

/**
 * Gets the rate limit key for this request (currently IP address).
 * Can be extended to use user ID for authenticated requests.
 */
fun ApplicationCall.getRateLimitKey(): String {
    return extractClientIpAddress()
}