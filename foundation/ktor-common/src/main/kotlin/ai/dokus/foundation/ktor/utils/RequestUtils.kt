package ai.dokus.foundation.ktor.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header

/**
 * Utility functions for extracting information from HTTP requests.
 */

/**
 * Extracts the client IP address from the request.
 *
 * Checks headers in this priority:
 * 1. X-Forwarded-For (first IP in list) - used by proxies and load balancers
 * 2. X-Real-IP - alternative proxy header
 * 3. Direct connection IP - fallback when no proxy headers present
 *
 * @return Client IP address as a string
 */
fun ApplicationCall.extractClientIpAddress(): String {
    // Check X-Forwarded-For header (may contain multiple IPs)
    request.header("X-Forwarded-For")?.let { forwardedFor ->
        return forwardedFor.split(",").firstOrNull()?.trim() ?: ""
    }

    // Check X-Real-IP header
    request.header("X-Real-IP")?.let { realIp ->
        return realIp.trim()
    }

    // Fall back to direct connection IP
    return request.local.remoteAddress
}
