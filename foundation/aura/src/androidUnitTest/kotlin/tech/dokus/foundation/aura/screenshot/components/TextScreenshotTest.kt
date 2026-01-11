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
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class TextScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun pTitle() {
        snapshotBothThemes("PTitle") {
            PTitle(text = "Page Title")
        }
    }

    @Test
    fun appNameText() {
        snapshotBothThemes("AppNameText") {
            AppNameText()
        }
    }

    @Test
    fun copyRightText() {
        snapshotBothThemes("CopyRightText") {
            CopyRightText()
        }
    }

    @Test
    fun sectionTitle() {
        snapshotBothThemes("SectionTitle") {
            SectionTitle(text = "Section Header")
        }
    }

    @Test
    fun textComponents_allVariants() {
        snapshotBothThemes("Text_allVariants") {
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
