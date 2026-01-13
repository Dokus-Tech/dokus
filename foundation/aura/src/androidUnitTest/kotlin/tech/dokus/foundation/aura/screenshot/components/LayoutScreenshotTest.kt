package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.layout.DokusExpandableAction
import tech.dokus.foundation.aura.components.layout.DokusPanelListItem
import tech.dokus.foundation.aura.components.layout.DokusTabbedPanel
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.layout.PCollapsibleSection
import tech.dokus.foundation.aura.components.layout.TwoPaneContainer
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class LayoutScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun pCollapsibleSection_expanded() {
        paparazzi.snapshotAllViewports("PCollapsibleSection_expanded", viewport) {
            PCollapsibleSection(
                title = "Section Title",
                isExpanded = true,
                onToggle = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Content line 1")
                    Text("Content line 2")
                    Text("Content line 3")
                }
            }
        }
    }

    @Test
    fun pCollapsibleSection_collapsed() {
        paparazzi.snapshotAllViewports("PCollapsibleSection_collapsed", viewport) {
            PCollapsibleSection(
                title = "Collapsed Section",
                isExpanded = false,
                onToggle = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("This content is hidden")
            }
        }
    }

    @Test
    fun pCollapsibleSection_multipleSections() {
        paparazzi.snapshotAllViewports("PCollapsibleSection_multipleSections", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PCollapsibleSection(
                    title = "First Section",
                    isExpanded = true,
                    onToggle = {}
                ) {
                    Text("First section content")
                }
                PCollapsibleSection(
                    title = "Second Section",
                    isExpanded = false,
                    onToggle = {}
                ) {
                    Text("Second section content")
                }
                PCollapsibleSection(
                    title = "Third Section",
                    isExpanded = true,
                    onToggle = {}
                ) {
                    Text("Third section content")
                }
            }
        }
    }

    @Test
    fun twoPaneContainer() {
        paparazzi.snapshotAllViewports("TwoPaneContainer", viewport) {
            TwoPaneContainer(
                left = {
                    Text(
                        "Left Pane",
                        modifier = Modifier.padding(16.dp)
                    )
                },
                right = {
                    Text(
                        "Right Pane",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            )
        }
    }

    @Test
    fun dokusTabbedPanel_basic() {
        val tabs = listOf("Overview", "Details", "History")
        paparazzi.snapshotAllViewports("DokusTabbedPanel_basic", viewport) {
            DokusTabbedPanel(
                title = "Activity",
                tabs = tabs,
                selectedTab = "Overview",
                onTabSelected = {},
                tabLabel = { it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DokusPanelListItem(
                        title = "Payment received",
                        supportingText = "Invoice #001"
                    )
                    DokusPanelListItem(
                        title = "Reminder sent",
                        supportingText = "Invoice #002"
                    )
                }
            }
        }
    }

    @Test
    fun dokusTableLayout_basic() {
        val columns = listOf(
            DokusTableColumnSpec(weight = 2f),
            DokusTableColumnSpec(weight = 1f),
            DokusTableColumnSpec(weight = 1f, horizontalAlignment = Alignment.End)
        )
        paparazzi.snapshotAllViewports("DokusTableLayout_basic", viewport) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DokusTableRow {
                    DokusTableCell(column = columns[0]) { Text("Invoice #001") }
                    DokusTableCell(column = columns[1]) { Text("Due Jan 20") }
                    DokusTableCell(column = columns[2]) { Text("EUR 1,230.00") }
                }
                DokusTableRow {
                    DokusTableCell(column = columns[0]) { Text("Invoice #002") }
                    DokusTableCell(column = columns[1]) { Text("Due Feb 04") }
                    DokusTableCell(column = columns[2]) { Text("EUR 850.00") }
                }
            }
        }
    }

    @Test
    fun dokusExpandableAction_states() {
        paparazzi.snapshotAllViewports("DokusExpandableAction_states", viewport) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DokusExpandableAction(
                    isExpanded = false,
                    onToggleExpand = {},
                    subtext = { Text("Additional options") },
                    primaryAction = {
                        PPrimaryButton(text = "Submit", onClick = {})
                    },
                    expandedContent = {
                        Text("Expanded content goes here")
                    }
                )
                DokusExpandableAction(
                    isExpanded = true,
                    onToggleExpand = {},
                    primaryAction = {
                        PPrimaryButton(text = "Submit", onClick = {})
                    },
                    expandedContent = {
                        Text("Expanded content goes here")
                    }
                )
            }
        }
    }
}
