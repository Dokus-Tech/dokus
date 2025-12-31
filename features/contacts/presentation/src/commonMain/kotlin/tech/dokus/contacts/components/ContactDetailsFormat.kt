package tech.dokus.contacts.components

import kotlinx.datetime.LocalDateTime

internal fun formatDateTime(dateTime: LocalDateTime): String {
    val day = dateTime.day.toString().padStart(2, '0')
    val month = (dateTime.month.ordinal + 1).toString().padStart(2, '0')
    val year = dateTime.year
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')

    return "$day/$month/$year $hour:$minute"
}
