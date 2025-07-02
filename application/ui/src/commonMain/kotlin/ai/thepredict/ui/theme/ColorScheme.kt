package ai.thepredict.ui.theme

import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

// Primary color from Figma design
private val seedColor = Color(0xFF3463E4)

fun createColorScheme(useDarkTheme: Boolean) = dynamicColorScheme(
    seedColor = seedColor,
    style = PaletteStyle.Content,
    isAmoled = false,
    isDark = useDarkTheme
).let {
    it.copy(primary = it.primaryContainer, onPrimary = it.onPrimaryContainer)
}
