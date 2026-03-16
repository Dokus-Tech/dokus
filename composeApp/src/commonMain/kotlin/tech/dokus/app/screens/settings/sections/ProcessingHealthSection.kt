package tech.dokus.app.screens.settings.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.viewmodel.WorkspaceSettingsState.BulkReprocessState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.processing_health_title
import tech.dokus.aura.resources.processing_health_subtitle
import tech.dokus.aura.resources.processing_health_recent_processed
import tech.dokus.aura.resources.processing_health_needs_review
import tech.dokus.aura.resources.processing_health_failed
import tech.dokus.aura.resources.processing_health_eligible
import tech.dokus.aura.resources.processing_health_recommendation
import tech.dokus.aura.resources.processing_health_recommendation_retry
import tech.dokus.aura.resources.processing_health_reprocess_button
import tech.dokus.aura.resources.processing_health_reprocess_done
import tech.dokus.domain.model.ProcessingHealthRecommendation
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.settings.DataRow
import tech.dokus.foundation.aura.components.settings.SettingsSection
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun ProcessingHealthSection(
    processingHealth: DokusState<ProcessingHealthRecommendation>,
    bulkReprocessState: BulkReprocessState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onReprocess: () -> Unit,
) {
    val subtitle = stringResource(Res.string.processing_health_subtitle)

    SettingsSection(
        title = stringResource(Res.string.processing_health_title),
        subtitle = subtitle,
        expanded = expanded,
        onToggle = onToggle,
        primary = false,
    ) {
        when {
            processingHealth.isLoading() -> {
                Text(
                    text = "...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }

            processingHealth.isError() -> {
                Text(
                    text = processingHealth.exception.message ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            processingHealth.isSuccess() -> {
                val data = processingHealth.data
                ProcessingHealthContent(
                    data = data,
                    bulkReprocessState = bulkReprocessState,
                    onReprocess = onReprocess,
                )
            }
        }
    }
}

@Composable
private fun ProcessingHealthContent(
    data: ProcessingHealthRecommendation,
    bulkReprocessState: BulkReprocessState,
    onReprocess: () -> Unit,
) {
    DataRow(
        label = stringResource(Res.string.processing_health_recent_processed),
        value = data.totalProcessedLast30Days.toString(),
    )
    DataRow(
        label = stringResource(Res.string.processing_health_needs_review),
        value = data.needsReviewCount.toString(),
    )
    DataRow(
        label = stringResource(Res.string.processing_health_failed),
        value = data.failedCount.toString(),
    )
    DataRow(
        label = stringResource(Res.string.processing_health_eligible),
        value = data.eligibleForReprocessCount.toString(),
    )

    if (data.recommended) {
        Spacer(Modifier.height(Constraints.Spacing.medium))
        Text(
            text = stringResource(Res.string.processing_health_recommendation),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Spacer(Modifier.height(Constraints.Spacing.small))
        Text(
            text = stringResource(Res.string.processing_health_recommendation_retry),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Spacer(Modifier.height(Constraints.Spacing.medium))

        when (bulkReprocessState) {
            is BulkReprocessState.Idle -> {
                PButton(
                    text = stringResource(Res.string.processing_health_reprocess_button),
                    variant = PButtonVariant.Outline,
                    onClick = onReprocess,
                )
            }

            is BulkReprocessState.InProgress -> {
                PButton(
                    text = stringResource(Res.string.processing_health_reprocess_button),
                    variant = PButtonVariant.Outline,
                    onClick = {},
                    isLoading = true,
                )
            }

            is BulkReprocessState.Done -> {
                Text(
                    text = stringResource(
                        Res.string.processing_health_reprocess_done,
                        bulkReprocessState.queuedCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            is BulkReprocessState.Failed -> {
                Text(
                    text = bulkReprocessState.error.message ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(Constraints.Spacing.small))
                PButton(
                    text = stringResource(Res.string.processing_health_reprocess_button),
                    variant = PButtonVariant.Outline,
                    onClick = onReprocess,
                )
            }
        }
    }
}
