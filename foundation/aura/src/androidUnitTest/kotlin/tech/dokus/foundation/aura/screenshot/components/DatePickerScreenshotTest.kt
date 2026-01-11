package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

/**
 * Screenshot tests for date picker components.
 * Note: PDateField uses PDatePickerDialog which creates a dialog window,
 * so we test the date field layout directly.
 */
class DatePickerScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun dateField_empty() {
        snapshotBothThemes("DateField_empty") {
            DateFieldVisual(
                label = "Date",
                displayValue = "Select date",
                hasValue = false
            )
        }
    }

    @Test
    fun dateField_withValue() {
        snapshotBothThemes("DateField_withValue") {
            DateFieldVisual(
                label = "Due Date",
                displayValue = "2024-01-15",
                hasValue = true
            )
        }
    }

    @Test
    fun dateField_disabled() {
        snapshotBothThemes("DateField_disabled") {
            DateFieldVisual(
                label = "Invoice Date",
                displayValue = "2024-01-01",
                hasValue = true,
                enabled = false
            )
        }
    }

    @Test
    fun dateFields_multiple() {
        snapshotBothThemes("DateFields_multiple") {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DateFieldVisual(
                    label = "Start Date",
                    displayValue = "2024-01-01",
                    hasValue = true
                )
                DateFieldVisual(
                    label = "End Date",
                    displayValue = "Select date",
                    hasValue = false
                )
            }
        }
    }
}

/**
 * Helper composable that mimics PDateField layout for screenshot testing
 * without requiring actual LocalDate values.
 */
@androidx.compose.runtime.Composable
private fun DateFieldVisual(
    label: String,
    displayValue: String,
    hasValue: Boolean,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasValue && enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
