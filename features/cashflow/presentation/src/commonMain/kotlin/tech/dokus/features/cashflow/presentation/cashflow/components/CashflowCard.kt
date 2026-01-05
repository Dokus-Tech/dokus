package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.bill_status_cancelled
import tech.dokus.aura.resources.bill_status_draft
import tech.dokus.aura.resources.bill_status_overdue
import tech.dokus.aura.resources.bill_status_paid
import tech.dokus.aura.resources.bill_status_pending
import tech.dokus.aura.resources.bill_status_scheduled
import tech.dokus.aura.resources.cashflow_card_title
import tech.dokus.aura.resources.cashflow_document_number_bill
import tech.dokus.aura.resources.cashflow_document_number_expense
import tech.dokus.aura.resources.document_type_bill
import tech.dokus.aura.resources.document_type_expense
import tech.dokus.aura.resources.document_type_invoice
import tech.dokus.aura.resources.invoice_status_cancelled
import tech.dokus.aura.resources.invoice_status_draft
import tech.dokus.aura.resources.invoice_status_overdue
import tech.dokus.aura.resources.invoice_status_paid
import tech.dokus.aura.resources.invoice_status_partial
import tech.dokus.aura.resources.invoice_status_refunded
import tech.dokus.aura.resources.invoice_status_viewed
import tech.dokus.aura.resources.pending_documents_need_confirmation
import tech.dokus.aura.resources.pending_documents_next
import tech.dokus.aura.resources.pending_documents_previous
import tech.dokus.domain.enums.BillStatus
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding

/**
 * A card component displaying a cash flow list with financial document items and navigation controls.
 *
 * @param documents List of financial documents to display
 * @param onPreviousClick Callback when the previous arrow button is clicked
 * @param onNextClick Callback when the next arrow button is clicked
 * @param modifier Optional modifier for the card
 */
@Composable
fun CashflowCard(
    documents: List<FinancialDocumentDto>,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier,
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Title
            Text(
                text = stringResource(Res.string.cashflow_card_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Document items list
            documents.forEachIndexed { index, document ->
                CashflowDocumentItem(document = document)

                // Add divider between items (not after the last item)
                if (index < documents.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Previous button
                FilledIconButton(
                    onClick = onPreviousClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.pending_documents_previous)
                    )
                }

                // Next button
                FilledIconButton(
                    onClick = onNextClick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(Res.string.pending_documents_next)
                    )
                }
            }
        }
    }
}

/**
 * A single cash flow document item row displaying document number and status badge.
 *
 * @param document The financial document to display
 * @param modifier Optional modifier for the row
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Composable
private fun CashflowDocumentItem(
    document: FinancialDocumentDto,
    modifier: Modifier = Modifier
) {
    val documentNumber = when (document) {
        is FinancialDocumentDto.InvoiceDto -> document.invoiceNumber.toString()
        is FinancialDocumentDto.ExpenseDto -> stringResource(
            Res.string.cashflow_document_number_expense,
            document.id.value
        )
        is FinancialDocumentDto.BillDto -> document.invoiceNumber ?: stringResource(
            Res.string.cashflow_document_number_bill,
            document.id.value
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Document number with optional icon
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document type icon (optional)
            Text(
                text = document.typeIcon(),
                style = MaterialTheme.typography.bodyMedium
            )

            // Document number
            Text(
                text = documentNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Status badge
        DocumentStatusBadge(document = document)
    }
}

/**
 * A status indicator using dot + text pattern (Design System v1).
 * No filled backgrounds, no pills - just a colored dot and text.
 *
 * @param document The financial document to display status for
 * @param modifier Optional modifier for the indicator
 */
@Composable
private fun DocumentStatusBadge(
    document: FinancialDocumentDto,
    modifier: Modifier = Modifier
) {
    // Determine color and text based on document type and status
    val (statusColor, statusText) = when (document) {
        is FinancialDocumentDto.InvoiceDto -> getInvoiceStatusStyle(document.status)
        is FinancialDocumentDto.ExpenseDto -> Pair(
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(Res.string.document_type_expense)
        )
        is FinancialDocumentDto.BillDto -> Pair(
            MaterialTheme.colorScheme.secondary,
            document.status.toDisplayText()
        )
    }

    // Dot + text pattern (Design System v1)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(statusColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = statusColor
        )
    }
}

@Composable
private fun getInvoiceStatusStyle(status: InvoiceStatus): Pair<Color, String> {
    return when (status) {
        InvoiceStatus.Draft -> Pair(
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(Res.string.invoice_status_draft)
        )
        InvoiceStatus.Sent, InvoiceStatus.Overdue -> Pair(
            MaterialTheme.colorScheme.error,
            if (status == InvoiceStatus.Overdue) {
                stringResource(Res.string.invoice_status_overdue)
            } else {
                stringResource(Res.string.pending_documents_need_confirmation)
            }
        )
        InvoiceStatus.Viewed -> Pair(
            MaterialTheme.colorScheme.primary,
            stringResource(Res.string.invoice_status_viewed)
        )
        InvoiceStatus.PartiallyPaid -> Pair(
            MaterialTheme.colorScheme.primary,
            stringResource(Res.string.invoice_status_partial)
        )
        InvoiceStatus.Paid -> Pair(
            MaterialTheme.colorScheme.tertiary,
            stringResource(Res.string.invoice_status_paid)
        )
        InvoiceStatus.Cancelled -> Pair(
            MaterialTheme.colorScheme.onSurfaceVariant,
            stringResource(Res.string.invoice_status_cancelled)
        )
        InvoiceStatus.Refunded -> Pair(
            MaterialTheme.colorScheme.tertiary,
            stringResource(Res.string.invoice_status_refunded)
        )
    }
}

/**
 * Extension function to get the document icon/emoji representation.
 */
@Composable
private fun FinancialDocumentDto.typeIcon(): String = when (this) {
    is FinancialDocumentDto.InvoiceDto -> stringResource(Res.string.document_type_invoice)
    is FinancialDocumentDto.ExpenseDto -> stringResource(Res.string.document_type_expense)
    is FinancialDocumentDto.BillDto -> stringResource(Res.string.document_type_bill)
}

@Composable
private fun BillStatus.toDisplayText(): String = when (this) {
    BillStatus.Draft -> stringResource(Res.string.bill_status_draft)
    BillStatus.Pending -> stringResource(Res.string.bill_status_pending)
    BillStatus.Scheduled -> stringResource(Res.string.bill_status_scheduled)
    BillStatus.Paid -> stringResource(Res.string.bill_status_paid)
    BillStatus.Overdue -> stringResource(Res.string.bill_status_overdue)
    BillStatus.Cancelled -> stringResource(Res.string.bill_status_cancelled)
}
