package tech.dokus.foundation.aura.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun PIcon(
    icon: ImageVector,
    description: String?,
    isError: Boolean = false,
    tint: Color = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    },
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = modifier
    )
}