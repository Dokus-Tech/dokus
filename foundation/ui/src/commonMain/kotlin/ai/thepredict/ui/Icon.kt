package ai.thepredict.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun PIcon(
    icon: ImageVector,
    description: String?,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tint = if (isError) {
        MaterialTheme.colorScheme.error
    } else {
        LocalContentColor.current
    }
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = modifier
    )
}