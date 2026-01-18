package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.MaterialTheme
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.components.background.CalmParticleField
import tech.dokus.foundation.aura.components.background.WarpJumpEffect
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class BackgroundScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun calmParticleField() {
        paparazzi.snapshotAllViewports("CalmParticleField", viewport) {
            BackgroundCanvas {
                CalmParticleField()
            }
        }
    }

    @Test
    fun warpJumpEffect_active() {
        paparazzi.snapshotAllViewports("WarpJumpEffect_active", viewport) {
            BackgroundCanvas {
                WarpJumpEffect(
                    isActive = true,
                    selectedItemPosition = Offset(200f, 200f)
                )
            }
        }
    }
}

@Composable
private fun BackgroundCanvas(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}
