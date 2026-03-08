package tech.dokus.domain.utils

import kotlin.time.Clock

/**
 * Returns the current time in epoch milliseconds.
 */
val currentTimeMillis: Long
    get() = Clock.System.now().toEpochMilliseconds()
