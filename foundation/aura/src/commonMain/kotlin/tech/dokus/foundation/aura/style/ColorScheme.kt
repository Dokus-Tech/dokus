package tech.dokus.foundation.aura.style

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ============================================================
// Dokus v2 Color Tokens — Warm Amber Identity
// ============================================================
// Source of truth: Obsidian → ui_v2/THEME.md
// Light: warm off-white canvas with amber accents
// Dark: warm near-black with lifted amber
// ============================================================

// ─── Light Surfaces ───
private const val BgLightHex = 0xFFF2F1EE              // bg — app canvas
private const val PageLightHex = 0xFFFFFFFF             // page — card surface
private const val CanvasLightHex = 0xFFF0EFEC           // canvas — recessed areas
private const val WarmLightHex = 0xFFF8F7F4             // warm — hover/selected
private const val TextLightHex = 0xFF1A1815             // text — primary
private const val TextSecLightHex = 0xFF5C5650          // textSec — secondary
private const val TextMutedLightHex = 0xFF9C958C        // textMuted — labels
private const val TextFaintLightHex = 0xFFD4D0CA        // textFaint — disabled

// ─── Dark Surfaces ───
private const val BgDarkHex = 0xFF0C0B09                // bg dark
private const val PageDarkHex = 0xFF161412              // page dark
private const val CanvasDarkHex = 0xFF1C1A17            // canvas dark
private const val WarmDarkHex = 0xFF1E1C18              // warm dark
private const val TextDarkHex = 0xFFE8E4DE              // text dark
private const val TextSecDarkHex = 0xFFA69E94           // textSec dark
private const val TextMutedDarkHex = 0xFF7A726A         // textMuted dark
private const val TextFaintDarkHex = 0xFF3D3832         // textFaint dark

// ─── Amber (Primary / Identity) ───
private const val AmberLightHex = 0xFFB8860B            // amber — primary
private const val AmberDarkHex = 0xFFD4A017             // amber dark (lifted)
private const val AmberMedDarkHex = 0xFFE0B028          // amberMed dark
private const val PrimaryContainerLightHex = 0xFFFAF7EE // amberSoft on white
private const val PrimaryContainerDarkHex = 0xFF292213  // amberSoft on dark page

// ─── Green (Tertiary / Success / Positive) ───
private const val GreenLightHex = 0xFF1E8449            // green — confirmed
private const val GreenDarkHex = 0xFF3CC98A             // green dark (brighter)
private const val TertiaryContainerLightHex = 0xFFF2F8F4 // greenSoft on white
private const val TertiaryContainerDarkHex = 0xFF19221C // greenSoft on dark page

// ─── Red (Error / Negative) ───
private const val RedLightHex = 0xFFC0392B              // red — errors
private const val RedDarkHex = 0xFFE8435A               // red dark (brighter)
private const val ErrorContainerLightHex = 0xFFFBF3F2   // redSoft on white
private const val ErrorContainerDarkHex = 0xFF271818    // redSoft on dark page

// ─── Color Instances ───

// Surfaces - Light
private val bgLight = Color(BgLightHex)
private val pageLight = Color(PageLightHex)
private val canvasLight = Color(CanvasLightHex)
private val textLight = Color(TextLightHex)
private val textSecLight = Color(TextSecLightHex)

// Surfaces - Dark
private val bgDark = Color(BgDarkHex)
private val pageDark = Color(PageDarkHex)
private val canvasDark = Color(CanvasDarkHex)
private val textDark = Color(TextDarkHex)
private val textSecDark = Color(TextSecDarkHex)

// Amber
private val amberLight = Color(AmberLightHex)
private val amberDark = Color(AmberDarkHex)
private val amberMedDark = Color(AmberMedDarkHex)
private val primaryContainerLight = Color(PrimaryContainerLightHex)
private val primaryContainerDark = Color(PrimaryContainerDarkHex)

// Green
private val greenLight = Color(GreenLightHex)
private val greenDark = Color(GreenDarkHex)
private val tertiaryContainerLight = Color(TertiaryContainerLightHex)
private val tertiaryContainerDark = Color(TertiaryContainerDarkHex)

// Red
private val redLight = Color(RedLightHex)
private val redDark = Color(RedDarkHex)
private val errorContainerLight = Color(ErrorContainerLightHex)
private val errorContainerDark = Color(ErrorContainerDarkHex)

fun createColorScheme(useDarkTheme: Boolean): ColorScheme = if (useDarkTheme) {
    darkColorScheme(
        primary = amberDark,
        onPrimary = bgDark,
        primaryContainer = primaryContainerDark,
        onPrimaryContainer = amberMedDark,

        secondary = textSecDark,
        onSecondary = bgDark,

        tertiary = greenDark,
        onTertiary = bgDark,
        tertiaryContainer = tertiaryContainerDark,
        onTertiaryContainer = greenDark,

        background = bgDark,
        onBackground = textDark,

        surface = pageDark,
        onSurface = textDark,

        surfaceVariant = canvasDark,
        onSurfaceVariant = textSecDark,

        outline = Color.White.copy(alpha = 0.10f),
        outlineVariant = Color.White.copy(alpha = 0.06f),

        error = redDark,
        onError = bgDark,
        errorContainer = errorContainerDark,
        onErrorContainer = redDark,

        inverseSurface = textDark,
        inverseOnSurface = bgDark,
        inversePrimary = amberLight,
    )
} else {
    lightColorScheme(
        primary = amberLight,
        onPrimary = Color.White,
        primaryContainer = primaryContainerLight,
        onPrimaryContainer = amberLight,

        secondary = textSecLight,
        onSecondary = Color.White,

        tertiary = greenLight,
        onTertiary = Color.White,
        tertiaryContainer = tertiaryContainerLight,
        onTertiaryContainer = greenLight,

        background = bgLight,
        onBackground = textLight,

        surface = pageLight,
        onSurface = textLight,

        surfaceVariant = canvasLight,
        onSurfaceVariant = textSecLight,

        outline = Color.Black.copy(alpha = 0.10f),
        outlineVariant = Color.Black.copy(alpha = 0.06f),

        error = redLight,
        onError = Color.White,
        errorContainer = errorContainerLight,
        onErrorContainer = redLight,

        inverseSurface = textLight,
        inverseOnSurface = bgLight,
        inversePrimary = amberDark,
    )
}

// ─── Extension Properties ───

// Calm ripple — neutral color for subtle feedback
val ColorScheme.rippleColor: Color get() = onSurface

// Theme brightness helper
private const val DarkLuminanceThreshold = 0.5f
val ColorScheme.isDark: Boolean get() = background.luminance() < DarkLuminanceThreshold

// Semantic status colors (v2)
val ColorScheme.statusConfirmed: Color get() = tertiary  // green
val ColorScheme.statusWarning: Color get() = primary     // amber
val ColorScheme.statusError: Color get() = error         // red

// Text hierarchy tokens (v2)
val ColorScheme.textMuted: Color
    get() = if (isDark) Color(TextMutedDarkHex) else Color(TextMutedLightHex)
val ColorScheme.textFaint: Color
    get() = if (isDark) Color(TextFaintDarkHex) else Color(TextFaintLightHex)

// Position colors (financial reality, NOT error/success semantics)
val ColorScheme.positionPositive: Color get() = tertiary  // green
val ColorScheme.positionNegative: Color get() = error     // red

// Interactive surface hover (desktop row hover, v2 "warm" token)
val ColorScheme.surfaceHover: Color
    get() = if (isDark) Color(WarmDarkHex) else Color(WarmLightHex)
