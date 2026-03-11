package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Visual variant for [DokusCalloutBanner].
 */
@Immutable
sealed interface CalloutVariant {

    /** Amber accent card (dark surface + amber border). Default for action prompts and warnings. */
    data object Warning : CalloutVariant

    /** Filled tinted background. Use for validation errors or hard blocks. */
    data class Filled(val color: Color) : CalloutVariant
}

/**
 * Standardized inline callout banner.
 *
 * Layout: **status dot · title + subtitle · trailing action**
 *
 * - [CalloutVariant.Warning] (default): [DokusCardSurface] with amber accent border + warning dot.
 * - [CalloutVariant.Filled]: filled tinted background + error dot.
 *
 * @param title Primary message text (bold, single line).
 * @param subtitle Optional secondary text below the title (muted, single line).
 * @param variant Visual style.
 * @param trailing Optional trailing content (button, label, amount).
 */
@Composable
fun DokusCalloutBanner(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    variant: CalloutVariant = CalloutVariant.Warning,
    trailing: (@Composable () -> Unit)? = null,
) {
    val dotType = when (variant) {
        is CalloutVariant.Warning -> StatusDotType.Warning
        is CalloutVariant.Filled -> StatusDotType.Error
    }

    when (variant) {
        is CalloutVariant.Warning -> {
            DokusCardSurface(accent = true, modifier = modifier) {
                BannerContent(title, subtitle, dotType, trailing)
            }
        }

        is CalloutVariant.Filled -> {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .background(
                        color = variant.color.copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.medium,
                    ),
            ) {
                BannerContent(title, subtitle, dotType, trailing)
            }
        }
    }
}

@Composable
private fun BannerContent(
    title: String,
    subtitle: String?,
    dotType: StatusDotType,
    trailing: (@Composable () -> Unit)?,
) {
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
        StatusDot(
            type = dotType,
            size = 8.dp,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
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
        DokusCalloutBanner(
            title = "15 payments without documents",
            trailing = {
                Text(
                    text = "\u20ac8 420,50 unresolved",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            },
        )
    }
}

@Preview
@Composable
private fun DokusCalloutBannerWithSubtitlePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCalloutBanner(
            title = "APRR \u2014 Entr\u00e9e April 2025",
            subtitle = "Apr 26 \u00b7 APRR",
            trailing = {
                PPrimaryButton(text = "Review", onClick = {})
            },
        )
    }
}

@Preview
@Composable
private fun DokusCalloutBannerWithButtonPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCalloutBanner(
            title = "3 payments require documents",
            trailing = {
                TextButton(onClick = {}) {
                    Text(
                        text = "Review now",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
        )
    }
}

@Preview
@Composable
private fun DokusCalloutBannerErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DokusCalloutBanner(
            title = "A contact with this VAT number already exists",
            subtitle = "Acme Corporation (BE0123456789)",
            variant = CalloutVariant.Filled(color = MaterialTheme.colorScheme.error),
            trailing = {
                TextButton(onClick = {}) {
                    Text("View", color = MaterialTheme.colorScheme.error)
                }
            },
        )
    }
}
