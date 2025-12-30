package tech.dokus.foundation.aura.style

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

// Dokus brand colors - elegant dark theme with gold accents
private val dokusGold = Color(0xFFD4AF37) // Elegant gold - primary brand color
private val dokusBlack = Color(0xFF0A0A0F) // Deep black with slight blue tint

// Neutral gray palette for calm finance aesthetic - dark mode
private val neutralTertiaryDark = Color(0xFF9A9A9F) // Medium gray for tertiary elements
private val neutralTertiaryContainerDark = Color(0xFF2A2A2F) // Elevated surface gray
private val neutralErrorContainerDark = Color(0xFF322828) // Muted dark gray (subtle warmth for error context)
private val neutralSurfaceVariantDark = Color(0xFF1A1A1F) // Slight variation from surface

// Neutral gray palette for calm finance aesthetic - light mode
private val neutralTertiaryLight = Color(0xFF6A6A6F) // Medium gray for tertiary elements
private val neutralTertiaryContainerLight = Color(0xFFE0E0E5) // Light elevated surface
private val neutralErrorContainerLight = Color(0xFFE8D8D8) // Muted light gray (subtle warmth for error context)
private val neutralSurfaceVariantLight = Color(0xFFF0F0F5) // Slight off-white variation

fun createColorScheme(useDarkTheme: Boolean) = dynamicColorScheme(
    seedColor = dokusGold,
    style = PaletteStyle.Neutral,
    isAmoled = false,
    isDark = useDarkTheme,
    modifyColorScheme = {
        it.copy(
            background = if (useDarkTheme) dokusBlack else it.background,
            surface = if (useDarkTheme) dokusBlack else it.surface,
            // Override tertiary colors to neutral grays for calm finance aesthetic
            tertiary = if (useDarkTheme) neutralTertiaryDark else neutralTertiaryLight,
            tertiaryContainer = if (useDarkTheme) neutralTertiaryContainerDark else neutralTertiaryContainerLight,
            // Override error container to muted neutral (preserves error semantics with less visual noise)
            errorContainer = if (useDarkTheme) neutralErrorContainerDark else neutralErrorContainerLight,
            // Override surface variant to neutral gray
            surfaceVariant = if (useDarkTheme) neutralSurfaceVariantDark else neutralSurfaceVariantLight
        )
    }
)

// Calm ripple color - use onSurface for neutral ripple effects instead of primary (gold)
val ColorScheme.rippleColor: Color get() = onSurface