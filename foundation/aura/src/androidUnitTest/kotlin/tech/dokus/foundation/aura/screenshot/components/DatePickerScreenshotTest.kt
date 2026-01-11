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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

/**
 * Screenshot tests for date picker components.
 * Note: PDateField uses PDatePickerDialog which creates a dialog window,
 * so we test the date field layout directly.
 */
@RunWith(Parameterized::class)
class DatePickerScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun dateField_empty() {
        paparazzi.snapshotAllViewports("DateField_empty", viewport) {
            DateFieldVisual(
                label = "Date",
                displayValue = "Select date",
                hasValue = false
            )
        }
    }

    @Test
    fun dateField_withValue() {
        paparazzi.snapshotAllViewports("DateField_withValue", viewport) {
            DateFieldVisual(
                label = "Due Date",
                displayValue = "2024-01-15",
                hasValue = true
            )
        }
    }

    @Test
    fun dateField_disabled() {
        paparazzi.snapshotAllViewports("DateField_disabled", viewport) {
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
        paparazzi.snapshotAllViewports("DateFields_multiple", viewport) {
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
