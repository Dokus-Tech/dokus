package tech.dokus.foundation.ktor.database

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Gets the current timestamp as kotlinx.datetime.Instant
 */
fun now(): Instant = Clock.System.now()

/**
 * Converts kotlinx.datetime.Instant to LocalDateTime in the system timezone
 */
fun Instant.toSystemLocalDateTime() = this.toLocalDateTime(TimeZone.currentSystemDefault())
