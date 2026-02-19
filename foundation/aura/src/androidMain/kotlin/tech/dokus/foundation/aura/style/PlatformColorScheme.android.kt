package tech.dokus.foundation.aura.style

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
internal actual fun resolvePlatformColorScheme(
    useDarkTheme: Boolean,
    useDynamicColor: Boolean,
    fallback: ColorScheme,
): ColorScheme {
    if (!useDynamicColor) return fallback
    val context = LocalContext.current
    return if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
