package tech.dokus.foundation.aura.style

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.interVariable
import tech.dokus.aura.resources.switzerVariable
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
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
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
        // Optional: titleLarge only if you want brand in top bars
        // titleLarge = titleLarge.copy(fontFamily = fontFamily),
    )
}

fun Typography.tuned(): Typography = copy(
    // Display (rare in product UI)
    displayLarge = displayLarge.withLineHeightMultiplier(1.15f),
    displayMedium = displayMedium.withLineHeightMultiplier(1.15f),
    displaySmall = displaySmall.withLineHeightMultiplier(1.2f),

    // Headlines (screen titles, empty states)
    headlineLarge = headlineLarge.withLineHeightMultiplier(1.2f),
    headlineMedium = headlineMedium.withLineHeightMultiplier(1.22f),
    headlineSmall = headlineSmall.withLineHeightMultiplier(1.25f),

    // Titles (cards, dialogs, sections)
    titleLarge = titleLarge.withLineHeightMultiplier(1.25f),
    titleMedium = titleMedium.withLineHeightMultiplier(1.25f),
    titleSmall = titleSmall.withLineHeightMultiplier(1.25f),

    // Body (lists, forms, paragraphs)
    bodyLarge = bodyLarge.withLineHeightMultiplier(1.5f),
    bodyMedium = bodyMedium.withLineHeightMultiplier(1.5f),
    bodySmall = bodySmall.withLineHeightMultiplier(1.45f),

    // Labels (buttons, chips, input labels)
    labelLarge = labelLarge.withLineHeightMultiplier(1.25f),
    labelMedium = labelMedium.withLineHeightMultiplier(1.2f),
    labelSmall = labelSmall.withLineHeightMultiplier(1.2f),
)

// Helper: keeps your intent readable
private fun TextStyle.withLineHeightMultiplier(multiplier: Float): TextStyle {
    val fs = fontSize
    // If fontSize is Unspecified, don't touch it.
    if (fs == TextUnit.Unspecified) return this
    return copy(lineHeight = (fs.value * multiplier).sp)
}

@Composable
fun createFontFamilyDisplay(): FontFamily {
    val font = Font(Res.font.switzerVariable)
    return remember(font) { FontFamily(font) }
}

@Composable
fun createFontFamily(): FontFamily {
    val font = Font(Res.font.interVariable)
    return remember(font) { FontFamily(font) }
}