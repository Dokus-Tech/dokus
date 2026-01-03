package tech.dokus.foundation.app.network

/**
 * Checks if the given exception is a network-related error.
 *
 * Network errors include:
 * - Connection refused
 * - Connection timeout
 * - Socket timeout
 * - Unknown host / DNS resolution failure
 * - No internet connection
 * - SSL/TLS handshake failures
 *
 * Platform-specific implementations handle the various exception types:
 * - JVM/Android: java.net.* exceptions
 * - iOS: NSError with NSURLErrorDomain
 * - WASM/JS: fetch API errors
 */
expect fun isNetworkException(throwable: Throwable): Boolean

/**
 * Common network-related exception message patterns.
 * Used as a fallback when platform-specific detection fails.
 */
@Suppress("CyclomaticComplexMethod") // Many message patterns to check by design
internal fun hasNetworkExceptionMessage(throwable: Throwable): Boolean {
    val message = throwable.message?.lowercase() ?: return false
    return message.contains("connection refused") ||
        message.contains("connect timed out") ||
        message.contains("socket timeout") ||
        message.contains("unknown host") ||
        message.contains("no route to host") ||
        message.contains("network is unreachable") ||
        message.contains("unable to resolve host") ||
        message.contains("failed to connect") ||
        message.contains("connection reset") ||
        message.contains("connection closed") ||
        message.contains("ssl handshake") ||
        message.contains("certificate") ||
        message.contains("econnrefused") ||
        message.contains("etimedout") ||
        message.contains("enetunreach")
}
