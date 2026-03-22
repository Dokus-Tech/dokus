package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_surface_looks_good
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailIntent
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.EditableField
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

/**
 * The decision stream — the heart of the review surface.
 *
 * Renders either:
 * - Clean state (no issues): green "Looks good" with contact card
 * - Issue state: active issue card + dimmed upcoming issues + progress bar
 */
@Composable
internal fun ReviewDecisionStream(
    state: DocumentDetailState,
    issues: List<ReviewIssue>,
    activeIssueIndex: Int,
    onIntent: (DocumentDetailIntent) -> Unit,
    onChooseDifferent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
    ) {
        if (issues.isEmpty()) {
            CleanDocumentState(state = state)
        } else {
            // Progress bar (only for multi-issue)
            if (issues.size > 1) {
                val nextTitle = issues.getOrNull(activeIssueIndex + 1)?.let {
                    @Composable { it.issueTitle() }
                }
                ReviewProgressBar(
                    currentStep = activeIssueIndex,
                    totalSteps = issues.size,
                    nextIssueTitle = nextTitle?.invoke(),
                )
            }

            // Active issue card
            val activeIssue = issues.getOrNull(activeIssueIndex)
            if (activeIssue != null) {
                ActiveIssueCard(
                    issue = activeIssue,
                    state = state,
                    onIntent = onIntent,
                    onChooseDifferent = onChooseDifferent,
                )
            }

            // Dimmed upcoming issues
            issues.forEachIndexed { index, issue ->
                if (index > activeIssueIndex) {
                    DimmedIssuePreview(title = issue.issueTitle())
                }
            }
        }
    }
}

@Composable
private fun CleanDocumentState(
    state: DocumentDetailState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        // Green "Looks good" indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            StatusDot(type = StatusDotType.Confirmed, size = 8.dp)
            Text(
                text = stringResource(Res.string.review_surface_looks_good),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Contact info card (if contact is resolved)
        when (val contact = state.effectiveContact) {
            is ResolvedContact.Linked -> {
                CleanContactRow(
                    name = contact.name,
                    vatNumber = contact.vatNumber,
                )
            }
            is ResolvedContact.Suggested -> {
                CleanContactRow(
                    name = contact.name,
                    vatNumber = contact.vatNumber,
                )
            }
            is ResolvedContact.Detected -> {
                CleanContactRow(
                    name = contact.name,
                    vatNumber = contact.vatNumber,
                )
            }
            is ResolvedContact.Unknown -> {
                // No contact to display — still clean (contact not required)
            }
        }
    }
}

@Composable
private fun CleanContactRow(
    name: String,
    vatNumber: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(start = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Green left border
        Box(
            modifier = Modifier
                .size(3.dp, 48.dp)
                .background(MaterialTheme.colorScheme.tertiary)
        )
        Column(
            modifier = Modifier.padding(Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!vatNumber.isNullOrBlank()) {
                Text(
                    text = vatNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    }
}

@Composable
private fun ActiveIssueCard(
    issue: ReviewIssue,
    state: DocumentDetailState,
    onIntent: (DocumentDetailIntent) -> Unit,
    onChooseDifferent: () -> Unit,
) {
    when (issue) {
        is ReviewIssue.ContactIssue -> ContactIssueCard(
            issue = issue,
            onAcceptSuggestion = { onIntent(DocumentDetailIntent.AcceptSuggestedContact) },
            onChooseDifferent = onChooseDifferent,
        )
        is ReviewIssue.DirectionIssue -> DirectionIssueCard(
            issue = issue,
            onSelectDirection = { onIntent(DocumentDetailIntent.SelectDirection(it)) },
        )
        is ReviewIssue.AmountIssue -> AmountIssueCard(
            issue = issue,
            totalValue = "",
            subtotalValue = "",
            vatValue = "",
            onUpdateTotal = { onIntent(DocumentDetailIntent.UpdateField(EditableField.TotalAmount, it)) },
            onUpdateSubtotal = { onIntent(DocumentDetailIntent.UpdateField(EditableField.SubtotalAmount, it)) },
            onUpdateVat = { onIntent(DocumentDetailIntent.UpdateField(EditableField.VatAmount, it)) },
        )
        is ReviewIssue.DateIssue -> DateIssueCard(
            issue = issue,
            onUpdateDueDate = { onIntent(DocumentDetailIntent.UpdateField(EditableField.DueDate, it)) },
            onKeepOriginal = { /* Accept as-is, trigger confirm */ onIntent(DocumentDetailIntent.Confirm) },
        )
    }
}
