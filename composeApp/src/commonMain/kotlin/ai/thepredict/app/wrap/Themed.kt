package ai.thepredict.app.wrap

import ai.thepredict.ui.theme.createColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun Themed(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = createColorScheme(useDarkTheme).copy(background = Color(0xFFFFFFFF))
    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}