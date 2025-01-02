package ai.thepredict.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

private val seedColor = Color(0xFF0B0909)

fun createColorScheme(useDarkTheme: Boolean) = dynamicColorScheme(
    seedColor = seedColor,
    style = PaletteStyle.Content,
    isAmoled = false,
    isDark = useDarkTheme
)