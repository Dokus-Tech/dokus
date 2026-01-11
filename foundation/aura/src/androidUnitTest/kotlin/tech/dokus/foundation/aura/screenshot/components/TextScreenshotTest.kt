package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.components.PTitle
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.components.text.CopyRightText
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class TextScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun pTitle() {
        paparazzi.snapshotAllViewports("PTitle", viewport) {
            PTitle(text = "Page Title")
        }
    }

    @Test
    fun appNameText() {
        paparazzi.snapshotAllViewports("AppNameText", viewport) {
            AppNameText()
        }
    }

    @Test
    fun copyRightText() {
        paparazzi.snapshotAllViewports("CopyRightText", viewport) {
            CopyRightText()
        }
    }

    @Test
    fun sectionTitle() {
        paparazzi.snapshotAllViewports("SectionTitle", viewport) {
            SectionTitle(text = "Section Header")
        }
    }

    @Test
    fun textComponents_allVariants() {
        paparazzi.snapshotAllViewports("Text_allVariants", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PTitle(text = "Main Title")
                SectionTitle(text = "Section Title")
                AppNameText()
                CopyRightText()
            }
        }
    }
}
