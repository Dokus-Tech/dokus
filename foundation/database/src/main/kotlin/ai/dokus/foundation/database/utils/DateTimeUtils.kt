package ai.dokus.foundation.database.utils

import kotlinx.datetime.*

/**
 * Utility functions for date/time handling in the database layer
 * Note: Exposed's kotlin.datetime module uses kotlinx.datetime types directly,
 * so most conversions are not needed. These are here for compatibility.
 */

// Extension functions for cleaner code (no-op since types are already kotlinx.datetime)
fun LocalDate.toKotlinLocalDate(): LocalDate = this
fun LocalDateTime.toKotlinLocalDateTime(): LocalDateTime = this

// For Java interop (if needed in repositories)
fun LocalDate.toJavaLocalDate(): java.time.LocalDate =
    java.time.LocalDate.of(this.year, this.monthNumber, this.dayOfMonth)

fun LocalDateTime.toJavaLocalDateTime(): java.time.LocalDateTime =
    java.time.LocalDateTime.of(
        this.year,
        this.monthNumber,
        this.dayOfMonth,
        this.hour,
        this.minute,
        this.second,
        this.nanosecond
    )

fun Instant.toJavaInstant(): java.time.Instant =
    java.time.Instant.ofEpochSecond(this.epochSeconds, this.nanosecondsOfSecond.toLong())

fun java.time.Instant.toKotlinInstant(): Instant =
    Instant.fromEpochSeconds(this.epochSecond, this.nano)