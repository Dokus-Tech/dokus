package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tech.dokus.domain.Money
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.PdfPreviewPane
import tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun CanonicalCenterPane(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.shouldUsePdfFallback) {
        PdfFallbackCenter(
            previewState = state.previewState,
            selectedFieldPath = state.selectedFieldPath,
            isProcessing = state.isProcessing,
            onLoadMore = { maxPages -> onIntent(DocumentReviewIntent.LoadMorePages(maxPages)) },
            modifier = modifier,
        )
        return
    }

    val draft = when (val data = state.draftData) {
        is InvoiceDraftData -> CanonicalDraft.Invoice(data)
        is CreditNoteDraftData -> CanonicalDraft.CreditNote(data)
        else -> null
    } ?: return

    val counterparty = counterpartyInfo(state)
    val currencySign = draft.currencySign
    val documentLabel = if (draft is CanonicalDraft.Invoice) "Invoice" else "Credit note"
    val documentNumber = draft.documentNumber ?: "\u2014"
    val totalAmount = draft.total ?: "\u2014"

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        DokusCardSurface(
            modifier = Modifier
                .padding(vertical = Constraints.Spacing.large)
                .width(Constraints.DocumentDetail.previewMaxWidth)
                .heightIn(min = 560.dp),
            shape = MaterialTheme.shapes.small,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        horizontal = Constraints.Spacing.xLarge,
                        vertical = Constraints.Spacing.xLarge
                    ),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                    ) {
                        Text(
                            text = counterparty.name ?: state.document.document.filename ?: "Unknown vendor",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        counterparty.address?.let { address ->
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted,
                            )
                        }
                    }
                    Text(
                        text = documentLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "$currencySign$totalAmount",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    draft.description?.let { description ->
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                ) {
                    CanonicalMetaCell("Issue", draft.issueDate ?: "\u2014")
                    if (draft is CanonicalDraft.Invoice) {
                        CanonicalMetaCell("Due", draft.dueDate ?: "\u2014")
                    }
                    CanonicalMetaCell(
                        if (draft is CanonicalDraft.Invoice) "Invoice" else "Credit note",
                        documentNumber
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CanonicalLineItems(
                    lineItems = draft.lineItems,
                    currencySign = currencySign,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                CanonicalTotals(
                    currencySign = currencySign,
                    subtotal = draft.subtotal,
                    vat = draft.vat,
                    total = totalAmount,
                )

                val bankDetails = draft.bankDetails
                if (!bankDetails.isNullOrBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = bankDetails,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }

                val notes = draft.notes
                if (!notes.isNullOrBlank()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfFallbackCenter(
    previewState: DocumentPreviewState,
    selectedFieldPath: String?,
    isProcessing: Boolean,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier,
    ) {
        PdfPreviewPane(
            state = previewState,
            selectedFieldPath = selectedFieldPath,
            onLoadMore = onLoadMore,
            modifier = Modifier.fillMaxSize(),
            showScanAnimation = isProcessing
        )
    }
}

@Composable
private fun CanonicalMetaCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CanonicalLineItems(
    lineItems: List<FinancialLineItem>,
    currencySign: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted,
                textAlign = TextAlign.End,
                modifier = Modifier.width(96.dp),
            )
        }

        if (lineItems.isEmpty()) {
            Text(
                text = "No line items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        } else {
            lineItems.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.description.ifBlank { "\u2014" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = itemLineAmount(item, currencySign),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(96.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CanonicalTotals(
    currencySign: String,
    subtotal: String?,
    vat: String?,
    total: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CanonicalTotalRow("Subtotal", subtotal, currencySign)
        CanonicalTotalRow("VAT", vat, currencySign)
        HorizontalDivider(
            modifier = Modifier.width(220.dp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.width(220.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "$currencySign$total",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun CanonicalTotalRow(
    label: String,
    value: String?,
    currencySign: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.width(220.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = if (value != null) "$currencySign$value" else "\u2014",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private sealed interface CanonicalDraft {
    val documentNumber: String?
    val issueDate: String?
    val subtotal: String?
    val vat: String?
    val total: String?
    val description: String?
    val lineItems: List<FinancialLineItem>
    val bankDetails: String?
    val notes: String?
    val currencySign: String

    data class Invoice(private val draft: InvoiceDraftData) : CanonicalDraft {
        override val documentNumber: String? = draft.invoiceNumber
        override val issueDate: String? = draft.issueDate?.toString()
        val dueDate: String? = draft.dueDate?.toString()
        override val subtotal: String? = draft.subtotalAmount?.toDisplayString()
        override val vat: String? = draft.vatAmount?.toDisplayString()
        override val total: String? = draft.totalAmount?.toDisplayString()
        override val description: String? = draft.notes
        override val lineItems: List<FinancialLineItem> = draft.lineItems
        override val bankDetails: String? = draft.iban?.value
        override val notes: String? = draft.notes
        override val currencySign: String = draft.currency.displaySign
    }

    data class CreditNote(private val draft: CreditNoteDraftData) : CanonicalDraft {
        override val documentNumber: String? = draft.creditNoteNumber
        override val issueDate: String? = draft.issueDate?.toString()
        override val subtotal: String? = draft.subtotalAmount?.toDisplayString()
        override val vat: String? = draft.vatAmount?.toDisplayString()
        override val total: String? = draft.totalAmount?.toDisplayString()
        override val description: String? = draft.reason ?: draft.notes
        override val lineItems: List<FinancialLineItem> = draft.lineItems
        override val bankDetails: String? = null
        override val notes: String? = draft.notes
        override val currencySign: String = draft.currency.displaySign
    }
}

private fun itemLineAmount(item: FinancialLineItem, currencySign: String): String {
    val amountMinor = item.netAmount
        ?: item.unitPrice?.let { unit -> (item.quantity ?: 1L) * unit }
    return amountMinor?.let { "$currencySign${Money(it).toDisplayString()}" } ?: "\u2014"
}
