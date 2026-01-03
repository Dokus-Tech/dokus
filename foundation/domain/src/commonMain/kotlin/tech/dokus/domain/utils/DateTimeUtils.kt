package tech.dokus.domain.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Convert LocalDateTime to kotlinx.datetime.Instant.
 * Note: kotlinx.datetime.Instant is deprecated in favor of kotlin.time.Instant,
 * but we keep this for compatibility with existing code.
 */
@Suppress("DEPRECATION")
@OptIn(ExperimentalTime::class)
fun LocalDateTime.toKotlinxInstant(): Instant {
    val kotlinTimeInstant = toInstant(TimeZone.UTC)
    return Instant.fromEpochSeconds(
        kotlinTimeInstant.epochSeconds,
        kotlinTimeInstant.nanosecondsOfSecond.toLong()
    )
}

/**
 * Returns the current time in epoch milliseconds.
 */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
