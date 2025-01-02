package ai.thepredict.ui

import ai.thepredict.ui.theme.createColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun PreviewWrapper(content: @Composable () -> Unit) {
    val colorScheme = createColorScheme(false)
    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}