package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Generic callout banner with a colored background, optional leading icon, and a content slot.
 *
 * Use for simple inline status messages, warnings, or informational callouts.
 * For complex layouts (lists, multiple buttons), use a domain-specific component instead.
 *
 * @param accentColor The color used for the icon tint and background (at 12% alpha).
 * @param icon Optional leading icon.
 * @param content Row content slot — use `Modifier.weight(1f)` on the main text to push trailing content to the end.
 */
@Composable
fun DokusCalloutBanner(
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.statusWarning,
    icon: ImageVector? = null,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = accentColor.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.medium,
            )
            .padding(Constraints.Spacing.large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
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
                color = MaterialTheme.colorScheme.statusWarning,
            )
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
            accentColor = MaterialTheme.colorScheme.error,
            icon = Icons.Default.Warning,
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
