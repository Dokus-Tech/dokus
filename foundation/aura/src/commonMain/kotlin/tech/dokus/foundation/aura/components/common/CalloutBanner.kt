package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Visual variant for [DokusCalloutBanner].
 */
@Immutable
sealed interface CalloutVariant {

    /**
     * Amber accent card (dark surface + amber border). Default for action prompts and warnings.
     */
    data object Warning : CalloutVariant

    /**
     * Filled tinted background with optional leading icon. Use for validation errors or hard blocks.
     */
    data class Filled(
        val color: Color,
        val icon: ImageVector? = null,
    ) : CalloutVariant
}

/**
 * Inline callout banner for warnings, action prompts, and attention-needed items.
 *
 * - [CalloutVariant.Warning] (default): renders a [DokusCardSurface] with amber accent border.
 * - [CalloutVariant.Filled]: renders a filled tinted background with optional leading icon.
 *
 * @param variant Visual style.
 * @param content Row content slot — use `Modifier.weight(1f)` on the main text to push trailing content to the end.
 */
@Composable
fun DokusCalloutBanner(
    modifier: Modifier = Modifier,
    variant: CalloutVariant = CalloutVariant.Warning,
    content: @Composable RowScope.() -> Unit,
) {
    when (variant) {
        is CalloutVariant.Warning -> WarningBanner(modifier, content)
        is CalloutVariant.Filled -> FilledBanner(variant.color, variant.icon, modifier, content)
    }
}

@Composable
private fun WarningBanner(
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    DokusCardSurface(accent = true, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.xLarge,
                    vertical = Constraints.Spacing.medium,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            content()
        }
    }
}

@Composable
private fun FilledBanner(
    color: Color,
    icon: ImageVector?,
    modifier: Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(Constraints.Spacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(Constraints.IconSize.medium),
            )
            Spacer(modifier = Modifier.width(Constraints.Spacing.medium))
        }
        content()
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun DokusCalloutBannerWarningPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCalloutBanner {
            Text(
                text = "15 payments without documents",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "\u20ac8 420,50 unresolved",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun DokusCalloutBannerWithButtonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCalloutBanner {
            Text(
                text = "3 payments require documents",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {}) {
                Text(
                    text = "Review now",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Preview
@Composable
private fun DokusCalloutBannerErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCalloutBanner(
            variant = CalloutVariant.Filled(
                color = MaterialTheme.colorScheme.error,
                icon = Icons.Default.Warning,
            ),
        ) {
            Text(
                text = "A contact with this VAT number already exists",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
