package tech.dokus.foundation.aura.style

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Color hex constants - Brand
private const val DokusGoldHex = 0xFFD4AF37

// Color hex constants - Primary
private const val DokusPrimaryLightHex = 0xFF3B82F6
private const val DokusPrimaryDarkHex = 0xFF60A5FA

// Color hex constants - Surfaces Light
private const val DokusBackgroundLightHex = 0xFFF8F9FB
private const val DokusOnBackgroundLightHex = 0xFF0F172A
private const val DokusSurfaceLightHex = 0xFFFFFFFF
private const val DokusOnSurfaceLightHex = 0xFF0F172A
private const val DokusSurfaceVariantLightHex = 0xFFF1F5F9
private const val DokusOnSurfaceVariantLightHex = 0xFF475569
private const val DokusOutlineLightHex = 0xFFE2E8F0
private const val DokusOutlineVariantLightHex = 0xFFEDF2F7

// Color hex constants - Surfaces Dark
private const val DokusBackgroundDarkHex = 0xFF020617
private const val DokusOnBackgroundDarkHex = 0xFFE5E7EB
private const val DokusSurfaceDarkHex = 0xFF020617
private const val DokusOnSurfaceDarkHex = 0xFFE5E7EB
private const val DokusSurfaceVariantDarkHex = 0xFF0B1220
private const val DokusOnSurfaceVariantDarkHex = 0xFF9CA3AF
private const val DokusOutlineDarkHex = 0xFF1F2937
private const val DokusOutlineVariantDarkHex = 0xFF1E293B

// Color hex constants - Secondary
private const val DokusSecondaryLightHex = 0xFF64748B
private const val DokusSecondaryDarkHex = 0xFF94A3B8

// Color hex constants - Primary Containers
private const val DokusPrimaryContainerLightHex = 0xFFE8F0FF
private const val DokusOnPrimaryContainerLightHex = 0xFF0B1B3F
private const val DokusPrimaryContainerDarkHex = 0xFF1E3A8A
private const val DokusOnPrimaryContainerDarkHex = 0xFFDBEAFE

// Color hex constants - Error Light
private const val DokusErrorLightHex = 0xFFDC2626
private const val DokusOnErrorLightHex = 0xFFFFFFFF
private const val DokusErrorContainerLightHex = 0xFFFEE2E2
private const val DokusOnErrorContainerLightHex = 0xFF7F1D1D

// Color hex constants - Error Dark
private const val DokusErrorDarkHex = 0xFFF87171
private const val DokusOnErrorDarkHex = 0xFF020617
private const val DokusErrorContainerDarkHex = 0xFF7F1D1D
private const val DokusOnErrorContainerDarkHex = 0xFFFEE2E2

// Dokus brand accent (signature only)
internal val dokusGold = Color(DokusGoldHex) // Brand accent only (never primary UI color)

// Canonical Dokus primary (trustworthy blue)
private val dokusPrimaryLight = Color(DokusPrimaryLightHex)
private val dokusPrimaryDark = Color(DokusPrimaryDarkHex)

// Canonical surfaces
private val dokusBackgroundLight = Color(DokusBackgroundLightHex)
private val dokusOnBackgroundLight = Color(DokusOnBackgroundLightHex)
private val dokusSurfaceLight = Color(DokusSurfaceLightHex)
private val dokusOnSurfaceLight = Color(DokusOnSurfaceLightHex)
private val dokusSurfaceVariantLight = Color(DokusSurfaceVariantLightHex)
private val dokusOnSurfaceVariantLight = Color(DokusOnSurfaceVariantLightHex)
private val dokusOutlineLight = Color(DokusOutlineLightHex)
private val dokusOutlineVariantLight = Color(DokusOutlineVariantLightHex)

private val dokusBackgroundDark = Color(DokusBackgroundDarkHex)
private val dokusOnBackgroundDark = Color(DokusOnBackgroundDarkHex)
private val dokusSurfaceDark = Color(DokusSurfaceDarkHex)
private val dokusOnSurfaceDark = Color(DokusOnSurfaceDarkHex)
private val dokusSurfaceVariantDark = Color(DokusSurfaceVariantDarkHex)
private val dokusOnSurfaceVariantDark = Color(DokusOnSurfaceVariantDarkHex)
private val dokusOutlineDark = Color(DokusOutlineDarkHex)
private val dokusOutlineVariantDark = Color(DokusOutlineVariantDarkHex)

// Secondary (neutral slate)
private val dokusSecondaryLight = Color(DokusSecondaryLightHex)
private val dokusSecondaryDark = Color(DokusSecondaryDarkHex)

// Primary containers
private val dokusPrimaryContainerLight = Color(DokusPrimaryContainerLightHex)
private val dokusOnPrimaryContainerLight = Color(DokusOnPrimaryContainerLightHex)
private val dokusPrimaryContainerDark = Color(DokusPrimaryContainerDarkHex)
private val dokusOnPrimaryContainerDark = Color(DokusOnPrimaryContainerDarkHex)

// Error tokens
private val dokusErrorLight = Color(DokusErrorLightHex)
private val dokusOnErrorLight = Color(DokusOnErrorLightHex)
private val dokusErrorContainerLight = Color(DokusErrorContainerLightHex)
private val dokusOnErrorContainerLight = Color(DokusOnErrorContainerLightHex)

private val dokusErrorDark = Color(DokusErrorDarkHex)
private val dokusOnErrorDark = Color(DokusOnErrorDarkHex)
private val dokusErrorContainerDark = Color(DokusErrorContainerDarkHex)
private val dokusOnErrorContainerDark = Color(DokusOnErrorContainerDarkHex)

fun createColorScheme(useDarkTheme: Boolean): ColorScheme = if (useDarkTheme) {
    darkColorScheme(
        primary = dokusPrimaryDark,
        onPrimary = dokusOnErrorDark, // near-black for contrast
        primaryContainer = dokusPrimaryContainerDark,
        onPrimaryContainer = dokusOnPrimaryContainerDark,

        secondary = dokusSecondaryDark,
        onSecondary = dokusOnErrorDark,

        background = dokusBackgroundDark,
        onBackground = dokusOnBackgroundDark,

        surface = dokusSurfaceDark,
        onSurface = dokusOnSurfaceDark,

        surfaceVariant = dokusSurfaceVariantDark,
        onSurfaceVariant = dokusOnSurfaceVariantDark,

        outline = dokusOutlineDark,
        outlineVariant = dokusOutlineVariantDark,

        error = dokusErrorDark,
        onError = dokusOnErrorDark,
        errorContainer = dokusErrorContainerDark,
        onErrorContainer = dokusOnErrorContainerDark,
    )
} else {
    lightColorScheme(
        primary = dokusPrimaryLight,
        onPrimary = dokusOnErrorLight,
        primaryContainer = dokusPrimaryContainerLight,
        onPrimaryContainer = dokusOnPrimaryContainerLight,

        secondary = dokusSecondaryLight,
        onSecondary = dokusOnErrorLight,

        background = dokusBackgroundLight,
        onBackground = dokusOnBackgroundLight,

        surface = dokusSurfaceLight,
        onSurface = dokusOnSurfaceLight,

        surfaceVariant = dokusSurfaceVariantLight,
        onSurfaceVariant = dokusOnSurfaceVariantLight,

        outline = dokusOutlineLight,
        outlineVariant = dokusOutlineVariantLight,

        error = dokusErrorLight,
        onError = dokusOnErrorLight,
        errorContainer = dokusErrorContainerLight,
        onErrorContainer = dokusOnErrorContainerLight,
    )
}

// Calm ripple color - use onSurface for neutral ripple effects instead of primary (gold)
val ColorScheme.rippleColor: Color get() = onSurface

// Helper for surface decisions that depend on theme brightness.
private const val DarkLuminanceThreshold = 0.5f
val ColorScheme.isDark: Boolean get() = background.luminance() < DarkLuminanceThreshold

// Brand accent access (use intentionally; do not map to primary)
@Suppress("UnusedReceiverParameter")
val ColorScheme.brandGold: Color get() = dokusGold

// Semantic status colors (Design System v1)
@Suppress("UnusedReceiverParameter")
val ColorScheme.statusProcessing: Color get() = Color(0xFF64748B)
@Suppress("UnusedReceiverParameter")
val ColorScheme.statusConfirmed: Color get() = Color(0xFF16A34A)
@Suppress("UnusedReceiverParameter")
val ColorScheme.statusWarning: Color get() = Color(0xFFD97706)
@Suppress("UnusedReceiverParameter")
val ColorScheme.statusError: Color get() = Color(0xFFB91C1C)

// Text hierarchy tokens (Design System v1)
val ColorScheme.textMuted: Color
    get() = if (isDark) Color(0xFF6B7280) else Color(0xFF94A3B8)
val ColorScheme.textDisabled: Color
    get() = if (isDark) Color(0xFF4B5563) else Color(0xFFCBD5E1)

// Position colors (financial reality, NOT error/success semantics)
// These are separate from statusError/statusConfirmed intentionally:
// - positionNegative = "you'll spend more than receive" (neutral financial fact)
// - statusError = "something is broken" (requires action)
@Suppress("UnusedReceiverParameter")
val ColorScheme.positionPositive: Color get() = Color(0xFF059669) // emerald-600
@Suppress("UnusedReceiverParameter")
val ColorScheme.positionNegative: Color get() = Color(0xFF991B1B) // red-800 (calmer than error)

// Interactive surface hover (for desktop row hover effects)
val ColorScheme.surfaceHover: Color
    get() = if (isDark) Color(0xFF0B1220) else Color(0xFFF1F5F9)
