package tech.dokus.foundation.aura.style

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
internal expect fun resolvePlatformColorScheme(
    useDarkTheme: Boolean,
    useDynamicColor: Boolean,
    fallback: ColorScheme,
): ColorScheme
