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
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
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
}
