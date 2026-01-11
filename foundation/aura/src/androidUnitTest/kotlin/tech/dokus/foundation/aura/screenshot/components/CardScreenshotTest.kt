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
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.PCard
import tech.dokus.foundation.aura.components.PCardPlusIcon
import tech.dokus.foundation.aura.components.POutlinedCard
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class CardScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun dokusCardSurface_default() {
        snapshotBothThemes("DokusCardSurface_default") {
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
        snapshotBothThemes("DokusCardSurface_soft") {
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
        snapshotBothThemes("DokusCardSurface_clickable") {
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
        snapshotBothThemes("DokusCard_simple") {
            DokusCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simple card content")
            }
        }
    }

    @Test
    fun dokusCard_withHeaderAndFooter() {
        snapshotBothThemes("DokusCard_withHeaderAndFooter") {
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
        snapshotBothThemes("DokusCard_densePadding") {
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
        snapshotBothThemes("DokusGlassSurface") {
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
        snapshotBothThemes("PCard") {
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
        snapshotBothThemes("POutlinedCard") {
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
        snapshotBothThemes("PCardPlusIcon") {
            PCardPlusIcon(modifier = Modifier.size(80.dp))
        }
    }

    @Test
    fun cardVariantsComparison() {
        snapshotBothThemes("Card_variants_comparison") {
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
