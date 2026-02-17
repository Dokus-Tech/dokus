package tech.dokus.foundation.aura.style

import androidx.compose.runtime.Composable

/**
 * Synchronises the platform system-bar icon appearance with the current theme.
 *
 * On Android this sets [isAppearanceLightStatusBars] / [isAppearanceLightNavigationBars].
 * On iOS this overrides the window's user-interface style so the status bar follows the
 * app-level theme even when it differs from the system setting.
 * Desktop and Web are no-ops.
 */
@Composable
internal expect fun SystemBarEffect(isDark: Boolean)
