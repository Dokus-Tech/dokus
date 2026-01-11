package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.StatusBadge
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotBothThemes
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class BadgeScreenshotTest {

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun statusBadge_success() {
        paparazzi.snapshotBothThemes("StatusBadge_success") {
            StatusBadge(
                text = "Completed",
                color = Color(0xFF4CAF50)
            )
        }
    }

    @Test
    fun statusBadge_warning() {
        paparazzi.snapshotBothThemes("StatusBadge_warning") {
            StatusBadge(
                text = "Pending",
                color = Color(0xFFFFC107)
            )
        }
    }

    @Test
    fun statusBadge_error() {
        paparazzi.snapshotBothThemes("StatusBadge_error") {
            StatusBadge(
                text = "Failed",
                color = Color(0xFFF44336)
            )
        }
    }

    @Test
    fun statusBadge_info() {
        paparazzi.snapshotBothThemes("StatusBadge_info") {
            StatusBadge(
                text = "In Progress",
                color = Color(0xFF2196F3)
            )
        }
    }

    @Test
    fun statusBadge_allVariants() {
        paparazzi.snapshotBothThemes("StatusBadge_allVariants") {
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
