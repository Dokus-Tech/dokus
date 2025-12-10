package ai.dokus.foundation.database.utils

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Convert LocalDateTime to kotlinx.datetime.Instant.
 *
 * This is a JVM-specific utility for the database module to avoid
 * type resolution issues with the multiplatform domain module.
 */
@Suppress("DEPRECATION")
@OptIn(kotlin.time.ExperimentalTime::class)
fun LocalDateTime.toKotlinxInstant(): Instant {
    val kotlinTimeInstant = toInstant(TimeZone.UTC)
    return Instant.fromEpochSeconds(
        kotlinTimeInstant.epochSeconds,
        kotlinTimeInstant.nanosecondsOfSecond.toLong()
    )
}
