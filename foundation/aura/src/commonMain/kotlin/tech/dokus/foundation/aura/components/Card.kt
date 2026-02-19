@file:Suppress("TopLevelPropertyNaming") // Using PascalCase for UI constants (Kotlin convention)

package tech.dokus.foundation.aura.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.isDark

private val BorderWidth = 1.dp
private val GlassElevation = 0.dp
private val HeaderFooterSpacing = 12.dp
private const val SoftSurfaceAlpha = 0.97f
private const val GlassSurfaceAlpha = 0.93f
private const val GlassBorderAlpha = 0.2f
private const val DefaultCardPadding = 16
private const val DenseCardPadding = 12
private val AccentLineInset = 20.dp
private val AccentLineHeight = 1.dp
private val ShadowElevation = 2.dp
private const val AccentAlpha = 0.2f

enum class DokusCardVariant {
    Default,
    Soft,
}

enum class DokusCardPadding(val padding: Int) {
    Default(DefaultCardPadding),
    Dense(DenseCardPadding),
}

@Composable
fun DokusCardSurface(
    modifier: Modifier = Modifier,
    variant: DokusCardVariant = DokusCardVariant.Default,
    shape: Shape = MaterialTheme.shapes.medium,
    accent: Boolean = false,
    shadow: Boolean = false,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.isDark
    val containerColor = when (variant) {
        DokusCardVariant.Default -> if (isDark) colorScheme.surfaceVariant else colorScheme.surface
        DokusCardVariant.Soft -> colorScheme.surface.copy(alpha = SoftSurfaceAlpha)
    }
    val borderStroke = if (accent) {
        BorderStroke(BorderWidth, colorScheme.borderAmber)
    } else {
        BorderStroke(BorderWidth, colorScheme.outlineVariant)
    }
    val shadowElevation = if (shadow) ShadowElevation else 0.dp

    val accentModifier = if (accent) {
        val accentColor = colorScheme.primary
        Modifier.drawBehind {
            val insetPx = AccentLineInset.toPx()
            val lineHeight = AccentLineHeight.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0f),
                        accentColor.copy(alpha = AccentAlpha),
                        accentColor.copy(alpha = 0f),
                    ),
                    startX = insetPx,
                    endX = size.width - insetPx,
                ),
                topLeft = Offset(insetPx, 0f),
                size = androidx.compose.ui.geometry.Size(
                    size.width - insetPx * 2,
                    lineHeight,
                ),
            )
        }
    } else {
        Modifier
    }

    if (onClick != null) {
        Surface(
            modifier = modifier.then(accentModifier),
            shape = shape,
            color = containerColor,
            border = borderStroke,
            tonalElevation = 0.dp,
            shadowElevation = shadowElevation,
            onClick = onClick,
            enabled = enabled,
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier.then(accentModifier),
            shape = shape,
            color = containerColor,
            border = borderStroke,
            tonalElevation = 0.dp,
            shadowElevation = shadowElevation,
        ) {
            content()
        }
    }
}

@Composable
fun DokusCard(
    modifier: Modifier = Modifier,
    variant: DokusCardVariant = DokusCardVariant.Default,
    padding: DokusCardPadding = DokusCardPadding.Default,
    accent: Boolean = false,
    shadow: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val paddingValue = padding.padding.dp
    DokusCardSurface(
        modifier = modifier,
        variant = variant,
        accent = accent,
        shadow = shadow,
        onClick = onClick,
        enabled = enabled,
    ) {
        Column(modifier = Modifier.padding(paddingValue)) {
            if (header != null) {
                header()
                Spacer(modifier = Modifier.height(HeaderFooterSpacing))
            }
            content()
            if (footer != null) {
                Spacer(modifier = Modifier.height(HeaderFooterSpacing))
                footer()
            }
        }
    }
}

@Composable
fun DokusGlassSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = colorScheme.surface.copy(alpha = GlassSurfaceAlpha)
    val borderStroke = BorderStroke(BorderWidth, colorScheme.outlineVariant.copy(alpha = GlassBorderAlpha))
    val shape = MaterialTheme.shapes.medium

    if (onClick != null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = borderStroke,
            tonalElevation = 0.dp,
            shadowElevation = GlassElevation,
            onClick = onClick,
            enabled = enabled,
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = borderStroke,
            tonalElevation = 0.dp,
            shadowElevation = GlassElevation,
        ) {
            content()
        }
    }
}

@Composable
fun PCardPlusIcon(modifier: Modifier) {
    DokusCardSurface(modifier) {
        Text(
            text = "+",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    DokusCardSurface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun POutlinedCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    DokusCardSurface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
