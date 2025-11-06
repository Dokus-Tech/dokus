package ai.dokus.foundation.design.style

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

// Dokus brand colors - elegant dark theme with gold accents
private val dokusGold = Color(0xFFD4AF37) // Elegant gold - primary brand color
private val dokusBlack = Color(0xFF0A0A0F) // Deep black with slight blue tint

fun createColorScheme(useDarkTheme: Boolean) = dynamicColorScheme(
    seedColor = dokusGold,
    style = PaletteStyle.Fidelity,
    isAmoled = false,
    isDark = useDarkTheme,
    modifyColorScheme = {
        it.copy(
            background = if (useDarkTheme) dokusBlack else it.background,
            surface = if (useDarkTheme) dokusBlack else it.surface
        )
    }
)

val ColorScheme.rippleColor: Color get() = primary