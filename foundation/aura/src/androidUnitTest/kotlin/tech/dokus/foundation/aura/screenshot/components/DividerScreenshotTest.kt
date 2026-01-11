package tech.dokus.foundation.aura.screenshot.components

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
import tech.dokus.foundation.aura.components.PDashedDivider
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class DividerScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun pDashedDivider() {
        paparazzi.snapshotAllViewports("PDashedDivider", viewport) {
            PDashedDivider(modifier = Modifier.fillMaxWidth())
        }
    }

    @Test
    fun pDashedDivider_inContext() {
        paparazzi.snapshotAllViewports("PDashedDivider_inContext", viewport) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Content above")
                PDashedDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
                Text("Content below")
            }
        }
    }
}
