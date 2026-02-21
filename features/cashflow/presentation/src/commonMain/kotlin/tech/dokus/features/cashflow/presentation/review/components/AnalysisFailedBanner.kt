package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_analysis_continue_manually
import tech.dokus.aura.resources.cashflow_analysis_failed_title
import tech.dokus.aura.resources.cashflow_analysis_retry
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.PPrimaryButton
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Calm failure banner shown when AI extraction fails.
 * Uses neutral styling (secondaryContainer) instead of error red.
 *
 * @param reason Optional error message explaining why extraction failed
 * @param isRetrying True while retry is in progress
 * @param onRetry Called when user clicks "Retry Analysis"
 * @param onContinueManually Called when user clicks "Continue Manually" to dismiss and edit fields
 * @param modifier Modifier for the banner
 */
@Composable
internal fun AnalysisFailedBanner(
    reason: String?,
    isRetrying: Boolean,
    onRetry: () -> Unit,
    onContinueManually: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier,
        variant = DokusCardVariant.Soft,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(Constraints.IconSize.medium),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(Res.string.cashflow_analysis_failed_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (!reason.isNullOrBlank()) {
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                PPrimaryButton(
                    text = stringResource(Res.string.cashflow_analysis_retry),
                    onClick = onRetry,
                    enabled = !isRetrying,
                    isLoading = isRetrying,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onContinueManually,
                    enabled = !isRetrying,
                ) {
                    Text(
                        text = stringResource(Res.string.cashflow_analysis_continue_manually),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun AnalysisFailedBannerPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        AnalysisFailedBanner(
            reason = "Could not extract document data",
            isRetrying = false,
            onRetry = {},
            onContinueManually = {}
        )
    }
}
