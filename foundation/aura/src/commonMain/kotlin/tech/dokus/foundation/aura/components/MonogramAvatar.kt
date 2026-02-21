package tech.dokus.foundation.aura.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val LargeSizeThreshold = 48.dp
private val SmallBorderWidth = 1.dp
private val LargeBorderWidth = 1.5.dp
private const val GradientEndAlpha = 0.12f

/**
 * Initials-based avatar with amber gradient.
 *
 * Used for contacts, team members, user profile.
 * Distinct from [CompanyAvatarImage] which handles image URLs.
 *
 * @param initials First letters of name (e.g. "AK", "TB")
 * @param size Avatar size in dp
 * @param radius Corner radius in dp
 * @param fontSize Font size (auto-calculated as size * 0.3 if Unspecified)
 * @param selected Selected state (solid amberSoft background instead of gradient)
 */
@Composable
fun MonogramAvatar(
    initials: String,
    size: Dp,
    radius: Dp,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
    selected: Boolean = false,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(radius)
    val borderWidth = if (size > LargeSizeThreshold) LargeBorderWidth else SmallBorderWidth
    val amberSoft = MaterialTheme.colorScheme.amberSoft
    val primary = MaterialTheme.colorScheme.primary
    val borderAmber = MaterialTheme.colorScheme.borderAmber

    val background = if (selected) {
        Brush.linearGradient(listOf(amberSoft, amberSoft))
    } else {
        Brush.linearGradient(
            colors = listOf(amberSoft, primary.copy(alpha = GradientEndAlpha)),
            start = Offset.Zero,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }

    val resolvedFontSize = if (fontSize == TextUnit.Unspecified) {
        (size.value * 0.3f).sp
    } else {
        fontSize
    }

    val semanticsModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Box(
        modifier = semanticsModifier
            .size(size)
            .clip(shape)
            .background(background, shape)
            .border(borderWidth, borderAmber, shape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.take(2).uppercase(),
            color = primary,
            fontSize = resolvedFontSize,
            fontWeight = FontWeight.Bold,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
        )
    }
}

@Preview
@Composable
private fun MonogramAvatarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        MonogramAvatar(initials = "JD", size = 48.dp, radius = 8.dp)
    }
}
