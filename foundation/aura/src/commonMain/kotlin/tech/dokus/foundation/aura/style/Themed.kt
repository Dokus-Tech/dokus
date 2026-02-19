package tech.dokus.foundation.aura.style

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.local.LocalThemeManager
import tech.dokus.foundation.platform.activePlatform
import tech.dokus.foundation.platform.isWeb

// Dokus Shape System
private val dokusShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),  // badges, tags
    small = RoundedCornerShape(7.dp),       // buttons, inputs
    medium = RoundedCornerShape(10.dp),     // cards, panels
    large = RoundedCornerShape(16.dp),      // floating windows
    extraLarge = RoundedCornerShape(16.dp), // floating windows
)

/**
 * Themed wrapper that applies Material3 theme to content.
 *
 * Theme mode is determined by [LocalThemeManager]:
 * - [ThemeMode.LIGHT]: Always light theme
 * - [ThemeMode.DARK]: Always dark theme
 * - [ThemeMode.SYSTEM]: Follow system setting
 *
 * Note: [LocalThemeManager] must be provided by [tech.dokus.foundation.aura.local.ThemeManagerProvided]
 * higher in the compose tree.
 */
@Composable
fun Themed(
    content: @Composable () -> Unit,
) {
    val themeManager = LocalThemeManager.current
    val themeMode by themeManager.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemDark
    }

    SystemBarEffect(useDarkTheme)

    val colorScheme = createColorScheme(useDarkTheme)

    val fontFamily = createFontFamily()
    val typography = if (activePlatform.isWeb) {
        // Web rendering for custom fonts is unstable; use platform monospace to preserve character.
        createDokusTypography(FontFamily.Monospace)
    } else {
        createDokusTypography(fontFamily)
    }
    // Calm ripple configuration: neutral color with low alpha (≤ 0.12) for subtle feedback
    val calmRippleAlpha = RippleAlpha(
        pressedAlpha = 0.10f,
        focusedAlpha = 0.10f,
        draggedAlpha = 0.08f,
        hoveredAlpha = 0.06f
    )
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            color = colorScheme.rippleColor,
            rippleAlpha = calmRippleAlpha
        )
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = dokusShapes) {
            content()
        }
    }
}
