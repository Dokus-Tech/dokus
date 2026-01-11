package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.PTitle
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.components.text.CopyRightText
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotBothThemes
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class TextScreenshotTest {

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun pTitle() {
        paparazzi.snapshotBothThemes("PTitle") {
            PTitle(text = "Page Title")
        }
    }

    @Test
    fun appNameText() {
        paparazzi.snapshotBothThemes("AppNameText") {
            AppNameText()
        }
    }

    @Test
    fun copyRightText() {
        paparazzi.snapshotBothThemes("CopyRightText") {
            CopyRightText()
        }
    }

    @Test
    fun sectionTitle() {
        paparazzi.snapshotBothThemes("SectionTitle") {
            SectionTitle(text = "Section Header")
        }
    }

    @Test
    fun textComponents_allVariants() {
        paparazzi.snapshotBothThemes("Text_allVariants") {
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
