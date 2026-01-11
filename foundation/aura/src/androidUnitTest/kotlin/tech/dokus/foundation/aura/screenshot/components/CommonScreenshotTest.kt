package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusErrorText
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerCircle
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class CommonScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun pTopAppBar_withTitle() {
        snapshotBothThemes("PTopAppBar_withTitle") {
            PTopAppBar(
                title = "Page Title",
                navController = null,
                showBackButton = false
            )
        }
    }

    @Test
    fun pTopAppBar_longTitle() {
        snapshotBothThemes("PTopAppBar_longTitle") {
            PTopAppBar(
                title = "This is a very long page title that should be truncated",
                navController = null,
                showBackButton = false
            )
        }
    }

    @Test
    fun dokusErrorText() {
        snapshotBothThemes("DokusErrorText") {
            DokusErrorText(text = "Something went wrong. Please try again.")
        }
    }

    @Test
    fun dokusErrorContent_withRetry() {
        snapshotBothThemes("DokusErrorContent_withRetry") {
            DokusErrorContent(
                title = "Connection Error",
                text = "Unable to connect to the server. Please check your internet connection.",
                retryHandler = tech.dokus.domain.asbtractions.RetryHandler { }
            )
        }
    }

    @Test
    fun dokusErrorContent_withoutRetry() {
        snapshotBothThemes("DokusErrorContent_withoutRetry") {
            DokusErrorContent(
                text = "This operation cannot be completed.",
                retryHandler = null
            )
        }
    }

    @Test
    fun dokusErrorContent_compact() {
        snapshotBothThemes("DokusErrorContent_compact") {
            DokusErrorContent(
                text = "Failed to load data",
                retryHandler = tech.dokus.domain.asbtractions.RetryHandler { },
                compact = true
            )
        }
    }

    @Test
    fun shimmerBox() {
        snapshotBothThemes("ShimmerBox") {
            ShimmerBox(modifier = Modifier.size(200.dp, 100.dp))
        }
    }

    @Test
    fun shimmerCircle() {
        snapshotBothThemes("ShimmerCircle") {
            ShimmerCircle(size = 48.dp)
        }
    }

    @Test
    fun shimmerLine() {
        snapshotBothThemes("ShimmerLine") {
            ShimmerLine(modifier = Modifier.width(200.dp))
        }
    }

    @Test
    fun shimmer_cardPlaceholder() {
        snapshotBothThemes("Shimmer_cardPlaceholder") {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShimmerCircle(size = 48.dp)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerLine(modifier = Modifier.width(150.dp))
                        ShimmerLine(modifier = Modifier.width(100.dp))
                    }
                }
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
            }
        }
    }
}
