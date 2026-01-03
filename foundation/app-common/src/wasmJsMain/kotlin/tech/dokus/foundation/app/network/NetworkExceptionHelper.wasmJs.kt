@file:Suppress(
    "CyclomaticComplexMethod",
    "ComplexCondition",
    "ReturnCount"
) // Network error detection requires many checks

package tech.dokus.foundation.app.network

/**
 * WASM/JS implementation of network exception detection.
 * Handles fetch API and XMLHttpRequest errors.
 *
 * Common JS network errors:
 * - TypeError: Failed to fetch
 * - NetworkError
 * - AbortError (for timeouts)
 */
actual fun isNetworkException(throwable: Throwable): Boolean {
    val message = throwable.message?.lowercase() ?: ""

    // JS fetch errors
    if (message.contains("failed to fetch") ||
        message.contains("networkerror") ||
        message.contains("network request failed") ||
        message.contains("load failed") ||
        message.contains("xhr error") ||
        message.contains("cors") ||
        message.contains("timeout")
    ) {
        return true
    }

    // Check for JS error types in the class name
    val className = throwable::class.simpleName?.lowercase() ?: ""
    if (className.contains("typeerror") ||
        className.contains("networkerror") ||
        className.contains("aborterror")
    ) {
        return true
    }

    // Check cause chain
    throwable.cause?.let {
        if (isNetworkException(it)) return true
    }

    // Fallback to message pattern matching
    return hasNetworkExceptionMessage(throwable)
}
