package tech.dokus.foundation.aura.style

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.interVariable
import tech.dokus.aura.resources.switzerVariable

@Composable
fun Typography.withFontFamily(fontFamily: FontFamily): Typography {
    return copy(
        displayLarge = displayLarge.copy(fontFamily = fontFamily),
        displayMedium = displayMedium.copy(fontFamily = fontFamily),
        displaySmall = displaySmall.copy(fontFamily = fontFamily),
        headlineLarge = headlineLarge.copy(fontFamily = fontFamily),
        headlineMedium = headlineMedium.copy(fontFamily = fontFamily),
        headlineSmall = headlineSmall.copy(fontFamily = fontFamily),
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
         titleLarge = titleLarge.copy(fontFamily = fontFamily),
    )
}

fun Typography.tuned(): Typography = copy(
    // Dokus Design System line-height rules:
    // - Titles: 1.2
    // - Body: 1.35
    // - Labels: 1.2
    // Display stays slightly tighter for large hero text.

    // Display (rare in product UI)
    displayLarge = displayLarge.withLineHeightMultiplier(1.15f),
    displayMedium = displayMedium.withLineHeightMultiplier(1.15f),
    displaySmall = displaySmall.withLineHeightMultiplier(1.2f),

    // Headlines (screen titles, empty states)
    headlineLarge = headlineLarge.withLineHeightMultiplier(1.2f),
    headlineMedium = headlineMedium.withLineHeightMultiplier(1.2f),
    headlineSmall = headlineSmall.withLineHeightMultiplier(1.2f),

    // Titles (cards, dialogs, sections)
    titleLarge = titleLarge.withLineHeightMultiplier(1.2f),
    titleMedium = titleMedium.withLineHeightMultiplier(1.2f),
    titleSmall = titleSmall.withLineHeightMultiplier(1.2f),

    // Body (lists, forms, paragraphs)
    bodyLarge = bodyLarge.withLineHeightMultiplier(1.35f),
    bodyMedium = bodyMedium.withLineHeightMultiplier(1.35f),
    bodySmall = bodySmall.withLineHeightMultiplier(1.35f),

    // Labels (buttons, chips, input labels)
    labelLarge = labelLarge.withLineHeightMultiplier(1.2f),
    labelMedium = labelMedium.withLineHeightMultiplier(1.2f),
    labelSmall = labelSmall.withLineHeightMultiplier(1.2f),
)

fun createDokusTypography(fontFamily: FontFamily): Typography {
    val displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 48.sp
    )
    val headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp
    )
    val headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    )
    val titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    )
    val titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    )
    val bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    )
    val bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
    val labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
    val labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
    val labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )

    return Typography(
        displayLarge = displayLarge,
        displayMedium = headlineLarge,
        displaySmall = headlineMedium,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = titleLarge,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = labelLarge,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodyMedium,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    ).tuned()
}

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
