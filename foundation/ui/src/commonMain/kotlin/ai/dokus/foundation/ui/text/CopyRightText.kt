package ai.dokus.foundation.ui.text

import ai.dokus.app.core.constrains.isLargeScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun CopyRightText(modifier: Modifier = Modifier) {
    val color = if (isLargeScreen) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.primary
    }
    Text(
        modifier = modifier,
        text = "©2025 Dokus",
        fontWeight = FontWeight.Medium,
        color = color,
        style = MaterialTheme.typography.titleSmall
    )
}