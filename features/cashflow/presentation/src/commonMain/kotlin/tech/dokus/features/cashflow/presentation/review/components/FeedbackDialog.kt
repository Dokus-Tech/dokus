package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_feedback_placeholder
import tech.dokus.aura.resources.cashflow_feedback_reject_instead
import tech.dokus.aura.resources.cashflow_feedback_submit
import tech.dokus.aura.resources.cashflow_feedback_title
import tech.dokus.features.cashflow.presentation.review.FeedbackDialogState
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.constrains.Constrains

/**
 * Dialog for providing correction feedback before re-analysis.
 * Correction-first: user describes what's wrong, system re-processes with feedback.
 * "Reject document" is accessible as a secondary text link.
 */
@Composable
internal fun FeedbackDialog(
    state: FeedbackDialogState,
    onFeedbackChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onRejectInstead: () -> Unit,
    onDismiss: () -> Unit,
) {
    DokusDialog(
        onDismissRequest = {
            if (!state.isSubmitting) onDismiss()
        },
        title = stringResource(Res.string.cashflow_feedback_title),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
            ) {
                OutlinedTextField(
                    value = state.feedbackText,
                    onValueChange = onFeedbackChanged,
                    placeholder = { Text(stringResource(Res.string.cashflow_feedback_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
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
            enabled = state.feedbackText.isNotBlank() && !state.isSubmitting,
        ),
        dismissOnBackPress = !state.isSubmitting,
        dismissOnClickOutside = !state.isSubmitting,
    )
}
