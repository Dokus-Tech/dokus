package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_match_review_different_document
import tech.dokus.aura.resources.cashflow_match_review_same_document
import tech.dokus.aura.resources.document_section_payment
import tech.dokus.aura.resources.document_sources_independently_verified
import tech.dokus.aura.resources.inspector_conflict_confirmation
import tech.dokus.aura.resources.inspector_fuzzy_match
import tech.dokus.aura.resources.inspector_label_automation
import tech.dokus.aura.resources.inspector_label_source
import tech.dokus.aura.resources.inspector_label_subtotal
import tech.dokus.aura.resources.inspector_label_total
import tech.dokus.aura.resources.inspector_label_vat
import tech.dokus.aura.resources.inspector_no_sources
import tech.dokus.aura.resources.inspector_section_amount
import tech.dokus.aura.resources.inspector_section_sources
import tech.dokus.aura.resources.payment_auto_paid
import tech.dokus.aura.resources.payment_confidence
import tech.dokus.aura.resources.payment_method_bank_transfer
import tech.dokus.aura.resources.payment_no_payment_recorded
import tech.dokus.aura.resources.payment_record_title
import tech.dokus.aura.resources.payment_unable_to_load
import tech.dokus.aura.resources.payment_undo_auto
import tech.dokus.aura.resources.payment_undoing
import tech.dokus.aura.resources.state_loading
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.ReviewReason
import tech.dokus.domain.model.AutoPaymentStatus
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.EditableField
import tech.dokus.features.cashflow.presentation.review.components.details.EditableAmountRow
import tech.dokus.features.cashflow.presentation.review.dotType
import tech.dokus.features.cashflow.presentation.review.hasCrossMatchedSources
import tech.dokus.features.cashflow.presentation.review.localized
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.colorized
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.foundation.aura.style.greenSoft
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.features.cashflow.presentation.review.colorized as financialStatusColorized

private val AmountAccentWidth = 3.5.dp

@Composable
internal fun InspectorAmountSection(
    total: Money?,
    subtotal: Money?,
    vat: Money?,
    currencySign: String,
    isReadOnly: Boolean = true,
    onIntent: (DocumentReviewIntent) -> Unit = {},
) {
    val subtotalDisplay = subtotal?.let { "$currencySign${it.toDisplayString()}" }
    val vatDisplay = vat?.let { "$currencySign${it.toDisplayString()}" }
    val totalDisplay = total?.let { "$currencySign${it.toDisplayString()}" }

    InspectorSectionCard(title = stringResource(Res.string.inspector_section_amount)) {
        EditableAmountRow(
            label = stringResource(Res.string.inspector_label_subtotal),
            value = subtotalDisplay,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.SubtotalAmount, it)) },
            isReadOnly = isReadOnly,
        )
        EditableAmountRow(
            label = stringResource(Res.string.inspector_label_vat),
            value = vatDisplay,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.VatAmount, it)) },
            isReadOnly = isReadOnly,
        )
        EditableAmountRow(
            label = stringResource(Res.string.inspector_label_total),
            value = totalDisplay,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.TotalAmount, it)) },
            isTotal = true,
            isReadOnly = isReadOnly,
        )
    }
}

@Composable
internal fun InspectorSourcesSection(
    state: DocumentReviewState,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
    showSourceList: Boolean = true,
) {
    if (!showSourceList && state.documentRecord?.pendingMatchReview == null && !state.hasCrossMatchedSources) {
        return
    }

    InspectorSectionCard(title = stringResource(Res.string.inspector_section_sources)) {
        var hasContent = false

        if (showSourceList) {
            if (state.documentRecord?.sources.orEmpty().isEmpty()) {
                InspectorValueRow(stringResource(Res.string.inspector_label_source), stringResource(Res.string.inspector_no_sources))
            } else {
                state.documentRecord?.sources.orEmpty().forEach { source ->
                    SourceRow(
                        type = source.sourceChannel,
                        title = source.filename ?: source.sourceChannel.name,
                        onClick = { onIntent(DocumentReviewIntent.OpenSourceModal(source.id)) },
                    )
                }
            }
            hasContent = true
        }

        state.documentRecord?.pendingMatchReview?.let { review ->
            if (hasContent) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Text(
                text = when (review.reasonType) {
                    ReviewReason.MaterialConflict -> {
                        stringResource(Res.string.inspector_conflict_confirmation)
                    }

                    ReviewReason.FuzzyCandidate -> {
                        stringResource(Res.string.inspector_fuzzy_match)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
            if (!isAccountantReadOnly) {
                Row(horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)) {
                    TextButton(
                        onClick = { onIntent(DocumentReviewIntent.ResolvePossibleMatchSame) },
                        enabled = !state.isResolvingMatchReview,
                    ) {
                        Text(stringResource(Res.string.cashflow_match_review_same_document))
                    }
                    TextButton(
                        onClick = { onIntent(DocumentReviewIntent.ResolvePossibleMatchDifferent) },
                        enabled = !state.isResolvingMatchReview,
                    ) {
                        Text(stringResource(Res.string.cashflow_match_review_different_document))
                    }
                }
            }
            hasContent = true
        }

        if (state.hasCrossMatchedSources) {
            if (hasContent) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.greenSoft.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = Constraints.Spacing.small,
                            vertical = Constraints.Spacing.xSmall,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = MaterialTheme.shapes.small,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        PIcon(
                            icon = Lucide.Check,
                            description = null,
                            tint = MaterialTheme.colorScheme.statusConfirmed,
                            modifier = Modifier.size(Constraints.IconSize.xSmall),
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(Res.string.document_sources_independently_verified),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.statusConfirmed,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun InspectorPaymentSection(
    state: DocumentReviewState,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
) {
    val autoPaymentStatus = (state.autoPaymentStatus as? DokusState.Success<*>)?.data as? AutoPaymentStatus
    InspectorSectionCard(title = stringResource(Res.string.document_section_payment)) {
        when (val entryState = state.cashflowEntryState) {
            is DokusState.Success -> {
                val entry = entryState.data
                val paidAt = entry.paidAt?.date?.toString()
                if (paidAt != null || entry.status == CashflowEntryStatus.Paid) {
                    DokusCardSurface(
                        modifier = Modifier.fillMaxWidth(),
                        variant = DokusCardVariant.Soft,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Constraints.Spacing.small),
                            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                                ) {
                                    StatusDot(type = state.financialStatus.dotType, size = 6.dp)
                                    Column {
                                        Text(
                                            text = stringResource(Res.string.payment_method_bank_transfer),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = paidAt ?: "\u2014",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.textMuted,
                                        )
                                    }
                                }
                                Text(
                                    text = state.financialStatus.localized,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = state.financialStatus.financialStatusColorized,
                                )
                            }
                            if (autoPaymentStatus is AutoPaymentStatus.AutoPaid) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = stringResource(Res.string.payment_auto_paid),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                val confidenceText = "${(autoPaymentStatus.confidenceScore * 100).toInt()}%"
                                Text(
                                    text = stringResource(Res.string.payment_confidence, confidenceText),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.textMuted,
                                )
                                if (autoPaymentStatus.reasons.isNotEmpty()) {
                                    Text(
                                        text = autoPaymentStatus.reasons.joinToString(", ") { formatMatchReason(it) },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.textMuted,
                                    )
                                }
                                if (autoPaymentStatus.canUndo) {
                                    OutlinedButton(
                                        onClick = { onIntent(DocumentReviewIntent.UndoAutoPayment()) },
                                        enabled = !state.isUndoingAutoPayment,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (state.isUndoingAutoPayment) stringResource(Res.string.payment_undoing) else stringResource(Res.string.payment_undo_auto))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    InspectorValueRow(stringResource(Res.string.document_section_payment), stringResource(Res.string.payment_no_payment_recorded))
                    if (!isAccountantReadOnly && state.canRecordPayment) {
                        OutlinedButton(
                            onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.payment_record_title))
                        }
                    }
                }
            }

            is DokusState.Loading -> {
                InspectorValueRow(stringResource(Res.string.document_section_payment), stringResource(Res.string.state_loading))
            }

            is DokusState.Error<*> -> {
                InspectorValueRow(stringResource(Res.string.document_section_payment), stringResource(Res.string.payment_unable_to_load))
                if (!isAccountantReadOnly && state.canRecordPayment) {
                    OutlinedButton(
                        onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.payment_record_title))
                    }
                }
            }

            is DokusState.Idle<*> -> {
                InspectorValueRow(stringResource(Res.string.document_section_payment), stringResource(Res.string.payment_no_payment_recorded))
                if (!isAccountantReadOnly && state.canRecordPayment) {
                    OutlinedButton(
                        onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(Res.string.payment_record_title))
                    }
                }
            }
        }
        if (state.autoPaymentStatus is DokusState.Loading) {
            InspectorValueRow(stringResource(Res.string.inspector_label_automation), stringResource(Res.string.state_loading))
        }
    }
}

@Composable
private fun InspectorSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
            content()
        }
    }
}

@Composable
private fun InspectorValueRow(
    label: String,
    value: String,
    emphasized: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val hasValue = value != "—"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(
                        color = if (hasValue) {
                            MaterialTheme.colorScheme.statusConfirmed.copy(alpha = 0.86f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)
                        },
                        shape = CircleShape,
                    )
            )
            Text(
                text = value,
                style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SourceRow(
    type: DocumentSource,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Constraints.Spacing.xSmall),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = type.localizedUppercase,
            style = MaterialTheme.typography.labelSmall,
            color = type.colorized,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PIcon(
            icon = Lucide.ChevronRight,
            description = null,
            tint = MaterialTheme.colorScheme.textMuted,
        )
    }
}

private fun formatMatchReason(reason: String): String = when (reason) {
    "structured_reference_match" -> "Structured reference match"
    "exact_amount" -> "Exact amount"
    "date_proximity" -> "Date proximity"
    "iban_match" -> "IBAN match"
    "name_similarity" -> "Name similarity"
    "vat_match" -> "VAT number match"
    "invoice_number_match" -> "Invoice number match"
    else -> reason.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
