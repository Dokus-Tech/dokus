package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.layout.PCollapsibleSection
import tech.dokus.foundation.aura.components.layout.TwoPaneContainer
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class LayoutScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun pCollapsibleSection_expanded() {
        snapshotBothThemes("PCollapsibleSection_expanded") {
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
        snapshotBothThemes("PCollapsibleSection_collapsed") {
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
        snapshotBothThemes("PCollapsibleSection_multipleSections") {
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
        snapshotBothThemes("TwoPaneContainer") {
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
}
