package ai.dokus.foundation.design.style

import ai.dokus.foundation.design.local.LocalThemeManager
import ai.dokus.foundation.platform.activePlatform
import ai.dokus.foundation.platform.isWeb
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Themed wrapper that applies Material3 theme to content.
 *
 * Theme mode is determined by [LocalThemeManager]:
 * - [ThemeMode.LIGHT]: Always light theme
 * - [ThemeMode.DARK]: Always dark theme
 * - [ThemeMode.SYSTEM]: Follow system setting
 *
 * Note: [LocalThemeManager] must be provided by [ai.dokus.foundation.design.local.ThemeManagerProvided]
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
    val colorScheme = createColorScheme(useDarkTheme)

    val fontFamilyDisplay = createFontFamilyDisplay()
    val fontFamily = createFontFamily()
    val typography = MaterialTheme.typography.run {
        // For some reason, rendering of custom fonts fails on the web. They do load but render incorrectly
        // For now we'll keep typography default only for web and use custom for all other platforms
        if (activePlatform.isWeb) this
        else withFontFamily(fontFamily).withFontFamilyForDisplay(fontFamilyDisplay)
    }
    // Calm ripple configuration: neutral color with low alpha (≤ 0.12) for subtle feedback
    val calmRippleAlpha = RippleAlpha(
        pressedAlpha = 0.10f,
        focusedAlpha = 0.10f,
        draggedAlpha = 0.08f,
        hoveredAlpha = 0.06f
    )
    CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = colorScheme.rippleColor, rippleAlpha = calmRippleAlpha)) {
        MaterialTheme(colorScheme = colorScheme, typography = typography) {
            content()
        }
    }
}