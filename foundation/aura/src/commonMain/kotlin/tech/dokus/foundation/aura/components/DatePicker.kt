@file:OptIn(ExperimentalTime::class)

package tech.dokus.foundation.aura.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_confirm
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A date picker dialog that works with kotlinx.datetime.LocalDate.
 *
 * @param initialDate The initially selected date, or null if none
 * @param onDateSelected Callback when a date is selected (null if dismissed without selection)
 * @param onDismiss Callback when the dialog is dismissed
 * @param confirmText Text for the confirm button (default: "Confirm")
 * @param dismissText Text for the dismiss button (default: "Cancel")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDatePickerDialog(
    initialDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(Res.string.action_confirm),
    dismissText: String = stringResource(Res.string.action_cancel)
) {
    val initialMillis = initialDate?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val instant = Instant.fromEpochMilliseconds(selectedMillis)
                        val localDate = instant.toLocalDateTime(TimeZone.UTC).date
                        onDateSelected(localDate)
                    } else {
                        onDateSelected(null)
                    }
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
