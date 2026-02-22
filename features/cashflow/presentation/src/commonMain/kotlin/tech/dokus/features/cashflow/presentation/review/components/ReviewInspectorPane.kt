package tech.dokus.features.cashflow.presentation.review.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ReviewFinancialStatus
import tech.dokus.features.cashflow.presentation.review.dotType
import tech.dokus.features.cashflow.presentation.review.localized
import tech.dokus.features.cashflow.presentation.review.models.counterpartyInfo
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.colorized
import tech.dokus.foundation.aura.extensions.localizedUppercase
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.features.cashflow.presentation.review.colorized as financialStatusColorized

@Composable
internal fun ReviewInspectorPane(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val counterparty = counterpartyInfo(state)

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        InspectorHeader(
            state = state,
            onIntent = onIntent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.medium,
                    vertical = Constraints.Spacing.medium,
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(horizontal = Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            InspectorSectionCard(title = "Status") {
                ValueRow(
                    label = "State",
                    value = state.financialStatus.localized
                )
                if (!state.isDocumentConfirmed && !state.isDocumentRejected &&
                    state.financialStatus == ReviewFinancialStatus.Review
                ) {
                    Button(
                        onClick = { onIntent(DocumentReviewIntent.Confirm) },
                        enabled = state.canConfirm,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Confirm document")
                    }
                } else if (state.canRecordPayment) {
                    OutlinedButton(
                        onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Record payment")
                    }
                }
            }

            InspectorSectionCard(title = "Amount") {
                val total = state.totalAmount?.toDisplayString() ?: "\u2014"
                val currencySign = currencySign(state)
                ValueRow(
                    label = "Total",
                    value = "$currencySign$total",
                    emphasized = true
                )
                ValueRow("Subtotal", prefixedAmount(state, state.subtotalAmount()))
                ValueRow("VAT", prefixedAmount(state, state.vatAmount()))
            }

            InspectorSectionCard(title = "Timeline") {
                ValueRow("Issue date", state.issueDate() ?: "\u2014")
                ValueRow("Due date", state.dueDate() ?: "\u2014")
            }

            InspectorSectionCard(title = "Reference") {
                ValueRow("Invoice number", state.referenceNumber() ?: "\u2014")
            }

            InspectorSectionCard(title = "Contact") {
                ValueRow("Name", counterparty.name ?: "Unknown")
                counterparty.address?.let { ValueRow("Address", it) }
            }

            InspectorSectionCard(title = "Sources") {
                if (state.document.sources.isEmpty()) {
                    ValueRow("Source", "No sources")
                } else {
                    state.document.sources.forEach { source ->
                        SourceRow(
                            type = source.sourceChannel,
                            title = source.filename ?: source.sourceChannel.name,
                            onClick = { onIntent(DocumentReviewIntent.OpenSourceModal(source.id)) },
                        )
                    }
                }
            }

            InspectorSectionCard(title = "Transactions") {
                when (val entryState = state.cashflowEntryState) {
                    is DokusState.Success -> {
                        val entry = entryState.data
                        val paidAt = entry.paidAt?.date?.toString()
                        if (paidAt != null) {
                            ValueRow("Payment", "Bank transfer")
                            ValueRow("Date", paidAt)
                        } else {
                            ValueRow("Payment", "No payment recorded")
                        }
                    }
                    is DokusState.Loading -> {
                        ValueRow("Payment", "Loading\u2026")
                    }
                    is DokusState.Error<*> -> {
                        ValueRow("Payment", "Unable to load")
                    }
                    is DokusState.Idle<*> -> {
                        ValueRow("Payment", "No payment recorded")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = { onIntent(DocumentReviewIntent.RequestAmendment) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
        ) {
            Text("Request amendment")
        }
    }
}

@Composable
private fun InspectorHeader(
    state: DocumentReviewState.Content,
    onIntent: (DocumentReviewIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusDot(type = state.financialStatus.dotType, size = 6.dp)
                Text(
                    text = state.financialStatus.localized,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = state.financialStatus.financialStatusColorized,
                )
            }
            Text(
                text = state.issueDate() ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }

        if (!state.isDocumentConfirmed && !state.isDocumentRejected) {
            if (state.isEditMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onIntent(DocumentReviewIntent.CancelEditMode) }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onIntent(DocumentReviewIntent.SaveDraft) },
                        enabled = !state.isSaving,
                    ) {
                        Text("Save")
                    }
                }
            } else {
                OutlinedButton(onClick = { onIntent(DocumentReviewIntent.EnterEditMode) }) {
                    Text("Edit")
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
private fun ValueRow(
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
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
        Text(
            text = "\u203A",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted,
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

private fun currencySign(state: DocumentReviewState.Content): String = when (val data = state.draftData) {
    is InvoiceDraftData -> data.currency.displaySign
    is CreditNoteDraftData -> data.currency.displaySign
    else -> "\u20AC"
}

private fun prefixedAmount(state: DocumentReviewState.Content, value: tech.dokus.domain.Money?): String {
    return value?.let { "${currencySign(state)}${it.toDisplayString()}" } ?: "\u2014"
}

@Preview
@Composable
private fun ReviewInspectorPanePaidPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = CashflowEntryStatus.Paid),
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun ReviewInspectorPaneReviewPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ReviewInspectorPane(
            state = previewReviewContentState(entryStatus = null, isDocumentConfirmed = false),
            onIntent = {},
        )
    }
}
