package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

/**
 * Screenshot tests for dropdown and chip components.
 * Tests filter chips and choice chip patterns used throughout the app.
 */
class DropdownScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun filterChip_selected() {
        snapshotBothThemes("FilterChip_selected") {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text("Selected") },
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Test
    fun filterChip_unselected() {
        snapshotBothThemes("FilterChip_unselected") {
            FilterChip(
                selected = false,
                onClick = {},
                label = { Text("Unselected") },
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Test
    fun filterChips_group() {
        snapshotBothThemes("FilterChips_group") {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("All") }
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Draft") }
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Sent") }
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Paid") }
                )
            }
        }
    }

    @Test
    fun filterChips_multiSelect() {
        snapshotBothThemes("FilterChips_multiSelect") {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("Income") }
                )
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text("Expense") }
                )
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text("Transfer") }
                )
            }
        }
    }

    @Test
    fun filterChips_wrapping() {
        snapshotBothThemes("FilterChips_wrapping") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = true, onClick = {}, label = { Text("Option 1") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Option 2") })
                    FilterChip(selected = false, onClick = {}, label = { Text("Option 3") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = false, onClick = {}, label = { Text("Option 4") })
                    FilterChip(selected = true, onClick = {}, label = { Text("Option 5") })
                }
            }
        }
    }
}
