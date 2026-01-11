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
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotAllViewports
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

@RunWith(Parameterized::class)
class TileScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(viewport)

    @Test
    fun companyAvatarImage_withFallback() {
        paparazzi.snapshotAllViewports("CompanyAvatarImage_withFallback", viewport) {
            CompanyAvatarImage(
                avatarUrl = null,
                initial = "A",
                size = AvatarSize.Medium
            )
        }
    }

    @Test
    fun companyAvatarImage_differentInitials() {
        paparazzi.snapshotAllViewports("CompanyAvatarImage_differentInitials", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompanyAvatarImage(avatarUrl = null, initial = "A", size = AvatarSize.Medium)
                CompanyAvatarImage(avatarUrl = null, initial = "G", size = AvatarSize.Medium)
                CompanyAvatarImage(avatarUrl = null, initial = "M", size = AvatarSize.Medium)
                CompanyAvatarImage(avatarUrl = null, initial = "T", size = AvatarSize.Medium)
            }
        }
    }

    @Test
    fun companyAvatarImage_sizes() {
        paparazzi.snapshotAllViewports("CompanyAvatarImage_sizes", viewport) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompanyAvatarImage(avatarUrl = null, initial = "S", size = AvatarSize.Small)
                CompanyAvatarImage(avatarUrl = null, initial = "M", size = AvatarSize.Medium)
                CompanyAvatarImage(avatarUrl = null, initial = "L", size = AvatarSize.Large)
            }
        }
    }
}
