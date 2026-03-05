<<<<<<<< HEAD:foundation/backend-common/src/main/kotlin/tech/dokus/foundation/backend/utils/RunSuspendCatching.kt
package tech.dokus.foundation.backend.utils
========
@file:Suppress("NOTHING_TO_INLINE")

package tech.dokus.backend.util
>>>>>>>> origin/main:backendApp/src/main/kotlin/tech/dokus/backend/util/RunSuspendCatching.kt

/**
 * Re-export from foundation for backward compatibility.
 * Prefer importing from [tech.dokus.foundation.backend.utils.runSuspendCatching] directly.
 */
suspend inline fun <T> runSuspendCatching(block: () -> T): Result<T> =
    tech.dokus.foundation.backend.utils.runSuspendCatching(block)
