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

// Line height multipliers for typography tuning
private const val LINE_HEIGHT_DISPLAY = 1.15f
private const val LINE_HEIGHT_TITLE = 1.2f
private const val LINE_HEIGHT_BODY = 1.35f

// Font sizes in sp
private const val FONT_SIZE_DISPLAY_LARGE = 48
private const val FONT_SIZE_HEADLINE_LARGE = 32
private const val FONT_SIZE_HEADLINE_MEDIUM = 24
private const val FONT_SIZE_TITLE_LARGE = 20
private const val FONT_SIZE_TITLE_MEDIUM = 16
private const val FONT_SIZE_BODY_LARGE = 16
private const val FONT_SIZE_BODY_MEDIUM = 14
private const val FONT_SIZE_LABEL_LARGE = 14
private const val FONT_SIZE_LABEL_MEDIUM = 12
private const val FONT_SIZE_LABEL_SMALL = 11

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
    displayLarge = displayLarge.withLineHeightMultiplier(LINE_HEIGHT_DISPLAY),
    displayMedium = displayMedium.withLineHeightMultiplier(LINE_HEIGHT_DISPLAY),
    displaySmall = displaySmall.withLineHeightMultiplier(LINE_HEIGHT_TITLE),

    // Headlines (screen titles, empty states)
    headlineLarge = headlineLarge.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    headlineMedium = headlineMedium.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    headlineSmall = headlineSmall.withLineHeightMultiplier(LINE_HEIGHT_TITLE),

    // Titles (cards, dialogs, sections)
    titleLarge = titleLarge.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    titleMedium = titleMedium.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    titleSmall = titleSmall.withLineHeightMultiplier(LINE_HEIGHT_TITLE),

    // Body (lists, forms, paragraphs)
    bodyLarge = bodyLarge.withLineHeightMultiplier(LINE_HEIGHT_BODY),
    bodyMedium = bodyMedium.withLineHeightMultiplier(LINE_HEIGHT_BODY),
    bodySmall = bodySmall.withLineHeightMultiplier(LINE_HEIGHT_BODY),

    // Labels (buttons, chips, input labels)
    labelLarge = labelLarge.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    labelMedium = labelMedium.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    labelSmall = labelSmall.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
)

fun createDokusTypography(fontFamily: FontFamily): Typography {
    val displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = FONT_SIZE_DISPLAY_LARGE.sp
    )
    val headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = FONT_SIZE_HEADLINE_LARGE.sp
    )
    val headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = FONT_SIZE_HEADLINE_MEDIUM.sp
    )
    val titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FONT_SIZE_TITLE_LARGE.sp
    )
    val titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FONT_SIZE_TITLE_MEDIUM.sp
    )
    val bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FONT_SIZE_BODY_LARGE.sp
    )
    val bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = FONT_SIZE_BODY_MEDIUM.sp
    )
    val labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FONT_SIZE_LABEL_LARGE.sp
    )
    val labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FONT_SIZE_LABEL_MEDIUM.sp
    )
    val labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = FONT_SIZE_LABEL_SMALL.sp
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
