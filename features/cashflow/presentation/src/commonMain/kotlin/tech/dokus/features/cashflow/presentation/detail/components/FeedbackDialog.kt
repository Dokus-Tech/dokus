package tech.dokus.features.cashflow.presentation.detail.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_feedback_details_placeholder
import tech.dokus.aura.resources.cashflow_feedback_reject_instead
import tech.dokus.aura.resources.cashflow_feedback_submit
import tech.dokus.aura.resources.cashflow_feedback_title
import tech.dokus.features.cashflow.presentation.detail.FeedbackCategory
import tech.dokus.features.cashflow.presentation.detail.FeedbackDialogState
import tech.dokus.features.cashflow.presentation.detail.localized
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.submitOnEnter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Structured correction dialog with category chips.
 * User selects what's wrong (category), optionally adds details, then re-analyzes.
 * "Reject document" is accessible as a secondary text link.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FeedbackDialog(
    state: FeedbackDialogState,
    onCategorySelected: (FeedbackCategory) -> Unit,
    onFeedbackChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onRejectInstead: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canSubmit = state.selectedCategory != null && !state.isSubmitting

    DokusDialog(
        onDismissRequest = {
            if (!state.isSubmitting) onDismiss()
        },
        title = stringResource(Res.string.cashflow_feedback_title),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
            ) {
                // Category chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                ) {
                    FeedbackCategory.entries.forEach { category ->
                        FilterChip(
                            selected = state.selectedCategory == category,
                            onClick = { onCategorySelected(category) },
                            label = { Text(category.localized) },
                            enabled = !state.isSubmitting,
                        )
                    }
                }

                // Optional details text field
                OutlinedTextField(
                    value = state.feedbackText,
                    onValueChange = onFeedbackChanged,
                    placeholder = { Text(stringResource(Res.string.cashflow_feedback_details_placeholder)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .submitOnEnter(
                            enabled = canSubmit,
                            onSubmit = onSubmit,
                        ),
                    enabled = !state.isSubmitting,
                    minLines = 2,
                    maxLines = 4,
                )

                TextButton(
                    onClick = onRejectInstead,
                    enabled = !state.isSubmitting,
                ) {
                    Text(
                        text = stringResource(Res.string.cashflow_feedback_reject_instead),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.cashflow_feedback_submit),
            onClick = onSubmit,
            isLoading = state.isSubmitting,
            enabled = canSubmit,
        ),
        dismissOnBackPress = !state.isSubmitting,
        dismissOnClickOutside = !state.isSubmitting,
    )
}

@Preview
@Composable
private fun FeedbackDialogPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        FeedbackDialog(
            state = FeedbackDialogState(),
            onCategorySelected = {},
            onFeedbackChanged = {},
            onSubmit = {},
            onRejectInstead = {},
            onDismiss = {},
        )
    }
}
