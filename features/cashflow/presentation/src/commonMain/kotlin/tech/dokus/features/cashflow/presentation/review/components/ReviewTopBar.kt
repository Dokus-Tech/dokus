package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_confirm
import tech.dokus.aura.resources.action_reject
import tech.dokus.aura.resources.cashflow_chat_with_document
import tech.dokus.aura.resources.cashflow_confidence_badge
import tech.dokus.aura.resources.cashflow_document_review_title
import tech.dokus.aura.resources.state_confirming
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.foundation.aura.components.DraftStatusBadge
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.constrains.Constrains

// Confidence thresholds
private const val ConfidenceHighThreshold = 80
private const val ConfidenceMediumThreshold = 50

// Badge dimensions
private val BadgeCornerRadius = 4.dp
private val BadgeHorizontalPadding = 6.dp
private val BadgeVerticalPadding = 2.dp
private const val BadgeBackgroundAlpha = 0.1f

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
                    Text(
                        text = content?.document?.document?.filename
                            ?: stringResource(Res.string.cashflow_document_review_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (content != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            content.document.draft?.draftStatus?.let { draftStatus ->
                                DraftStatusBadge(status = draftStatus)
                            }
                            if (content.showConfidence) {
                                ConfidenceBadge(percent = content.confidencePercent)
                            }
                        }
                    }
                }
            },
            navigationIcon = {
                PBackButton(onBackPress = onBackClick)
            },
            actions = {
                if (content != null && isLargeScreen && !content.isDocumentConfirmed) {
                    val isBusy = content.isConfirming || content.isSaving || content.isBindingContact
                    Row(horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)) {
                        POutlinedButton(
                            text = stringResource(Res.string.action_reject),
                            enabled = !isBusy,
                            onClick = onRejectClick,
                        )
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
            thickness = Constrains.Stroke.thin
        )
    }
}

@Composable
private fun ConfidenceBadge(percent: Int) {
    val color = when {
        percent >= ConfidenceHighThreshold -> MaterialTheme.colorScheme.tertiary
        percent >= ConfidenceMediumThreshold -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.error
    }

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(BadgeCornerRadius))
            .background(color.copy(alpha = BadgeBackgroundAlpha))
            .padding(horizontal = BadgeHorizontalPadding, vertical = BadgeVerticalPadding)
    ) {
        Text(
            text = stringResource(Res.string.cashflow_confidence_badge, percent),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
