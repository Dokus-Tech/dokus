package tech.dokus.features.cashflow.presentation.review

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
import com.composables.icons.lucide.CircleCheck
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageSquare
import org.jetbrains.compose.resources.stringResource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_chat_with_document
import tech.dokus.aura.resources.cashflow_document_confirmed
import tech.dokus.aura.resources.cashflow_document_rejected
import tech.dokus.aura.resources.cashflow_somethings_wrong
import tech.dokus.aura.resources.cashflow_view_cashflow
import tech.dokus.aura.resources.cashflow_view_document
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Footer for Document Review screen.
 *
 * Pre-confirmation: "Something's wrong?" escape hatch only.
 * Post-confirmation: success indicator + view/chat buttons.
 */
@Composable
fun DocumentReviewFooter(
    documentStatus: DocumentStatus?,
    hasCashflowEntry: Boolean,
    onSomethingsWrong: () -> Unit,
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
            PendingFooter(onSomethingsWrong = onSomethingsWrong)
        }
    }
}

@Composable
private fun PendingFooter(
    onSomethingsWrong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constraints.Spacing.medium),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onSomethingsWrong) {
            Text(
                text = stringResource(Res.string.cashflow_somethings_wrong),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textMuted,
            )
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
            documentStatus = DocumentStatus.NeedsReview,
            hasCashflowEntry = false,
            onSomethingsWrong = {},
            onOpenChat = {},
            onViewEntity = {},
            onViewCashflowEntry = {}
        )
    }
}
