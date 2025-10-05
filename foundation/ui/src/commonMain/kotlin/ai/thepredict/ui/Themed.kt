package ai.thepredict.ui

import ai.thepredict.app.platform.activePlatform
import ai.thepredict.app.platform.isWeb
import ai.thepredict.ui.theme.createColorScheme
import ai.thepredict.ui.theme.createFontFamily
import ai.thepredict.ui.theme.createFontFamilyDisplay
import ai.thepredict.ui.theme.withFontFamily
import ai.thepredict.ui.theme.withFontFamilyForDisplay
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