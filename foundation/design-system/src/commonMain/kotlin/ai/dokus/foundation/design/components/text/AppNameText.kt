package ai.dokus.foundation.design.components.text

import ai.dokus.foundation.design.constrains.isLargeScreen
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.app_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppNameText(modifier: Modifier = Modifier) {
    val color = if (isLargeScreen) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.primary
    }
    Image(
        painter = painterResource(Res.drawable.app_logo),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        colorFilter = ColorFilter.tint(color),
        modifier = modifier
    )
}