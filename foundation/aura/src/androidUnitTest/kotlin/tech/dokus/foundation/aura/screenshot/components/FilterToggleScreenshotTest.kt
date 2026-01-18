package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.components.filter.DokusFilterToggle
import tech.dokus.foundation.aura.components.filter.DokusFilterToggleRow
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

/**
 * Parameterized screenshot tests for DokusFilterToggle components.
 * Runs at all viewport sizes: COMPACT, MEDIUM, EXPANDED.
 */
@RunWith(Parameterized::class)
class FilterToggleScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun filterToggle_selected() {
        paparazzi.snapshotAllViewports("FilterToggle_selected", viewport) {
            DokusFilterToggle(
                selected = true,
                onClick = {},
                label = "Selected"
            )
        }
    }

    @Test
    fun filterToggle_unselected() {
        paparazzi.snapshotAllViewports("FilterToggle_unselected", viewport) {
            DokusFilterToggle(
                selected = false,
                onClick = {},
                label = "Unselected"
            )
        }
    }

    @Test
    fun filterToggle_withBadge() {
        paparazzi.snapshotAllViewports("FilterToggle_withBadge", viewport) {
            DokusFilterToggle(
                selected = true,
                onClick = {},
                label = "Needs attention",
                badge = 5
            )
        }
    }

    @Test
    fun filterToggle_withBadgeUnselected() {
        paparazzi.snapshotAllViewports("FilterToggle_withBadgeUnselected", viewport) {
            DokusFilterToggle(
                selected = false,
                onClick = {},
                label = "Needs attention",
                badge = 12
            )
        }
    }

    @Test
    fun filterToggleRow_allStates() {
        paparazzi.snapshotAllViewports("FilterToggleRow_allStates", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row with one selected
                DokusFilterToggleRow {
                    DokusFilterToggle(selected = true, onClick = {}, label = "All")
                    DokusFilterToggle(selected = false, onClick = {}, label = "Active")
                    DokusFilterToggle(selected = false, onClick = {}, label = "Completed")
                }

                // Row with badge
                DokusFilterToggleRow {
                    DokusFilterToggle(selected = false, onClick = {}, label = "All")
                    DokusFilterToggle(selected = true, onClick = {}, label = "Needs attention", badge = 34)
                    DokusFilterToggle(selected = false, onClick = {}, label = "Confirmed")
                }
            }
        }
    }

    @Test
    fun filterToggleRow_documentFilters() {
        paparazzi.snapshotAllViewports("FilterToggleRow_documentFilters", viewport) {
            DokusFilterToggleRow(modifier = Modifier.padding(16.dp)) {
                DokusFilterToggle(selected = true, onClick = {}, label = "All")
                DokusFilterToggle(selected = false, onClick = {}, label = "Needs attention", badge = 5)
                DokusFilterToggle(selected = false, onClick = {}, label = "Confirmed")
            }
        }
    }

    @Test
    fun filterToggleRow_cashflowFilters() {
        paparazzi.snapshotAllViewports("FilterToggleRow_cashflowFilters", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // View mode row
                DokusFilterToggleRow {
                    DokusFilterToggle(selected = true, onClick = {}, label = "Upcoming")
                    DokusFilterToggle(selected = false, onClick = {}, label = "History")
                }

                // Direction row
                DokusFilterToggleRow {
                    DokusFilterToggle(selected = true, onClick = {}, label = "All")
                    DokusFilterToggle(selected = false, onClick = {}, label = "In")
                    DokusFilterToggle(selected = false, onClick = {}, label = "Out")
                }
            }
        }
    }
}
