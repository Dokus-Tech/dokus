package tech.dokus.backend.util

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching] but re-throws [CancellationException] to preserve structured concurrency.
 * Use this instead of [runCatching] in suspend functions.
 */
@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
