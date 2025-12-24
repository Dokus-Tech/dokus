package tech.dokus.foundation.app.network

/**
 * iOS implementation of network exception detection.
 * Darwin networking errors are wrapped in Kotlin exceptions.
 *
 * Common NSURLErrorDomain error codes:
 * - -1001: NSURLErrorTimedOut
 * - -1003: NSURLErrorCannotFindHost
 * - -1004: NSURLErrorCannotConnectToHost
 * - -1005: NSURLErrorNetworkConnectionLost
 * - -1006: NSURLErrorDNSLookupFailed
 * - -1009: NSURLErrorNotConnectedToInternet
 * - -1200: NSURLErrorSecureConnectionFailed
 */
actual fun isNetworkException(throwable: Throwable): Boolean {
    // iOS Ktor Darwin engine wraps NSError in exceptions
    // Check message patterns for common network errors
    val message = throwable.message?.lowercase() ?: ""

    // Check for NSURLError codes in the message
    val isNSURLError = message.contains("nsurlerror") ||
        message.contains("nsposixerrordomain") ||
        message.contains("kCFErrorDomainCFNetwork".lowercase())

    if (isNSURLError) return true

    // Check cause chain
    throwable.cause?.let {
        if (isNetworkException(it)) return true
    }

    // Fallback to message pattern matching
    return hasNetworkExceptionMessage(throwable)
}
