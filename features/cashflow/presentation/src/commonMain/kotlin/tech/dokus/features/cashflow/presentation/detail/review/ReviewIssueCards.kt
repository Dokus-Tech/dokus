package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_issue_amount_missing_subtotal
import tech.dokus.aura.resources.review_issue_amount_missing_total
import tech.dokus.aura.resources.review_issue_amount_missing_vat
import tech.dokus.aura.resources.review_issue_amount_title
import tech.dokus.aura.resources.review_issue_contact_suggested
import tech.dokus.aura.resources.review_issue_contact_title
import tech.dokus.aura.resources.review_issue_contact_unknown
import tech.dokus.aura.resources.review_issue_date_correct_due_date
import tech.dokus.aura.resources.review_issue_date_due_before_issue
import tech.dokus.aura.resources.review_issue_date_far_out
import tech.dokus.aura.resources.review_issue_date_keep_as
import tech.dokus.aura.resources.review_issue_date_label_due
import tech.dokus.aura.resources.review_issue_date_label_issue
import tech.dokus.aura.resources.review_issue_date_missing_due
import tech.dokus.aura.resources.review_issue_date_missing_issue
import tech.dokus.aura.resources.review_issue_date_title
import tech.dokus.aura.resources.review_issue_direction_subtitle
import tech.dokus.aura.resources.review_issue_direction_title
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

private val AccentBorderWidth = 2.dp
private val SuggestionShape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)

// =============================
// Contact Issue Card
// =============================

/**
 * Contact issue card — shows suggestion to accept or prompt to search.
 */
@Composable
internal fun ContactIssueCard(
    issue: ReviewIssue.ContactIssue,
    onAcceptSuggestion: () -> Unit,
    onChooseDifferent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        // Title
        IssueTitleLabel(text = stringResource(Res.string.review_issue_contact_title))

        when (val contact = issue.contact) {
            is ResolvedContact.Suggested -> {
                Text(
                    text = stringResource(Res.string.review_issue_contact_suggested),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SuggestionRow(
                    name = contact.name,
                    vatNumber = contact.vatNumber,
                    onClick = onAcceptSuggestion,
                )
            }
            is ResolvedContact.Detected -> {
                Text(
                    text = stringResource(Res.string.review_issue_contact_suggested),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SuggestionRow(
                    name = contact.name,
                    vatNumber = contact.vatNumber,
                    onClick = onChooseDifferent,
                )
            }
            is ResolvedContact.Unknown -> {
                Text(
                    text = stringResource(Res.string.review_issue_contact_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            is ResolvedContact.Linked -> {
                // Should not happen — linked contacts don't produce issues
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    name: String,
    vatNumber: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amberColor = MaterialTheme.colorScheme.primary

    DokusCardSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        accent = true,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // 2dp amber left border accent
                    drawLine(
                        color = amberColor,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = AccentBorderWidth.toPx(),
                    )
                }
                .padding(Constraints.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
}

// =============================
// Amount Issue Card
// =============================

/**
 * Amount issue card — shows which financial amounts are missing.
 */
@Composable
internal fun AmountIssueCard(
    issue: ReviewIssue.AmountIssue,
    totalValue: String,
    subtotalValue: String,
    vatValue: String,
    onUpdateTotal: (String) -> Unit,
    onUpdateSubtotal: (String) -> Unit,
    onUpdateVat: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        IssueTitleLabel(text = stringResource(Res.string.review_issue_amount_title))

        if (issue.missingTotal) {
            AmountFieldRow(
                label = stringResource(Res.string.review_issue_amount_missing_total),
                value = totalValue,
                onValueChange = onUpdateTotal,
            )
        }
        if (issue.missingSubtotal) {
            AmountFieldRow(
                label = stringResource(Res.string.review_issue_amount_missing_subtotal),
                value = subtotalValue,
                onValueChange = onUpdateSubtotal,
            )
        }
        if (issue.missingVat) {
            AmountFieldRow(
                label = stringResource(Res.string.review_issue_amount_missing_vat),
                value = vatValue,
                onValueChange = onUpdateVat,
            )
        }
    }
}

@Composable
private fun AmountFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember(value) { mutableStateOf(value) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        OutlinedTextField(
            value = fieldValue,
            onValueChange = {
                fieldValue = it
                onValueChange(it)
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge,
        )
    }
}

// =============================
// Date Issue Card
// =============================

/**
 * Date issue card — shows date anomaly with optional edit field.
 */
@Composable
internal fun DateIssueCard(
    issue: ReviewIssue.DateIssue,
    onUpdateDueDate: (String) -> Unit,
    onKeepOriginal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        IssueTitleLabel(text = stringResource(Res.string.review_issue_date_title))

        // Description
        val description = when {
            issue.missingIssueDate -> stringResource(Res.string.review_issue_date_missing_issue)
            issue.missingDueDate -> stringResource(Res.string.review_issue_date_missing_due)
            issue.dueDateBeforeIssueDate -> stringResource(Res.string.review_issue_date_due_before_issue)
            issue.dueDateFarOut -> stringResource(Res.string.review_issue_date_far_out)
            else -> ""
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Date display side-by-side (when both dates exist)
        if (issue.issueDate != null || issue.dueDate != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
            ) {
                if (issue.issueDate != null) {
                    DateColumn(
                        label = stringResource(Res.string.review_issue_date_label_issue),
                        value = issue.issueDate.toString(),
                        isError = false,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (issue.dueDate != null) {
                    DateColumn(
                        label = stringResource(Res.string.review_issue_date_label_due),
                        value = issue.dueDate.toString(),
                        isError = issue.dueDateBeforeIssueDate || issue.dueDateFarOut,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Editable due date field (when due date needs correction)
        if (issue.dueDateBeforeIssueDate || issue.dueDateFarOut || issue.missingDueDate) {
            var dueValue by remember(issue.dueDate) {
                mutableStateOf(issue.dueDate?.toString() ?: "")
            }

            Text(
                text = stringResource(Res.string.review_issue_date_correct_due_date),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = dueValue,
                onValueChange = {
                    dueValue = it
                    onUpdateDueDate(it)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
            )

            // "Keep as 2027-03-18" secondary action
            if (issue.dueDate != null) {
                Text(
                    text = stringResource(Res.string.review_issue_date_keep_as, issue.dueDate.toString()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.clickable(onClick = onKeepOriginal),
                )
            }
        }
    }
}

@Composable
private fun DateColumn(
    label: String,
    value: String,
    isError: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(Constraints.Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// =============================
// Direction Issue Card
// =============================

/**
 * Direction issue card — shows direction picker (Inbound/Outbound).
 */
@Composable
internal fun DirectionIssueCard(
    issue: ReviewIssue.DirectionIssue,
    onSelectDirection: (DocumentDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        IssueTitleLabel(text = stringResource(Res.string.review_issue_direction_title))
        Text(
            text = stringResource(Res.string.review_issue_direction_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            DirectionOption(
                label = "Inbound",
                isSelected = issue.currentDirection == DocumentDirection.Inbound,
                onClick = { onSelectDirection(DocumentDirection.Inbound) },
                modifier = Modifier.weight(1f),
            )
            DirectionOption(
                label = "Outbound",
                isSelected = issue.currentDirection == DocumentDirection.Outbound,
                onClick = { onSelectDirection(DocumentDirection.Outbound) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DirectionOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(start = AccentBorderWidth),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row {
            Box(
                modifier = Modifier
                    .width(AccentBorderWidth)
                    .height(48.dp)
                    .background(borderColor)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(Constraints.Spacing.medium),
            )
        }
    }
}

// =============================
// Shared
// =============================

/**
 * Issue title label — amber, uppercase, letter-spaced.
 */
@Composable
internal fun IssueTitleLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

/**
 * Dimmed issue preview — single-line title shown for inactive issues.
 */
@Composable
internal fun DimmedIssuePreview(
    title: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.45f)
            .padding(vertical = Constraints.Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Box(
            modifier = Modifier
                .width(Constraints.Spacing.small)
                .height(Constraints.Spacing.small)
                .background(
                    MaterialTheme.colorScheme.outlineVariant,
                    shape = MaterialTheme.shapes.extraSmall,
                )
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Get a human-readable title for a review issue (for progress bar "Next:" label and dimmed previews).
 */
@Composable
internal fun ReviewIssue.issueTitle(): String = when (this) {
    is ReviewIssue.ContactIssue -> stringResource(Res.string.review_issue_contact_title)
    is ReviewIssue.DirectionIssue -> stringResource(Res.string.review_issue_direction_title)
    is ReviewIssue.AmountIssue -> stringResource(Res.string.review_issue_amount_title)
    is ReviewIssue.DateIssue -> stringResource(Res.string.review_issue_date_title)
}
