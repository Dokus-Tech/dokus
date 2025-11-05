@file:OptIn(ExperimentalTime::class)

package ai.dokus.reporting.backend.database.utils

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.time.ExperimentalTime

/**
 * Utility functions for date/time handling in the database layer
 * Note: Exposed's kotlin.datetime module uses kotlinx.datetime types directly,
 * so most conversions are not needed. These are here for compatibility.
 */

fun LocalDate.toKotlinLocalDate(): LocalDate = this
fun LocalDateTime.toKotlinLocalDateTime(): LocalDateTime = this