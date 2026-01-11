package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.components.StatusBadge
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class BadgeScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun statusBadge_success() {
        paparazzi.snapshotAllViewports("StatusBadge_success", viewport) {
            StatusBadge(
                text = "Completed",
                color = Color(0xFF4CAF50)
            )
        }
    }

    @Test
    fun statusBadge_warning() {
        paparazzi.snapshotAllViewports("StatusBadge_warning", viewport) {
            StatusBadge(
                text = "Pending",
                color = Color(0xFFFFC107)
            )
        }
    }

    @Test
    fun statusBadge_error() {
        paparazzi.snapshotAllViewports("StatusBadge_error", viewport) {
            StatusBadge(
                text = "Failed",
                color = Color(0xFFF44336)
            )
        }
    }

    @Test
    fun statusBadge_info() {
        paparazzi.snapshotAllViewports("StatusBadge_info", viewport) {
            StatusBadge(
                text = "In Progress",
                color = Color(0xFF2196F3)
            )
        }
    }

    @Test
    fun statusBadge_allVariants() {
        paparazzi.snapshotAllViewports("StatusBadge_allVariants", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatusBadge(text = "Draft", color = Color(0xFF9E9E9E))
                StatusBadge(text = "Pending", color = Color(0xFFFFC107))
                StatusBadge(text = "Sent", color = Color(0xFF2196F3))
                StatusBadge(text = "Paid", color = Color(0xFF4CAF50))
                StatusBadge(text = "Overdue", color = Color(0xFFF44336))
                StatusBadge(text = "Cancelled", color = Color(0xFF757575))
            }
        }
    }
}
