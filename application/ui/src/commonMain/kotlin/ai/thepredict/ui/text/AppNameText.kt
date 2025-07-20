package ai.thepredict.ui.text

import ai.thepredict.app.core.constrains.isLargeScreen
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AppNameText(modifier: Modifier = Modifier) {
    val color = if (isLargeScreen) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.primary
    }
    Text(
        modifier = modifier,
        text = "Predict",
        color = color,
        style = MaterialTheme.typography.displaySmall
    )
}