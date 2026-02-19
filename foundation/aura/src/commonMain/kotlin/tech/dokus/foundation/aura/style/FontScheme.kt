package tech.dokus.foundation.aura.style

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.jetbrains_mono_bold
import tech.dokus.aura.resources.jetbrains_mono_medium
import tech.dokus.aura.resources.jetbrains_mono_regular
import tech.dokus.aura.resources.jetbrains_mono_semibold

// ┌────────────────┬──────────────────────────────┬────────┬────────┬──────────┐
// │    M3 Slot     │         Actual role           │  Size  │ Weight │ Tracking │
// ├────────────────┼──────────────────────────────┼────────┼────────┼──────────┤
// │ displayLarge   │ Hero amounts (28sp stats)     │ 28sp   │ 700    │ -0.04em  │
// │ displayMedium  │ Mobile page title <h1>        │ 22sp   │ 700    │ -0.03em  │
// │ displaySmall   │ Hero names (profile/contact)  │ 18sp   │ 700    │ -0.02em  │
// │ headlineLarge  │ Content window title          │ 15sp   │ 700    │ -0.02em  │
// │ headlineMedium │ Section title (SectionTitle)  │ 14sp   │ 700    │  0       │
// │ headlineSmall  │ Body emphasis / card title    │ 13sp   │ 600    │  0       │
// │ titleLarge     │ Body primary                  │ 13sp   │ 500    │  0       │
// │ titleMedium    │ Sidebar nav / table cell      │ 12.5sp │ 500    │  0       │
// │ titleSmall     │ Button / action label         │ 12sp   │ 600    │  0       │
// │ bodyLarge      │ Body secondary                │ 12sp   │ 400    │  0       │
// │ bodyMedium     │ Small text / dates            │ 11sp   │ 400    │  0       │
// │ bodySmall      │ Metadata / captions           │ 10sp   │ 400    │  0       │
// │ labelLarge     │ Table headers (uppercase)     │ 11sp   │ 600    │  0.02em  │
// │ labelMedium    │ Section label (UPPERCASE)     │ 10sp   │ 600    │  0.1em   │
// │ labelSmall     │ Badge / micro                 │  9sp   │ 600    │  0       │
// └────────────────┴──────────────────────────────┴────────┴────────┴──────────┘
//
// Edge cases use .copy(): 8sp badges, 10.5sp identifiers, 17sp brand, 32sp cashflow hero.

// Line height multipliers — JetBrains Mono has taller metrics than Inter
private const val LINE_HEIGHT_DISPLAY = 1.15f
private const val LINE_HEIGHT_TITLE = 1.3f
private const val LINE_HEIGHT_BODY = 1.4f

@Composable
fun createFontFamily(): FontFamily {
    val regular = Font(Res.font.jetbrains_mono_regular, FontWeight.Normal)
    val medium = Font(Res.font.jetbrains_mono_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.jetbrains_mono_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.jetbrains_mono_bold, FontWeight.Bold)
    return remember(regular, medium, semiBold, bold) {
        FontFamily(regular, medium, semiBold, bold)
    }
}

fun createDokusTypography(fontFamily: FontFamily): Typography {
    // Display: hero amounts and mobile page titles
    val displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.04).em,
    )
    val displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.03).em,
    )
    val displaySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = (-0.02).em,
    )

    // Headlines: window titles and section headers
    val headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = (-0.02).em,
    )
    val headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
    )
    val headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
    )

    // Titles: body primary, nav, buttons
    val titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    )
    val titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
    )
    val titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
    )

    // Body: secondary content, small text, metadata
    val bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
    )
    val bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
    )
    val bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
    )

    // Labels: table headers, section labels, badges
    val labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.02.em,
    )
    val labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        letterSpacing = 0.1.em,
    )
    val labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 9.sp,
    )

    return Typography(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall,
    ).tuned()
}

private fun Typography.tuned(): Typography = copy(
    displayLarge = displayLarge.withLineHeightMultiplier(LINE_HEIGHT_DISPLAY),
    displayMedium = displayMedium.withLineHeightMultiplier(LINE_HEIGHT_DISPLAY),
    displaySmall = displaySmall.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    headlineLarge = headlineLarge.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    headlineMedium = headlineMedium.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    headlineSmall = headlineSmall.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    titleLarge = titleLarge.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    titleMedium = titleMedium.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    titleSmall = titleSmall.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    bodyLarge = bodyLarge.withLineHeightMultiplier(LINE_HEIGHT_BODY),
    bodyMedium = bodyMedium.withLineHeightMultiplier(LINE_HEIGHT_BODY),
    bodySmall = bodySmall.withLineHeightMultiplier(LINE_HEIGHT_BODY),
    labelLarge = labelLarge.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    labelMedium = labelMedium.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
    labelSmall = labelSmall.withLineHeightMultiplier(LINE_HEIGHT_TITLE),
)

private fun TextStyle.withLineHeightMultiplier(multiplier: Float): TextStyle {
    val fs = fontSize
    if (fs == TextUnit.Unspecified) return this
    return copy(lineHeight = (fs.value * multiplier).sp)
}
