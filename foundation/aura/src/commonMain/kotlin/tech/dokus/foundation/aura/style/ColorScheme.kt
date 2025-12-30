package tech.dokus.foundation.aura.style

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Dokus brand accent (signature only)
internal val dokusGold = Color(0xFFD4AF37) // Brand accent only (never primary UI color)

// Canonical Dokus primary (trustworthy blue)
private val dokusPrimaryLight = Color(0xFF3B82F6)
private val dokusPrimaryDark = Color(0xFF60A5FA)

// Canonical surfaces
private val dokusBackgroundLight = Color(0xFFF8FAFC)
private val dokusOnBackgroundLight = Color(0xFF0F172A)
private val dokusSurfaceLight = Color(0xFFFFFFFF)
private val dokusOnSurfaceLight = Color(0xFF0F172A)
private val dokusSurfaceVariantLight = Color(0xFFF1F5F9)
private val dokusOnSurfaceVariantLight = Color(0xFF334155)
private val dokusOutlineLight = Color(0xFFCBD5E1)
private val dokusOutlineVariantLight = Color(0xFFE2E8F0)

private val dokusBackgroundDark = Color(0xFF020617)
private val dokusOnBackgroundDark = Color(0xFFE5E7EB)
private val dokusSurfaceDark = Color(0xFF020617)
private val dokusOnSurfaceDark = Color(0xFFE5E7EB)
private val dokusSurfaceVariantDark = Color(0xFF0F172A)
private val dokusOnSurfaceVariantDark = Color(0xFFCBD5E1)
private val dokusOutlineDark = Color(0xFF334155)
private val dokusOutlineVariantDark = Color(0xFF1E293B)

// Secondary (neutral slate)
private val dokusSecondaryLight = Color(0xFF64748B)
private val dokusSecondaryDark = Color(0xFF94A3B8)

// Primary containers
private val dokusPrimaryContainerLight = Color(0xFFE8F0FF)
private val dokusOnPrimaryContainerLight = Color(0xFF0B1B3F)
private val dokusPrimaryContainerDark = Color(0xFF1E3A8A)
private val dokusOnPrimaryContainerDark = Color(0xFFDBEAFE)

// Error tokens
private val dokusErrorLight = Color(0xFFDC2626)
private val dokusOnErrorLight = Color(0xFFFFFFFF)
private val dokusErrorContainerLight = Color(0xFFFEE2E2)
private val dokusOnErrorContainerLight = Color(0xFF7F1D1D)

private val dokusErrorDark = Color(0xFFF87171)
private val dokusOnErrorDark = Color(0xFF020617)
private val dokusErrorContainerDark = Color(0xFF7F1D1D)
private val dokusOnErrorContainerDark = Color(0xFFFEE2E2)

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
val ColorScheme.isDark: Boolean get() = background.luminance() < 0.5f

// Brand accent access (use intentionally; do not map to primary)
@Suppress("UnusedReceiverParameter")
val ColorScheme.brandGold: Color get() = dokusGold
