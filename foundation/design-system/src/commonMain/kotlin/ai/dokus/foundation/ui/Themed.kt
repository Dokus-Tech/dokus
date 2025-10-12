package ai.dokus.foundation.ui

import ai.dokus.foundation.platform.activePlatform
import ai.dokus.foundation.platform.isWeb
import ai.dokus.foundation.ui.theme.createColorScheme
import ai.dokus.foundation.ui.theme.createFontFamily
import ai.dokus.foundation.ui.theme.createFontFamilyDisplay
import ai.dokus.foundation.ui.theme.withFontFamily
import ai.dokus.foundation.ui.theme.withFontFamilyForDisplay
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun Themed(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = createColorScheme(useDarkTheme)

    val fontFamilyDisplay = createFontFamilyDisplay()
    val fontFamily = createFontFamily()
    val typography = MaterialTheme.typography.run {
        // For some reason rendering of custom fonts fail on web. They do load, but render incorrectly
        // For now we'll keep typography default only for web, and use custom for all other platforms
        if (activePlatform.isWeb) this
        else withFontFamily(fontFamily).withFontFamilyForDisplay(fontFamilyDisplay)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        content()
    }
}