package tech.dokus.foundation.aura.local

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * CompositionLocal for accessing the reduce motion preference.
 * When true, animations should be minimized or disabled for accessibility.
 *
 * This respects user preferences for reduced motion (WCAG 2.1 guideline).
 * Default is false (animations enabled).
 */
val LocalReduceMotion = staticCompositionLocalOf { false }
