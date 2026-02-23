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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_match_review_different_document
import tech.dokus.aura.resources.cashflow_match_review_same_document
import tech.dokus.aura.resources.document_section_payment
import tech.dokus.aura.resources.document_sources_independently_verified
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentMatchReviewReasonType
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ReviewFinancialStatus
import tech.dokus.features.cashflow.presentation.review.dotType
import tech.dokus.features.cashflow.presentation.review.hasCrossMatchedSources
import tech.dokus.features.cashflow.presentation.review.localized
import tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo
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
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.features.cashflow.presentation.review.colorized as financialStatusColorized

private val AmountAccentWidth = 3.5.dp

@Composable
internal fun InspectorAmountSection(state: DocumentReviewState.Content) {
    val total = state.totalAmount?.toDisplayString() ?: "\u2014"
    val currencySign = state.currencySign()
    val accentColor = when (state.financialStatus) {
        ReviewFinancialStatus.Paid -> MaterialTheme.colorScheme.statusConfirmed
        ReviewFinancialStatus.Overdue -> MaterialTheme.colorScheme.statusError
        ReviewFinancialStatus.Unpaid,
        ReviewFinancialStatus.Review -> MaterialTheme.colorScheme.statusWarning
    }

    InspectorSectionCard(title = "Amount") {
        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Constraints.Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = AmountAccentWidth, height = 46.dp)
                        .background(
                            color = accentColor,
                            shape = MaterialTheme.shapes.small,
                        )
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$currencySign$total",
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = 24.sp),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        InspectorValueRow("Subtotal", state.prefixedAmount(state.subtotalAmount()))
        InspectorValueRow("VAT", state.prefixedAmount(state.vatAmount()))
    }
}

@Composable
internal fun InspectorTimelineSection(state: DocumentReviewState.Content) {
    InspectorSectionCard(title = "Timeline") {
        InspectorValueRow("Issue date", state.issueDate() ?: "\u2014")
        InspectorValueRow("Due date", state.dueDate() ?: "\u2014")
    }
}

@Composable
internal fun InspectorReferenceSection(state: DocumentReviewState.Content) {
    InspectorSectionCard(title = "Reference") {
        InspectorValueRow("Invoice number", state.referenceNumber() ?: "\u2014")
    }
}

@Composable
internal fun InspectorContactSection(state: DocumentReviewState.Content) {
    val counterparty = counterpartyInfo(state)
    InspectorSectionCard(title = "Contact") {
        InspectorValueRow("Name", counterparty.name ?: "Unknown")
        counterparty.address?.let { address ->
            InspectorValueRow("Address", address)
        }
    }
}

@Composable
internal fun InspectorSourcesSection(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
) {
    InspectorSectionCard(title = "Sources") {
        if (state.document.sources.isEmpty()) {
            InspectorValueRow("Source", "No sources")
        } else {
            state.document.sources.forEach { source ->
                SourceRow(
                    type = source.sourceChannel,
                    title = source.filename ?: source.sourceChannel.name,
                    onClick = { onIntent(DocumentReviewIntent.OpenSourceModal(source.id)) },
                )
            }
        }

        state.document.pendingMatchReview?.let { review ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = when (review.reasonType) {
                    DocumentMatchReviewReasonType.MaterialConflict -> {
                        "Conflicting financial facts require confirmation."
                    }

                    DocumentMatchReviewReasonType.FuzzyCandidate -> {
                        "Potential same document found with fuzzy identity match."
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
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

        if (state.hasCrossMatchedSources) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
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
                            icon = Icons.Outlined.Check,
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
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
) {
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Constraints.Spacing.small),
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
                                        text = "Bank transfer",
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
                    }
                } else {
                    InspectorValueRow("Payment", "No payment recorded")
                    if (state.canRecordPayment) {
                        OutlinedButton(
                            onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Record payment")
                        }
                    }
                }
            }

            is DokusState.Loading -> {
                InspectorValueRow("Payment", "Loading\u2026")
            }

            is DokusState.Error<*> -> {
                InspectorValueRow("Payment", "Unable to load")
                if (state.canRecordPayment) {
                    OutlinedButton(
                        onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Record payment")
                    }
                }
            }

            is DokusState.Idle<*> -> {
                InspectorValueRow("Payment", "No payment recorded")
                if (state.canRecordPayment) {
                    OutlinedButton(
                        onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Record payment")
                    }
                }
            }
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
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
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            description = null,
            tint = MaterialTheme.colorScheme.textMuted,
        )
    }
}

private fun DocumentReviewState.Content.referenceNumber(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.invoiceNumber
    is CreditNoteDraftData -> data.creditNoteNumber
    else -> null
}

private fun DocumentReviewState.Content.issueDate(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.issueDate?.toString()
    is CreditNoteDraftData -> data.issueDate?.toString()
    else -> null
}

private fun DocumentReviewState.Content.dueDate(): String? = when (val data = draftData) {
    is InvoiceDraftData -> data.dueDate?.toString()
    else -> null
}

private fun DocumentReviewState.Content.subtotalAmount() = when (val data = draftData) {
    is InvoiceDraftData -> data.subtotalAmount
    is CreditNoteDraftData -> data.subtotalAmount
    else -> null
}

private fun DocumentReviewState.Content.vatAmount() = when (val data = draftData) {
    is InvoiceDraftData -> data.vatAmount
    is CreditNoteDraftData -> data.vatAmount
    else -> null
}

private fun DocumentReviewState.Content.currencySign(): String = when (val data = draftData) {
    is InvoiceDraftData -> data.currency.displaySign
    is CreditNoteDraftData -> data.currency.displaySign
    else -> "\u20AC"
}

private fun DocumentReviewState.Content.prefixedAmount(value: tech.dokus.domain.Money?): String =
    value?.let { "${currencySign()}${it.toDisplayString()}" } ?: "\u2014"
