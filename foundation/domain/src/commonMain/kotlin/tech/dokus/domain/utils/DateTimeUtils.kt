package tech.dokus.domain.utils

import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Convert LocalDateTime to kotlin.time.Instant.
 */
fun LocalDateTime.toKotlinxInstant(): Instant = toInstant(TimeZone.UTC)

/**
 * Returns the current time in epoch milliseconds.
 */
@OptIn(ExperimentalTime::class)
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
