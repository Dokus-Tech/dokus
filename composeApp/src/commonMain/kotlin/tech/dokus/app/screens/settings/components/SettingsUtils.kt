package tech.dokus.app.screens.settings.components

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Generate a preview of the invoice number format based on current settings.
 */
internal fun generateInvoiceNumberPreview(
    prefix: String,
    includeYear: Boolean,
    padding: Int
): String {
    val effectivePrefix = prefix.ifBlank { "INV" }
    val paddedNumber = "1".padStart(padding, '0')
    return if (includeYear) {
        "$effectivePrefix-2026-$paddedNumber"
    } else {
        "$effectivePrefix-$paddedNumber"
    }
}

/**
 * Format a LocalDateTime as a relative time string (e.g., "2h ago", "5d ago").
 */
internal fun formatRelativeTime(dateTime: LocalDateTime?): String {
    if (dateTime == null) return ""

    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    // Simple calculation based on minutes/hours/days difference
    // This is approximate - a real implementation would use a proper duration library
    val diffMinutes = try {
        val nowHour = now.hour + now.dayOfYear * 24
        val thenHour = dateTime.hour + dateTime.dayOfYear * 24
        (nowHour - thenHour) * 60 + (now.minute - dateTime.minute)
    } catch (e: Exception) {
        0
    }

    return when {
        diffMinutes < 1 -> "just now"
        diffMinutes < 60 -> "${diffMinutes}m ago"
        diffMinutes < 1440 -> "${diffMinutes / 60}h ago"
        diffMinutes < 10080 -> "${diffMinutes / 1440}d ago"
        else -> "${diffMinutes / 10080}w ago"
    }
}
