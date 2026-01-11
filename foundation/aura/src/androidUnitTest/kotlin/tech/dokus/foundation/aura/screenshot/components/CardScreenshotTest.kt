package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.PCard
import tech.dokus.foundation.aura.components.PCardPlusIcon
import tech.dokus.foundation.aura.components.POutlinedCard
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class CardScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun dokusCardSurface_default() {
        paparazzi.snapshotAllViewports("DokusCardSurface_default", viewport) {
            DokusCardSurface(
                modifier = Modifier.size(200.dp, 100.dp)
            ) {
                Text(
                    "Card Content",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    @Test
    fun dokusCardSurface_soft() {
        paparazzi.snapshotAllViewports("DokusCardSurface_soft", viewport) {
            DokusCardSurface(
                modifier = Modifier.size(200.dp, 100.dp),
                variant = DokusCardVariant.Soft
            ) {
                Text(
                    "Soft Card",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    @Test
    fun dokusCardSurface_clickable() {
        paparazzi.snapshotAllViewports("DokusCardSurface_clickable", viewport) {
            DokusCardSurface(
                modifier = Modifier.size(200.dp, 100.dp),
                onClick = {}
            ) {
                Text(
                    "Clickable Card",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    @Test
    fun dokusCard_simple() {
        paparazzi.snapshotAllViewports("DokusCard_simple", viewport) {
            DokusCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simple card content")
            }
        }
    }

    @Test
    fun dokusCard_withHeaderAndFooter() {
        paparazzi.snapshotAllViewports("DokusCard_withHeaderAndFooter", viewport) {
            DokusCard(
                modifier = Modifier.fillMaxWidth(),
                header = { Text("Card Header") },
                footer = { Text("Card Footer") }
            ) {
                Text("Card body content goes here")
            }
        }
    }

    @Test
    fun dokusCard_densePadding() {
        paparazzi.snapshotAllViewports("DokusCard_densePadding", viewport) {
            DokusCard(
                modifier = Modifier.fillMaxWidth(),
                padding = DokusCardPadding.Dense
            ) {
                Text("Dense padding card")
            }
        }
    }

    @Test
    fun dokusGlassSurface() {
        paparazzi.snapshotAllViewports("DokusGlassSurface", viewport) {
            DokusGlassSurface(
                modifier = Modifier.size(200.dp, 100.dp)
            ) {
                Text(
                    "Glass Surface",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }

    @Test
    fun pCard() {
        paparazzi.snapshotAllViewports("PCard", viewport) {
            PCard(modifier = Modifier.size(200.dp, 100.dp)) {
                Text(
                    "PCard Content",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    @Test
    fun pOutlinedCard() {
        paparazzi.snapshotAllViewports("POutlinedCard", viewport) {
            POutlinedCard(modifier = Modifier.size(200.dp, 100.dp)) {
                Text(
                    "Outlined Card",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    @Test
    fun pCardPlusIcon() {
        paparazzi.snapshotAllViewports("PCardPlusIcon", viewport) {
            PCardPlusIcon(modifier = Modifier.size(80.dp))
        }
    }

    @Test
    fun cardVariantsComparison() {
        paparazzi.snapshotAllViewports("Card_variants_comparison", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DokusCard(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    variant = DokusCardVariant.Default
                ) {
                    Text("Default Variant")
                }
                DokusCard(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    variant = DokusCardVariant.Soft
                ) {
                    Text("Soft Variant")
                }
            }
        }
    }
}
