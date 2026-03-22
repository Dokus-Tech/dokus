package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.review_duplicate_diff_confirm
import tech.dokus.aura.resources.review_duplicate_diff_opinion
import tech.dokus.aura.resources.review_duplicate_different_document
import tech.dokus.aura.resources.review_duplicate_existing
import tech.dokus.aura.resources.review_duplicate_impact_update
import tech.dokus.aura.resources.review_duplicate_incoming
import tech.dokus.aura.resources.review_duplicate_label_invoice
import tech.dokus.aura.resources.review_duplicate_label_issue_date
import tech.dokus.aura.resources.review_duplicate_label_total
import tech.dokus.aura.resources.review_duplicate_possible_duplicate
import tech.dokus.aura.resources.review_duplicate_review_later
import tech.dokus.aura.resources.review_duplicate_same_document
import tech.dokus.aura.resources.review_duplicate_same_opinion
import tech.dokus.aura.resources.review_duplicate_same_opinion_detail
import tech.dokus.aura.resources.review_surface_view_full_detail
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.sortDate
import tech.dokus.domain.model.totalAmount
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.detail.DuplicateDiff
import tech.dokus.features.cashflow.presentation.detail.DuplicateReviewState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

/**
 * Desktop duplicate comparison surface.
 *
 * Shows two document identity cards side by side with inline diffs
 * and "Same / Different" action buttons. Uses [DuplicateReviewState]
 * from the dedicated [DuplicateReviewContainer].
 */
@Composable
internal fun DesktopDuplicateReviewSurface(
    state: DuplicateReviewState,
    onResolveSame: () -> Unit,
    onResolveDifferent: () -> Unit,
    onSwitchToDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.isLoaded) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DokusLoader()
        }
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Constraints.Spacing.large),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                ) {
                    StatusDot(type = StatusDotType.Warning, size = 8.dp)
                    Text(
                        text = stringResource(Res.string.review_duplicate_possible_duplicate),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = stringResource(Res.string.review_surface_view_full_detail) + " \u2192",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.clickable(onClick = onSwitchToDetail),
                )
            }

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 700.dp)
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Two document cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                ) {
                    DocumentIdentityCard(
                        label = stringResource(Res.string.review_duplicate_existing),
                        labelColor = MaterialTheme.colorScheme.tertiary,
                        borderColor = MaterialTheme.colorScheme.tertiary,
                        draft = state.existingDraft,
                        statusLabel = "Confirmed",
                        statusColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f),
                    )
                    DocumentIdentityCard(
                        label = stringResource(Res.string.review_duplicate_incoming),
                        labelColor = MaterialTheme.colorScheme.primary,
                        borderColor = MaterialTheme.colorScheme.primary,
                        draft = state.incomingDraft,
                        statusLabel = "Processing",
                        statusColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Diff section
                DuplicateDiffSection(
                    reasonType = state.reasonType ?: ReviewReason.MaterialConflict,
                    diffs = state.diffs,
                    existingDraft = state.existingDraft,
                    incomingDraft = state.incomingDraft,
                )

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                ) {
                    PButton(
                        text = stringResource(Res.string.review_duplicate_different_document),
                        variant = PButtonVariant.OutlineMuted,
                        isEnabled = !state.isResolving,
                        onClick = onResolveDifferent,
                    )
                    PButton(
                        text = stringResource(Res.string.review_duplicate_same_document),
                        isEnabled = !state.isResolving,
                        isLoading = state.isResolving,
                        modifier = Modifier.weight(1f),
                        onClick = onResolveSame,
                    )
                }

                Text(
                    text = stringResource(Res.string.review_duplicate_review_later),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.clickable { /* skip */ },
                )
            }

            // Bottom bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                ReviewKeyboardHints(canConfirm = true)

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    thickness = Constraints.Stroke.thin,
                )

                Text(
                    text = stringResource(Res.string.review_surface_view_full_detail) + " \u2192",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier
                        .clickable(onClick = onSwitchToDetail)
                        .padding(vertical = Constraints.Spacing.small),
                )
            }
        }
    }
}

@Composable
private fun DocumentIdentityCard(
    label: String,
    labelColor: Color,
    borderColor: Color,
    draft: DocDto?,
    statusLabel: String,
    statusColor: Color,
    modifier: Modifier = Modifier,
) {
    val vendorName = when (draft) {
        is DocDto.Invoice.Draft -> draft.counterparty.name ?: ""
        is DocDto.Invoice.Confirmed -> ""
        is DocDto.CreditNote.Draft -> draft.counterparty.name ?: ""
        is DocDto.CreditNote.Confirmed -> ""
        else -> ""
    }
    val invoiceNumber = when (draft) {
        is DocDto.Invoice -> draft.invoiceNumber ?: ""
        is DocDto.CreditNote -> draft.creditNoteNumber ?: ""
        else -> ""
    }
    val totalAmount = draft?.totalAmount?.let { "\u20AC${it.toDisplayString()}" } ?: ""
    val dateDisplay = draft?.sortDate?.toString() ?: ""

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = labelColor)

        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(color = borderColor, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2.dp.toPx())
                    }
                    .padding(Constraints.Spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                Text(text = vendorName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (invoiceNumber.isNotBlank()) {
                    Text(text = "#$invoiceNumber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.textMuted)
                }
                Text(text = totalAmount, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface)
                Text(text = dateDisplay, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.textMuted)
            }
        }

        Text(text = statusLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = statusColor, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun DuplicateDiffSection(
    reasonType: ReviewReason,
    diffs: List<DuplicateDiff>,
    existingDraft: DocDto?,
    incomingDraft: DocDto?,
    modifier: Modifier = Modifier,
) {
    val reasonTitle = when (reasonType) {
        ReviewReason.MaterialConflict -> "AMOUNT CHANGED"
        ReviewReason.FuzzyCandidate -> "POSSIBLE MATCH"
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium)) {
        IssueTitleLabel(text = reasonTitle)

        // System opinion
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
            StatusDot(
                type = if (diffs.isEmpty()) StatusDotType.Confirmed else StatusDotType.Warning,
                size = 6.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall)) {
                Text(
                    text = if (diffs.isEmpty()) stringResource(Res.string.review_duplicate_same_opinion) else stringResource(Res.string.review_duplicate_diff_opinion),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (diffs.isEmpty()) stringResource(Res.string.review_duplicate_same_opinion_detail) else stringResource(Res.string.review_duplicate_diff_confirm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }

        // Inline diffs
        if (diffs.isNotEmpty()) {
            val amberColor = MaterialTheme.colorScheme.primary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(color = amberColor, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2.dp.toPx())
                    }
                    .padding(start = Constraints.Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
            ) {
                diffs.forEach { diff ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = when (diff.field) {
                                "total" -> stringResource(Res.string.review_duplicate_label_total)
                                "invoiceNo" -> stringResource(Res.string.review_duplicate_label_invoice)
                                "issueDate" -> stringResource(Res.string.review_duplicate_label_issue_date)
                                else -> diff.field
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)) {
                            Text(
                                text = diff.existingValue,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.textMuted,
                            )
                            Text(text = "\u2192", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.textMuted)
                            Text(
                                text = diff.incomingValue,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }

            // Impact note
            val totalDiff = diffs.firstOrNull { it.field == "total" }
            if (totalDiff != null) {
                Text(
                    text = stringResource(Res.string.review_duplicate_impact_update, totalDiff.existingValue, totalDiff.incomingValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    }
}
