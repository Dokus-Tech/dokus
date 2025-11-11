package ai.dokus.invoicing.backend.database.mappers

import kotlinx.datetime.LocalDate as KotlinLocalDate
import kotlinx.datetime.LocalDateTime as KotlinLocalDateTime
import java.time.LocalDate as JavaLocalDate
import java.time.LocalDateTime as JavaLocalDateTime

/**
 * Extension functions for converting between kotlinx.datetime and java.time types
 * Required for Exposed database interactions
 */

fun KotlinLocalDate.toJavaLocalDate(): JavaLocalDate =
    JavaLocalDate.of(this.year, this.monthNumber, this.dayOfMonth)

fun JavaLocalDate.toKotlinLocalDate(): KotlinLocalDate =
    KotlinLocalDate(this.year, this.monthValue, this.dayOfMonth)

fun KotlinLocalDateTime.toJavaLocalDateTime(): JavaLocalDateTime =
    JavaLocalDateTime.of(
        this.year,
        this.monthNumber,
        this.dayOfMonth,
        this.hour,
        this.minute,
        this.second,
        this.nanosecond
    )

fun JavaLocalDateTime.toKotlinLocalDateTime(): KotlinLocalDateTime =
    KotlinLocalDateTime(
        this.year,
        this.monthValue,
        this.dayOfMonth,
        this.hour,
        this.minute,
        this.second,
        this.nano
    )