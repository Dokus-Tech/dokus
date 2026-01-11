package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class TileScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun companyAvatarImage_withFallback() {
        snapshotBothThemes("CompanyAvatarImage_withFallback") {
            CompanyAvatarImage(
                avatarUrl = null,
                initial = "A",
                size = AvatarSize.Medium
            )
        }
    }

    @Test
    fun companyAvatarImage_differentInitials() {
        snapshotBothThemes("CompanyAvatarImage_differentInitials") {
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
        snapshotBothThemes("CompanyAvatarImage_sizes") {
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
