package ai.dokus.foundation.design.style

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

// Primary color from Figma design
private val seedColor = Color(0xFF3463E4)
private val lightBlue = Color(0xFFF0F4FF)

fun createColorScheme(useDarkTheme: Boolean) = dynamicColorScheme(
    seedColor = seedColor,
    style = PaletteStyle.Content,
    isAmoled = false,
    isDark = useDarkTheme,
    modifyColorScheme = {
        it.copy(
            primary = it.primaryContainer,
            onPrimary = it.onPrimaryContainer,
            surfaceVariant = lightBlue
        )
    }
)

val ColorScheme.rippleColor: Color get() = primary