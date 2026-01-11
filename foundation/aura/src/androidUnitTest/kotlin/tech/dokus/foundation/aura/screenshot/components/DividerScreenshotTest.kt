package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.PDashedDivider
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class DividerScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun pDashedDivider() {
        snapshotBothThemes("PDashedDivider") {
            PDashedDivider(modifier = Modifier.fillMaxWidth())
        }
    }

    @Test
    fun pDashedDivider_inContext() {
        snapshotBothThemes("PDashedDivider_inContext") {
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
