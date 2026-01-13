package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusErrorText
import tech.dokus.foundation.aura.components.common.DokusSelectableRow
import tech.dokus.foundation.aura.components.common.OfflineOverlay
import tech.dokus.foundation.aura.components.common.PSearchActionTopAppBar
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.components.common.PTopAppBarSearchAction
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerCircle
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class CommonScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun pTopAppBar_withTitle() {
        paparazzi.snapshotAllViewports("PTopAppBar_withTitle", viewport) {
            PTopAppBar(
                title = "Page Title",
                navController = null,
                showBackButton = false
            )
        }
    }

    @Test
    fun pTopAppBar_longTitle() {
        paparazzi.snapshotAllViewports("PTopAppBar_longTitle", viewport) {
            PTopAppBar(
                title = "This is a very long page title that should be truncated",
                navController = null,
                showBackButton = false
            )
        }
    }

    @Test
    fun dokusErrorText() {
        paparazzi.snapshotAllViewports("DokusErrorText", viewport) {
            DokusErrorText(text = "Something went wrong. Please try again.")
        }
    }

    @Test
    fun dokusErrorContent_withRetry() {
        paparazzi.snapshotAllViewports("DokusErrorContent_withRetry", viewport) {
            DokusErrorContent(
                title = "Connection Error",
                text = "Unable to connect to the server. Please check your internet connection.",
                retryHandler = tech.dokus.domain.asbtractions.RetryHandler { }
            )
        }
    }

    @Test
    fun dokusErrorContent_withoutRetry() {
        paparazzi.snapshotAllViewports("DokusErrorContent_withoutRetry", viewport) {
            DokusErrorContent(
                text = "This operation cannot be completed.",
                retryHandler = null
            )
        }
    }

    @Test
    fun dokusErrorContent_compact() {
        paparazzi.snapshotAllViewports("DokusErrorContent_compact", viewport) {
            DokusErrorContent(
                text = "Failed to load data",
                retryHandler = tech.dokus.domain.asbtractions.RetryHandler { },
                compact = true
            )
        }
    }

    @Test
    fun shimmerBox() {
        paparazzi.snapshotAllViewports("ShimmerBox", viewport) {
            ShimmerBox(modifier = Modifier.size(200.dp, 100.dp))
        }
    }

    @Test
    fun shimmerCircle() {
        paparazzi.snapshotAllViewports("ShimmerCircle", viewport) {
            ShimmerCircle(size = 48.dp)
        }
    }

    @Test
    fun shimmerLine() {
        paparazzi.snapshotAllViewports("ShimmerLine", viewport) {
            ShimmerLine(modifier = Modifier.width(200.dp))
        }
    }

    @Test
    fun shimmer_cardPlaceholder() {
        paparazzi.snapshotAllViewports("Shimmer_cardPlaceholder", viewport) {
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

    @Test
    fun searchFieldCompact_states() {
        paparazzi.snapshotAllViewports("PSearchFieldCompact_states", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PSearchFieldCompact(
                    value = "",
                    onValueChange = {},
                    placeholder = "Search..."
                )
                PSearchFieldCompact(
                    value = "Quarterly report",
                    onValueChange = {},
                    placeholder = "Search...",
                    onClear = {}
                )
            }
        }
    }

    @Test
    fun searchActionTopAppBar() {
        paparazzi.snapshotAllViewports("PSearchActionTopAppBar", viewport) {
            PSearchActionTopAppBar(
                query = "Invoices",
                onQueryChange = {},
                searchPlaceholder = "Search documents",
                actionText = "Create",
                onActionClick = {},
                actionIcon = Icons.Default.Search
            )
        }
    }

    @Test
    fun topAppBarSearchAction() {
        paparazzi.snapshotAllViewports("PTopAppBarSearchAction", viewport) {
            PTopAppBarSearchAction(
                searchContent = {
                    PSearchFieldCompact(
                        value = "",
                        onValueChange = {},
                        placeholder = "Search"
                    )
                },
                actions = {
                    POutlinedButton(text = "Filter", onClick = {})
                    PPrimaryButton(text = "New", onClick = {})
                }
            )
        }
    }

    @Test
    fun dokusSelectableRow_states() {
        paparazzi.snapshotAllViewports("DokusSelectableRow_states", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DokusSelectableRow(
                    text = "Default",
                    isSelected = false,
                    onClick = {}
                )
                DokusSelectableRow(
                    text = "Selected",
                    isSelected = true,
                    onClick = {}
                )
                DokusSelectableRow(
                    text = "Disabled",
                    isSelected = false,
                    enabled = false,
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun offlineOverlay_states() {
        paparazzi.snapshotAllViewports("OfflineOverlay_states", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OfflineOverlay(isOffline = false) {
                    DokusErrorText(text = "Online content")
                }
                OfflineOverlay(isOffline = true) {
                    DokusErrorText(text = "Offline content")
                }
            }
        }
    }
}
