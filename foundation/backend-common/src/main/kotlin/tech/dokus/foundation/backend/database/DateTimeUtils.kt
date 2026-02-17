package tech.dokus.foundation.backend.database

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Gets the current timestamp as kotlin.time.Instant
 */
fun now(): Instant = Clock.System.now()

/**
 * Converts Instant to LocalDateTime in the system timezone
 */
fun Instant.toSystemLocalDateTime() = this.toLocalDateTime(TimeZone.currentSystemDefault())
