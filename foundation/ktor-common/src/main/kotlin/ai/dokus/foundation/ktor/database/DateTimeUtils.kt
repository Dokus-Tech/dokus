package ai.dokus.foundation.ktor.database

import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.ExperimentalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalTime::class)

/**
 * Gets the current timestamp as kotlin.time.Instant
 */
fun now(): Instant = Clock.System.now()

/**
 * Converts kotlin.time.Instant to LocalDateTime in the system timezone
 */
@OptIn(ExperimentalTime::class)
fun Instant.toSystemLocalDateTime() = this.toLocalDateTime(TimeZone.currentSystemDefault())
