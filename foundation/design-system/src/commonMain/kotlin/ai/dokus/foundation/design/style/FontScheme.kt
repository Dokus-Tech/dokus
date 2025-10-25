package ai.dokus.foundation.design.style

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.direct
import ai.dokus.app.resources.generated.gilroy_extra_bold
import org.jetbrains.compose.resources.Font

@Composable
fun Typography.withFontFamily(fontFamily: FontFamily): Typography {
    return copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        titleLarge = titleLarge.copy(fontFamily = fontFamily),
        titleMedium = titleMedium.copy(fontFamily = fontFamily),
        titleSmall = titleSmall.copy(fontFamily = fontFamily),
        bodyLarge = bodyLarge.copy(fontFamily = fontFamily),
        bodyMedium = bodyMedium.copy(fontFamily = fontFamily),
        bodySmall = bodySmall.copy(fontFamily = fontFamily),
        labelLarge = labelLarge.copy(fontFamily = fontFamily),
        labelMedium = labelMedium.copy(fontFamily = fontFamily),
        labelSmall = labelSmall.copy(fontFamily = fontFamily),
    )
}

@Composable
fun Typography.withFontFamilyForDisplay(fontFamily: FontFamily): Typography {
    return copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
    )
}

@Composable
fun createFontFamilyDisplay(): FontFamily {
    val font = Font(Res.font.gilroy_extra_bold)
    return remember { FontFamily(font) }
}

@Composable
fun createFontFamily(): FontFamily {
    val font = Font(Res.font.direct)
    return remember { FontFamily(font) }
}