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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.style.isDark

enum class DokusCardVariant {
    Default,
    Soft,
}

enum class DokusCardPadding(val padding: Int) {
    Default(16),
    Dense(12),
}

@Composable
fun DokusCardSurface(
    modifier: Modifier = Modifier,
    variant: DokusCardVariant = DokusCardVariant.Default,
    shape: Shape = MaterialTheme.shapes.medium,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.isDark
    val containerColor = when (variant) {
        DokusCardVariant.Default -> if (isDark) colorScheme.surfaceVariant else colorScheme.surface
        DokusCardVariant.Soft -> colorScheme.surface.copy(alpha = 0.97f)
    }
    val borderStroke = BorderStroke(1.dp, colorScheme.outlineVariant)
    val shadowElevation = when (variant) {
        DokusCardVariant.Default -> if (isDark) 0.dp else 1.dp
        DokusCardVariant.Soft -> 1.dp
    }

    if (onClick != null) {
        Surface(
            modifier = modifier,
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
            modifier = modifier,
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
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val paddingValue = padding.padding.dp
    DokusCardSurface(
        modifier = modifier,
        variant = variant,
        onClick = onClick,
        enabled = enabled,
    ) {
        Column(modifier = Modifier.padding(paddingValue)) {
            if (header != null) {
                header()
                Spacer(modifier = Modifier.height(12.dp))
            }
            content()
            if (footer != null) {
                Spacer(modifier = Modifier.height(12.dp))
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
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = colorScheme.surface.copy(alpha = 0.93f)
    val borderStroke = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.2f))
    val shape = MaterialTheme.shapes.large

    if (onClick != null) {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            border = borderStroke,
            tonalElevation = 0.dp,
            shadowElevation = 2.dp,
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
            shadowElevation = 2.dp,
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
            textAlign = TextAlign.Center
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
