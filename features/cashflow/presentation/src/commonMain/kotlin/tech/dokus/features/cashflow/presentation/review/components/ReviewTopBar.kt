package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_confirm
import tech.dokus.aura.resources.cashflow_chat_with_document
import tech.dokus.aura.resources.cashflow_document_review_title
import tech.dokus.aura.resources.cashflow_needs_attention
import tech.dokus.aura.resources.cashflow_needs_input
import tech.dokus.aura.resources.cashflow_somethings_wrong
import tech.dokus.aura.resources.state_confirming
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted

private val StatusDotSize = 6.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReviewTopBar(
    state: DocumentReviewState,
    isLargeScreen: Boolean,
    onBackClick: () -> Unit,
    onChatClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onRejectClick: () -> Unit,
) {
    val content = state as? DocumentReviewState.Content

    Column {
        TopAppBar(
            title = {
                Column {
                    // Primary: Description (counterparty + context)
                    Text(
                        text = content?.description
                            ?: stringResource(Res.string.cashflow_document_review_title),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Understanding line: amount + status
                    if (content != null) {
                        UnderstandingLine(
                            totalAmount = content.totalAmount?.toDisplayString(),
                            isBlocking = content.isBlocking,
                            hasAttention = content.hasAttention,
                            isProcessing = content.isProcessing
                        )
                    }
                }
            },
            navigationIcon = {
                PBackButton(onBackPress = onBackClick)
            },
            actions = {
                val showActions = content != null &&
                    isLargeScreen &&
                    !content.isDocumentConfirmed &&
                    !content.isDocumentRejected
                if (showActions) {
                    val isBusy = content.isConfirming ||
                        content.isSaving ||
                        content.isBindingContact ||
                        content.isRejecting
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Something's wrong" text link (replaces Reject button)
                        TextButton(
                            onClick = onRejectClick,
                            enabled = !isBusy
                        ) {
                            Text(
                                text = stringResource(Res.string.cashflow_somethings_wrong),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.textMuted
                            )
                        }
                        PPrimaryButton(
                            text = if (content.isConfirming) {
                                stringResource(Res.string.state_confirming)
                            } else {
                                stringResource(Res.string.action_confirm)
                            },
                            enabled = content.canConfirm,
                            isLoading = content.isConfirming || content.isBindingContact,
                            onClick = onConfirmClick,
                        )
                    }
                }

                if (content != null && content.isDocumentConfirmed) {
                    IconButton(onClick = onChatClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Message,
                            contentDescription = stringResource(Res.string.cashflow_chat_with_document),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = Constraints.Stroke.thin
        )
    }
}

/**
 * Understanding line showing amount + status.
 * Displays: "€1,234.56 · needs input" or "€1,234.56 · processing"
 */
@Composable
private fun UnderstandingLine(
    totalAmount: String?,
    isBlocking: Boolean,
    hasAttention: Boolean,
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)
    ) {
        // Amount
        Text(
            text = totalAmount ?: "—",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = " · ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted
        )

        when {
            isProcessing -> {
                Text(
                    text = "Processing…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
            isBlocking -> {
                // Amber dot + "needs input"
                Box(
                    modifier = Modifier
                        .size(StatusDotSize)
                        .background(MaterialTheme.colorScheme.statusWarning, CircleShape)
                )
                Spacer(Modifier.width(Constraints.Spacing.xSmall))
                Text(
                    text = stringResource(Res.string.cashflow_needs_input),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.statusWarning
                )
            }
            hasAttention -> {
                // Softer amber dot (still needs attention but not blocking)
                Box(
                    modifier = Modifier
                        .size(StatusDotSize)
                        .background(MaterialTheme.colorScheme.statusWarning.copy(alpha = 0.6f), CircleShape)
                )
                Spacer(Modifier.width(Constraints.Spacing.xSmall))
                Text(
                    text = stringResource(Res.string.cashflow_needs_attention),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            else -> {
                // Ready / normal state
                Text(
                    text = "Ready",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }
    }
}
