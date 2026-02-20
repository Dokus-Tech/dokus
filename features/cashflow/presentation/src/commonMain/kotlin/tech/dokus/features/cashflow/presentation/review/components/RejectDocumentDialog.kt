package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_action_keep_reviewing
import tech.dokus.aura.resources.cashflow_action_reject_document
import tech.dokus.aura.resources.cashflow_reject_note_optional
import tech.dokus.aura.resources.cashflow_reject_prompt
import tech.dokus.aura.resources.cashflow_reject_reason_duplicate
import tech.dokus.aura.resources.cashflow_reject_reason_not_my_business
import tech.dokus.aura.resources.cashflow_reject_reason_other
import tech.dokus.aura.resources.cashflow_reject_reason_spam
import tech.dokus.aura.resources.cashflow_reject_reason_test
import tech.dokus.aura.resources.cashflow_reject_title
import tech.dokus.domain.enums.DocumentRejectReason
import tech.dokus.features.cashflow.presentation.review.RejectDialogState
import tech.dokus.foundation.aura.components.common.DokusSelectableRowGroup
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.constrains.Constraints

/**
 * Dialog for rejecting a document with reason selection.
 * Uses DokusDialog with DokusSelectableRowGroup for the reason list.
 *
 * @param state The current dialog state from MVI
 * @param onReasonSelected Called when a reject reason is selected
 * @param onNoteChanged Called when the optional note text changes
 * @param onConfirm Called when the user confirms rejection
 * @param onDismiss Called when the user dismisses the dialog
 */
@Composable
internal fun RejectDocumentDialog(
    state: RejectDialogState,
    onReasonSelected: (DocumentRejectReason) -> Unit,
    onNoteChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reasons = listOf(
        DocumentRejectReason.NotMyBusiness,
        DocumentRejectReason.Duplicate,
        DocumentRejectReason.Spam,
        DocumentRejectReason.Test,
        DocumentRejectReason.Other,
    )

    DokusDialog(
        onDismissRequest = {
            if (!state.isConfirming) onDismiss()
        },
        title = stringResource(Res.string.cashflow_reject_title),
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
            ) {
                Text(
                    text = stringResource(Res.string.cashflow_reject_prompt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(Constraints.Spacing.small))

                DokusSelectableRowGroup(
                    items = reasons,
                    selectedItem = state.selectedReason,
                    onItemSelected = onReasonSelected,
                    itemText = { reason -> reason.toDisplayString() },
                    enabled = !state.isConfirming,
                    requestInitialFocus = true,
                )

                // Optional note field when "Other" is selected
                AnimatedVisibility(
                    visible = state.selectedReason == DocumentRejectReason.Other,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))
                        OutlinedTextField(
                            value = state.otherNote,
                            onValueChange = onNoteChanged,
                            label = { Text(stringResource(Res.string.cashflow_reject_note_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isConfirming,
                            minLines = 2,
                            maxLines = 4,
                        )
                    }
                }
            }
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.cashflow_action_reject_document),
            onClick = onConfirm,
            isLoading = state.isConfirming,
            isDestructive = true,
            enabled = !state.isConfirming
        ),
        secondaryAction = DokusDialogAction(
            text = stringResource(Res.string.cashflow_action_keep_reviewing),
            onClick = onDismiss,
            enabled = !state.isConfirming
        ),
        dismissOnBackPress = !state.isConfirming,
        dismissOnClickOutside = !state.isConfirming
    )
}

/**
 * Extension to get localized display string for reject reasons.
 */
@Composable
private fun DocumentRejectReason.toDisplayString(): String {
    return when (this) {
        DocumentRejectReason.NotMyBusiness -> stringResource(Res.string.cashflow_reject_reason_not_my_business)
        DocumentRejectReason.Duplicate -> stringResource(Res.string.cashflow_reject_reason_duplicate)
        DocumentRejectReason.Spam -> stringResource(Res.string.cashflow_reject_reason_spam)
        DocumentRejectReason.Test -> stringResource(Res.string.cashflow_reject_reason_test)
        DocumentRejectReason.Other -> stringResource(Res.string.cashflow_reject_reason_other)
    }
}
