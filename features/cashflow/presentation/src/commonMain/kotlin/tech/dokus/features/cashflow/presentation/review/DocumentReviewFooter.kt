package tech.dokus.features.cashflow.presentation.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquare
import com.composables.icons.lucide.Save
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_confirm
import tech.dokus.aura.resources.action_save
import tech.dokus.aura.resources.cashflow_chat_with_document
import tech.dokus.aura.resources.cashflow_document_confirmed
import tech.dokus.aura.resources.cashflow_document_rejected
import tech.dokus.aura.resources.cashflow_needed_to_complete
import tech.dokus.aura.resources.cashflow_somethings_wrong
import tech.dokus.aura.resources.cashflow_view_cashflow
import tech.dokus.aura.resources.cashflow_view_document
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Redesigned approval footer for Document Review screen.
 *
 * Pre-confirmation layout:
 * - Error banner (if confirm is blocked)
 * - Action row: [Reject] [Save] [Confirm]
 *
 * Post-confirmation layout:
 * - Success message: "Document confirmed"
 * - [View Entity] [View Cashflow Entry] buttons
 * - [Chat with Document] button
 *
 * @param canConfirm Whether document can be confirmed
 * @param isConfirming Whether confirmation is in progress
 * @param isSaving Whether save is in progress
 * @param isBindingContact Whether contact binding is in progress
 * @param isRejecting Whether reject is in progress
 * @param hasUnsavedChanges Whether there are unsaved field edits
 * @param isDocumentConfirmed Whether document has been confirmed
 * @param isDocumentRejected Whether document has been rejected
 * @param hasCashflowEntry Whether a cashflow entry was created
 * @param confirmBlockedReason Why confirm is blocked (for error display)
 * @param onConfirm Callback for confirm action
 * @param onSaveChanges Callback for save action
 * @param onReject Callback for reject action
 * @param onOpenChat Callback to open document chat
 * @param onViewEntity Callback to view the created entity
 * @param onViewCashflowEntry Callback to view the cashflow entry
 */
@Composable
fun DocumentReviewFooter(
    canConfirm: Boolean,
    isConfirming: Boolean,
    isSaving: Boolean,
    isBindingContact: Boolean,
    isRejecting: Boolean,
    hasUnsavedChanges: Boolean,
    documentStatus: DocumentStatus?,
    hasCashflowEntry: Boolean,
    confirmBlockedReason: StringResource?,
    onConfirm: () -> Unit,
    onSaveChanges: () -> Unit,
    onReject: () -> Unit,
    onOpenChat: () -> Unit,
    onViewEntity: () -> Unit,
    onViewCashflowEntry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        val isConfirmed = documentStatus == DocumentStatus.Confirmed
        val isRejected = documentStatus == DocumentStatus.Rejected
        if (isConfirmed || isRejected) {
            ConfirmedFooter(
                onOpenChat = onOpenChat,
                onViewEntity = onViewEntity,
                onViewCashflowEntry = onViewCashflowEntry,
                label = if (isRejected) {
                    stringResource(Res.string.cashflow_document_rejected)
                } else {
                    stringResource(Res.string.cashflow_document_confirmed)
                },
                showChat = !isRejected,
                showViewActions = isConfirmed,
                hasCashflowEntry = hasCashflowEntry
            )
        } else {
            PendingFooter(
                canConfirm = canConfirm,
                isConfirming = isConfirming,
                isSaving = isSaving,
                isBindingContact = isBindingContact,
                isRejecting = isRejecting,
                hasUnsavedChanges = hasUnsavedChanges,
                confirmBlockedReason = confirmBlockedReason,
                onConfirm = onConfirm,
                onSaveChanges = onSaveChanges,
                onReject = onReject,
            )
        }
    }
}

@Composable
private fun PendingFooter(
    canConfirm: Boolean,
    isConfirming: Boolean,
    isSaving: Boolean,
    isBindingContact: Boolean,
    isRejecting: Boolean,
    hasUnsavedChanges: Boolean,
    confirmBlockedReason: StringResource?,
    onConfirm: () -> Unit,
    onSaveChanges: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = isConfirming || isSaving || isBindingContact || isRejecting

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constraints.Spacing.medium),
    ) {
        // Narrative hint when confirm is blocked (no alarm icon, subtle text)
        AnimatedVisibility(
            visible = confirmBlockedReason != null && !isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Text(
                text = stringResource(Res.string.cashflow_needed_to_complete),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.padding(bottom = Constraints.Spacing.small),
            )
        }

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "Something's wrong" text link (left side)
            TextButton(
                onClick = onReject,
                enabled = !isLoading,
            ) {
                Text(
                    text = stringResource(Res.string.cashflow_somethings_wrong),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }

            // Save + Confirm buttons (right side)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                // Save button (only visible when there are unsaved changes)
                AnimatedVisibility(visible = hasUnsavedChanges) {
                    PButton(
                        text = stringResource(Res.string.action_save),
                        variant = PButtonVariant.Outline,
                        icon = Lucide.Save,
                        isLoading = isSaving,
                        isEnabled = !isLoading,
                        onClick = onSaveChanges,
                    )
                }

                // Confirm button
                PButton(
                    text = stringResource(Res.string.action_confirm),
                    icon = Lucide.Check,
                    isLoading = isConfirming || isBindingContact,
                    isEnabled = canConfirm && !isLoading,
                    onClick = onConfirm,
                )
            }
        }
    }
}

@Composable
private fun ConfirmedFooter(
    onOpenChat: () -> Unit,
    onViewEntity: () -> Unit,
    onViewCashflowEntry: () -> Unit,
    label: String,
    showChat: Boolean,
    showViewActions: Boolean,
    hasCashflowEntry: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constraints.Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Success indicator
        Row(
            modifier = Modifier.padding(bottom = Constraints.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Lucide.CircleCheck,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(Constraints.Spacing.small))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        // View entity and cashflow entry buttons
        if (showViewActions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Constraints.Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                PButton(
                    text = stringResource(Res.string.cashflow_view_document),
                    variant = PButtonVariant.Outline,
                    modifier = Modifier.weight(1f),
                    onClick = onViewEntity,
                )
                if (hasCashflowEntry) {
                    PButton(
                        text = stringResource(Res.string.cashflow_view_cashflow),
                        variant = PButtonVariant.Outline,
                        modifier = Modifier.weight(1f),
                        onClick = onViewCashflowEntry,
                    )
                }
            }
        }

        // Chat button
        if (showChat) {
            PButton(
                text = stringResource(Res.string.cashflow_chat_with_document),
                icon = Lucide.MessageSquare,
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenChat,
            )
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun DocumentReviewFooterPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentReviewFooter(
            canConfirm = true,
            isConfirming = false,
            isSaving = false,
            isBindingContact = false,
            isRejecting = false,
            hasUnsavedChanges = false,
            documentStatus = DocumentStatus.NeedsReview,
            hasCashflowEntry = false,
            confirmBlockedReason = null,
            onConfirm = {},
            onSaveChanges = {},
            onReject = {},
            onOpenChat = {},
            onViewEntity = {},
            onViewCashflowEntry = {}
        )
    }
}
