package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.screenshot.BaseScreenshotTest
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

/**
 * Screenshot tests for dialog components.
 * Note: DokusDialog uses Dialog() which creates a separate window,
 * so we test the dialog content layout directly using DokusGlassSurface.
 */
class DialogScreenshotTest : BaseScreenshotTest() {

    @get:Rule
    override val paparazzi = createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun dialogContent_simple() {
        snapshotBothThemes("DialogContent_simple") {
            DokusGlassSurface(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Dialog Title",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "This is the dialog content. It provides information to the user.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    @Test
    fun dialogContent_withIcon() {
        snapshotBothThemes("DialogContent_withIcon") {
            DokusGlassSurface(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "Warning",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Are you sure you want to proceed with this action?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    @Test
    fun dialogContent_confirmStyle() {
        snapshotBothThemes("DialogContent_confirmStyle") {
            DokusGlassSurface(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Delete Item",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "This action cannot be undone. Are you sure you want to delete this item?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
