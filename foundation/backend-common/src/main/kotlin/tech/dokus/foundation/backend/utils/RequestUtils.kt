package tech.dokus.foundation.backend.utils

import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header

/**
 * Utility functions for extracting information from HTTP requests.
 */

private val IPV4_REGEX = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}$""")
private val IPV6_REGEX = Regex("""^[0-9a-fA-F:]+$""")

/**
 * Validates that the given string looks like a valid IPv4 or IPv6 address.
 */
private fun isValidIpAddress(ip: String): Boolean {
    val trimmed = ip.trim()
    if (trimmed.isEmpty() || trimmed.length > 45) return false
    return IPV4_REGEX.matches(trimmed) || IPV6_REGEX.matches(trimmed)
}

/**
 * Extracts the client IP address from the request.
 *
 * Checks headers in this priority:
 * 1. X-Forwarded-For (first IP in list) - used by proxies and load balancers
 * 2. X-Real-IP - alternative proxy header
 * 3. Direct connection IP - fallback when no proxy headers present
 *
 * Only returns values that look like valid IP addresses. Falls back to the
 * direct connection address if proxy headers contain invalid values.
 *
 * @return Client IP address as a string
 */
fun ApplicationCall.extractClientIpAddress(): String {
    // Check X-Forwarded-For header (may contain multiple IPs)
    request.header("X-Forwarded-For")?.let { forwardedFor ->
        val candidate = forwardedFor.split(",").firstOrNull()?.trim().orEmpty()
        if (isValidIpAddress(candidate)) return candidate
    }

    // Check X-Real-IP header
    request.header("X-Real-IP")?.let { realIp ->
        val candidate = realIp.trim()
        if (isValidIpAddress(candidate)) return candidate
    }

    // Fall back to direct connection IP
    return request.local.remoteAddress
}
