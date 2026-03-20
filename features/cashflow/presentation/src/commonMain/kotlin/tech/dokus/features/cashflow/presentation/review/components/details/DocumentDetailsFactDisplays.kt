package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_credit_note_number
import tech.dokus.aura.resources.cashflow_direction
import tech.dokus.aura.resources.cashflow_direction_in
import tech.dokus.aura.resources.cashflow_direction_out
import tech.dokus.aura.resources.cashflow_invoice_number
import tech.dokus.aura.resources.cashflow_receipt_number
import tech.dokus.aura.resources.common_date
import tech.dokus.aura.resources.inspector_label_period
import tech.dokus.aura.resources.inspector_label_transactions
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.aura.resources.workspace_iban
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.EditableField
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

@Composable
internal fun InvoiceDetailsFactDisplay(
    direction: DocumentDirection,
    isReadOnly: Boolean,
    onDirectionSelected: (DocumentDirection) -> Unit,
    onIntent: (DocumentReviewIntent) -> Unit,
    invoiceNumber: String?,
    issueDate: String?,
    dueDate: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        DirectionSelector(
            direction = direction,
            isReadOnly = isReadOnly,
            onDirectionSelected = onDirectionSelected
        )
        EditableFactField(
            label = stringResource(Res.string.cashflow_invoice_number),
            value = invoiceNumber,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.InvoiceNumber, it)) },
            isReadOnly = isReadOnly,
        )
        EditableFactField(
            label = stringResource(Res.string.invoice_issue_date),
            value = issueDate,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.IssueDate, it)) },
            isReadOnly = isReadOnly,
        )
        EditableFactField(
            label = stringResource(Res.string.invoice_due_date),
            value = dueDate,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.DueDate, it)) },
            isReadOnly = isReadOnly,
        )
    }
}

@Composable
internal fun ReceiptDetailsFactDisplay(
    receiptNumber: String?,
    date: String?,
    isReadOnly: Boolean = false,
    onIntent: (DocumentReviewIntent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        EditableFactField(
            label = stringResource(Res.string.cashflow_receipt_number),
            value = receiptNumber,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.ReceiptNumber, it)) },
            isReadOnly = isReadOnly,
        )
        EditableFactField(
            label = stringResource(Res.string.common_date),
            value = date,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.ReceiptDate, it)) },
            isReadOnly = isReadOnly,
        )
    }
}

@Composable
internal fun CreditNoteDetailsFactDisplay(
    direction: DocumentDirection,
    isReadOnly: Boolean,
    onDirectionSelected: (DocumentDirection) -> Unit,
    onIntent: (DocumentReviewIntent) -> Unit,
    creditNoteNumber: String?,
    issueDate: String?,
    originalInvoiceNumber: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        DirectionSelector(
            direction = direction,
            isReadOnly = isReadOnly,
            onDirectionSelected = onDirectionSelected
        )
        EditableFactField(
            label = stringResource(Res.string.cashflow_credit_note_number),
            value = creditNoteNumber,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.CreditNoteNumber, it)) },
            isReadOnly = isReadOnly,
        )
        EditableFactField(
            label = stringResource(Res.string.invoice_issue_date),
            value = issueDate,
            onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.IssueDate, it)) },
            isReadOnly = isReadOnly,
        )
        if (!originalInvoiceNumber.isNullOrBlank()) {
            EditableFactField(
                label = stringResource(Res.string.cashflow_invoice_number),
                value = originalInvoiceNumber,
                onValueChanged = { onIntent(DocumentReviewIntent.UpdateField(EditableField.OriginalInvoiceNumber, it)) },
                isReadOnly = isReadOnly,
            )
        }
    }
}

@Composable
internal fun BankStatementDetailsFactDisplay(
    accountIban: String?,
    periodStart: String?,
    periodEnd: String?,
    transactionCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        accountIban?.let { iban ->
            FactField(
                label = stringResource(Res.string.workspace_iban),
                value = iban,
            )
        }
        formatPeriodRange(periodStart, periodEnd)?.let { range ->
            FactField(
                label = stringResource(Res.string.inspector_label_period),
                value = range,
            )
        }
        FactField(
            label = stringResource(Res.string.inspector_label_transactions),
            value = transactionCount.toString(),
        )
    }
}

internal fun formatPeriodRange(start: String?, end: String?): String? = when {
    start != null && end != null -> "$start \u2013 $end"
    start != null -> start
    end != null -> end
    else -> null
}

@Composable
internal fun DirectionSelector(
    direction: DocumentDirection,
    isReadOnly: Boolean,
    onDirectionSelected: (DocumentDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Show toggle only when direction is Unknown and editable; otherwise read-only text
    if (isReadOnly || direction != DocumentDirection.Unknown) {
        FactField(
            label = stringResource(Res.string.cashflow_direction),
            value = cashflowDirectionLabel(direction),
            modifier = modifier,
        )
        return
    }

    val cashflowInSelected = direction == DocumentDirection.Outbound
    val cashflowOutSelected = direction == DocumentDirection.Inbound

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)
    ) {
        MicroLabel(text = stringResource(Res.string.cashflow_direction))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp),
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DirectionChoice(
                text = stringResource(Res.string.cashflow_direction_in),
                selected = cashflowInSelected,
                onClick = { onDirectionSelected(DocumentDirection.Outbound) },
                modifier = Modifier.weight(1f),
            )
            DirectionChoice(
                text = stringResource(Res.string.cashflow_direction_out),
                selected = cashflowOutSelected,
                onClick = { onDirectionSelected(DocumentDirection.Inbound) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DirectionChoice(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.Medium,
        color = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.textMuted
        },
        modifier = modifier
            .background(
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                },
                shape = RoundedCornerShape(9.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = Constraints.Spacing.small),
    )
}

@Composable
private fun cashflowDirectionLabel(direction: DocumentDirection): String = when (direction) {
    DocumentDirection.Inbound -> stringResource(Res.string.cashflow_direction_out)
    DocumentDirection.Outbound -> stringResource(Res.string.cashflow_direction_in)
    DocumentDirection.Neutral,
    DocumentDirection.Unknown -> "—"
}
