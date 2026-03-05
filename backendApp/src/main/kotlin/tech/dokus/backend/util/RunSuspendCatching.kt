@file:Suppress("NOTHING_TO_INLINE")

package tech.dokus.backend.util

/**
 * Re-export from foundation for backward compatibility.
 * Prefer importing from [tech.dokus.foundation.backend.utils.runSuspendCatching] directly.
 */
suspend inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    tech.dokus.foundation.backend.utils.runSuspendCatching(block)
