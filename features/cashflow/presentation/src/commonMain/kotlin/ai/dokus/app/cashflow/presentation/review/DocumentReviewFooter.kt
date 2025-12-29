package ai.dokus.app.cashflow.presentation.review

import ai.dokus.foundation.design.components.PIcon
import ai.dokus.foundation.design.constrains.Constrains
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.AlertTriangle
import compose.icons.feathericons.Check
import compose.icons.feathericons.CheckCircle
import compose.icons.feathericons.MessageSquare
import compose.icons.feathericons.Save
import compose.icons.feathericons.X

/**
 * Redesigned approval footer for Document Review screen.
 *
 * Pre-confirmation layout:
 * - Error banner (if confirm is blocked)
 * - Action row: [Reject] [Save] [Confirm]
 *
 * Post-confirmation layout:
 * - Success message: "Document confirmed"
 * - [Chat with Document] button
 *
 * @param canConfirm Whether document can be confirmed
 * @param isConfirming Whether confirmation is in progress
 * @param isSaving Whether save is in progress
 * @param isBindingContact Whether contact binding is in progress
 * @param hasUnsavedChanges Whether there are unsaved field edits
 * @param isDocumentConfirmed Whether document has been confirmed
 * @param confirmBlockedReason Why confirm is blocked (for error display)
 * @param onConfirm Callback for confirm action
 * @param onSaveChanges Callback for save action
 * @param onReject Callback for reject action
 * @param onOpenChat Callback to open document chat
 */
@Composable
fun DocumentReviewFooter(
    canConfirm: Boolean,
    isConfirming: Boolean,
    isSaving: Boolean,
    isBindingContact: Boolean,
    hasUnsavedChanges: Boolean,
    isDocumentConfirmed: Boolean,
    confirmBlockedReason: String?,
    onConfirm: () -> Unit,
    onSaveChanges: () -> Unit,
    onReject: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp,
    ) {
        if (isDocumentConfirmed) {
            ConfirmedFooter(
                onOpenChat = onOpenChat,
            )
        } else {
            PendingFooter(
                canConfirm = canConfirm,
                isConfirming = isConfirming,
                isSaving = isSaving,
                isBindingContact = isBindingContact,
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
    hasUnsavedChanges: Boolean,
    confirmBlockedReason: String?,
    onConfirm: () -> Unit,
    onSaveChanges: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLoading = isConfirming || isSaving || isBindingContact

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constrains.Spacing.medium),
    ) {
        // Error banner when confirm is blocked
        AnimatedVisibility(
            visible = confirmBlockedReason != null && !isLoading,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Constrains.Spacing.small),
            ) {
                Row(
                    modifier = Modifier.padding(Constrains.Spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = FeatherIcons.AlertTriangle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(modifier = Modifier.width(Constrains.Spacing.small))
                    Text(
                        text = confirmBlockedReason ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        // Action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Reject button (left side)
            TextButton(
                onClick = onReject,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                PIcon(
                    icon = FeatherIcons.X,
                    description = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Reject")
            }

            // Save + Confirm buttons (right side)
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
            ) {
                // Save button (only visible when there are unsaved changes)
                AnimatedVisibility(visible = hasUnsavedChanges) {
                    OutlinedButton(
                        onClick = onSaveChanges,
                        enabled = !isLoading,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            PIcon(
                                icon = FeatherIcons.Save,
                                description = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save")
                    }
                }

                // Confirm button
                Button(
                    onClick = onConfirm,
                    enabled = canConfirm && !isLoading,
                ) {
                    if (isConfirming || isBindingContact) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        PIcon(
                            icon = FeatherIcons.Check,
                            description = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
private fun ConfirmedFooter(
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constrains.Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Success indicator
        Row(
            modifier = Modifier.padding(bottom = Constrains.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = FeatherIcons.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Text(
                text = "Document confirmed",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        // Chat button
        Button(
            onClick = onOpenChat,
            modifier = Modifier.fillMaxWidth(),
        ) {
            PIcon(
                icon = FeatherIcons.MessageSquare,
                description = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Text("Chat with Document")
        }
    }
}
