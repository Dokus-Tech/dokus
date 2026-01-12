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
import org.jetbrains.compose.resources.StringResource
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.aura.resources.Res
import tech.dokus.foundation.aura.components.chips.PChoiceChips
import tech.dokus.foundation.aura.components.dropdown.FilterOption
import tech.dokus.foundation.aura.components.dropdown.PFilterDropdown
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

/**
 * Screenshot tests for dropdown and chip components.
 * Tests filter chips and choice chip patterns used throughout the app.
 */
@RunWith(Parameterized::class)
class DropdownScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun filterChip_selected() {
        paparazzi.snapshotAllViewports("FilterChip_selected", viewport) {
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
        paparazzi.snapshotAllViewports("FilterChip_unselected", viewport) {
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
        paparazzi.snapshotAllViewports("FilterChips_group", viewport) {
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
        paparazzi.snapshotAllViewports("FilterChips_multiSelect", viewport) {
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
        paparazzi.snapshotAllViewports("FilterChips_wrapping", viewport) {
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

    @Test
    fun pFilterDropdown_collapsed() {
        paparazzi.snapshotAllViewports("PFilterDropdown_collapsed", viewport) {
            PFilterDropdown(
                label = "Filter:",
                selectedOption = SortOption.Documents,
                options = SortOption.entries.toList(),
                onOptionSelected = {}
            )
        }
    }

    @Test
    fun pChoiceChips_group() {
        val options = listOf("All", "Open", "Paid", "Overdue")
        paparazzi.snapshotAllViewports("PChoiceChips_group", viewport) {
            PChoiceChips(
                options = options,
                selected = "Paid",
                onSelect = {},
                optionLabel = { it },
                label = "Status"
            )
        }
    }
}

private enum class SortOption(override val labelRes: StringResource) : FilterOption {
    Documents(Res.string.nav_documents),
    Contacts(Res.string.nav_contacts),
    Team(Res.string.nav_team)
}
