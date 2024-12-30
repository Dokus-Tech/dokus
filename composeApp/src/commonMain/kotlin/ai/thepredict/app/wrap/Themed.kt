package ai.thepredict.app.wrap

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

@Composable
fun Themed(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = dynamicColorScheme(
        seedColor = Color(0xFF0B0909),
        style = PaletteStyle.Content,
        isAmoled = false,
        isDark = useDarkTheme
    )
    MaterialTheme(colorScheme = colorScheme) {
        content()
    }
}