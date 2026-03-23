package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_amount_with_currency
import tech.dokus.aura.resources.cashflow_document_number_expense
import tech.dokus.aura.resources.date_format_long
import tech.dokus.aura.resources.date_month_long_april
import tech.dokus.aura.resources.date_month_long_august
import tech.dokus.aura.resources.date_month_long_december
import tech.dokus.aura.resources.date_month_long_february
import tech.dokus.aura.resources.date_month_long_january
import tech.dokus.aura.resources.date_month_long_july
import tech.dokus.aura.resources.date_month_long_june
import tech.dokus.aura.resources.date_month_long_march
import tech.dokus.aura.resources.date_month_long_may
import tech.dokus.aura.resources.date_month_long_november
import tech.dokus.aura.resources.date_month_long_october
import tech.dokus.aura.resources.date_month_long_september
import tech.dokus.aura.resources.document_table_amount
import tech.dokus.aura.resources.document_table_contact
import tech.dokus.aura.resources.document_table_date
import tech.dokus.aura.resources.document_table_invoice
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.currency
import tech.dokus.domain.model.sortDate
import tech.dokus.domain.model.totalAmount
import tech.dokus.foundation.aura.components.CashflowType
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.style.dokusSizing
import tech.dokus.foundation.aura.style.dokusSpacing

internal val AmountGroupingRegex = Regex("(\\d)(?=(\\d{3})+$)")

/**
 * Data class representing a financial document table row.
 * This maps from DocDto domain model to UI-specific structure.
 */
data class FinancialDocumentRow(
    val id: DocumentId?,
    val invoiceNumber: String,
    val contactName: String,
    val contactEmail: String,
    val amount: String,
    val date: String,
    val cashflowType: CashflowType,
    val hasAlert: Boolean = false
)

/**
 * Converts a DocDto to a FinancialDocumentRow for display.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Composable
fun DocDto.toTableRow(): FinancialDocumentRow {
    val cashflowType = when (this) {
        is DocDto.Invoice -> {
            if (this.direction == DocumentDirection.Inbound) CashflowType.CashOut else CashflowType.CashIn
        }
        is DocDto.Receipt -> CashflowType.CashOut
        is DocDto.CreditNote -> CashflowType.CashOut
        is DocDto.BankStatement -> CashflowType.CashOut
        is DocDto.ProForma -> CashflowType.CashIn
        is DocDto.Quote -> CashflowType.CashIn
        is DocDto.PurchaseOrder -> CashflowType.CashOut
        is DocDto.ClassifiedDoc -> CashflowType.CashOut
    }

    val contactName = when (this) {
        is DocDto.Receipt -> this.merchantName.orEmpty()
        is DocDto.Invoice,
        is DocDto.CreditNote,
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc -> ""
    }

    val contactEmail = ""

    val documentNumber = when (this) {
        is DocDto.Invoice.Confirmed -> invoiceNumber.orEmpty()
        is DocDto.Invoice.Draft -> invoiceNumber.orEmpty()
        is DocDto.Receipt.Confirmed -> stringResource(
            Res.string.cashflow_document_number_expense,
            id.value
        )
        is DocDto.Receipt.Draft -> receiptNumber.orEmpty()
        is DocDto.CreditNote -> creditNoteNumber.orEmpty()
        is DocDto.BankStatement -> ""
        is DocDto.ProForma -> ""
        is DocDto.Quote -> ""
        is DocDto.PurchaseOrder -> ""
        is DocDto.ClassifiedDoc -> ""
    }

    val hasAlert = when (this) {
        is DocDto.Invoice.Confirmed -> status == InvoiceStatus.Sent || status == InvoiceStatus.Overdue
        is DocDto.Invoice.Draft -> false
        is DocDto.Receipt -> false
        is DocDto.CreditNote -> false
        is DocDto.BankStatement -> false
        is DocDto.ClassifiedDoc -> false
    }

    val docCurrency = this.currency
    val docAmount = this.totalAmount

    // Format amount with comma separator (fallback to display string on parse failure)
    val formattedNumber = docAmount?.let { amount ->
        runCatching {
            val amountValue = amount.toDouble()
            val intAmount = amountValue.toInt()
            intAmount.toString().replace(AmountGroupingRegex, "$1,")
        }.getOrNull()
    }

    val formattedAmount = stringResource(
        Res.string.cashflow_amount_with_currency,
        docCurrency.displaySign,
        formattedNumber ?: docAmount?.formatAmount().orEmpty()
    )

    val docId = when (this) {
        is DocDto.Invoice.Confirmed -> documentId
        is DocDto.Receipt.Confirmed -> documentId
        is DocDto.CreditNote.Confirmed -> documentId
        else -> null
    }

    val docSortDate = this.sortDate

    return FinancialDocumentRow(
        id = docId,
        invoiceNumber = documentNumber,
        contactName = contactName,
        contactEmail = contactEmail,
        amount = formattedAmount,
        date = docSortDate?.let { formatDate(it) }.orEmpty(),
        cashflowType = cashflowType,
        hasAlert = hasAlert
    )
}

/**
 * Returns a stable key for a DocDto, used for Compose `key` blocks.
 */
internal fun DocDto.stableKey(): Any = when (this) {
    is DocDto.Invoice.Confirmed -> id
    is DocDto.Receipt.Confirmed -> id
    is DocDto.CreditNote.Confirmed -> id
    is DocDto.BankStatement.Confirmed -> accountIban ?: this
    is DocDto.Invoice.Draft -> this
    is DocDto.Receipt.Draft -> this
    is DocDto.CreditNote.Draft -> this
    is DocDto.BankStatement.Draft -> this
    is DocDto.ClassifiedDoc -> this
}

/**
 * Formats a LocalDate to display format (e.g., "May 25, 2024").
 */
@Composable
private fun formatDate(date: LocalDate): String {
    val months = listOf(
        stringResource(Res.string.date_month_long_january),
        stringResource(Res.string.date_month_long_february),
        stringResource(Res.string.date_month_long_march),
        stringResource(Res.string.date_month_long_april),
        stringResource(Res.string.date_month_long_may),
        stringResource(Res.string.date_month_long_june),
        stringResource(Res.string.date_month_long_july),
        stringResource(Res.string.date_month_long_august),
        stringResource(Res.string.date_month_long_september),
        stringResource(Res.string.date_month_long_october),
        stringResource(Res.string.date_month_long_november),
        stringResource(Res.string.date_month_long_december),
    )
    return stringResource(
        Res.string.date_format_long,
        months[date.month.ordinal],
        date.day,
        date.year
    )
}

/**
 * Table component for displaying financial documents with columns:
 * Invoice, Contact, Amount, Date, Type, and Actions.
 *
 * @param documents List of financial documents to display
 * @param onDocumentClick Callback when a document row is clicked
 * @param onMoreClick Callback when the more menu button is clicked for a document
 * @param modifier Optional modifier for the table
 */
@Composable
fun FinancialDocumentTable(
    documents: List<DocDto>,
    onDocumentClick: (DocDto) -> Unit,
    onMoreClick: (DocDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizing = MaterialTheme.dokusSizing
    DokusCardSurface(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header Row
            FinancialDocumentTableHeader()

            // Document Rows
            documents.forEachIndexed { index, document ->
                key(document.stableKey()) {
                    FinancialDocumentTableRow(
                        row = document.toTableRow(),
                        onClick = { onDocumentClick(document) },
                        onMoreClick = { onMoreClick(document) }
                    )

                    // Add divider between rows (not after the last item)
                    if (index < documents.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = sizing.strokeThin
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header row for the financial document table.
 */
@Composable
private fun FinancialDocumentTableHeader(
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.dokusSpacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = spacing.xLarge, vertical = spacing.large),
        horizontalArrangement = Arrangement.spacedBy(spacing.xLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Invoice column
        Text(
            text = stringResource(Res.string.document_table_invoice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(spacing.large * 8.75f)
        )

        // Contact column (grows to fill space)
        Text(
            text = stringResource(Res.string.document_table_contact),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // Amount column
        Text(
            text = stringResource(Res.string.document_table_amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(spacing.large * 6.25f)
        )

        // Date column
        Text(
            text = stringResource(Res.string.document_table_date),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(spacing.large * 7.5f)
        )

        // Type column (moved to second line based on Figma)
        Spacer(modifier = Modifier.width(spacing.large * 6.25f))

        // Actions column
        Spacer(modifier = Modifier.width(spacing.large * 4.5f))
    }
}
