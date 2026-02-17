package tech.dokus.database.utils

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Instant

/**
 * Convert LocalDateTime to kotlin.time.Instant.
 *
 * Exposed datetime() columns return LocalDateTime. The domain model uses
 * kotlin.time.Instant (kotlinx-datetime 0.7.1 makes it a typealias in commonMain).
 */
fun LocalDateTime.toKotlinxInstant(): Instant = toInstant(TimeZone.UTC)
