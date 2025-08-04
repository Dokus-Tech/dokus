package ai.thepredict.ui.text

import ai.thepredict.app.core.constrains.isLargeScreen
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import thepredict.application.ui.generated.resources.Res
import thepredict.application.ui.generated.resources.app_logo

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
//    Text(
//        modifier = modifier,
//        text = "Predict",
//        color = color,
//        style = MaterialTheme.typography.displaySmall
//    )
}