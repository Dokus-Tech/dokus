package ai.dokus.app.cashflow.components

import ai.dokus.app.resources.generated.Res
import ai.dokus.foundation.design.components.CashflowType
import ai.dokus.foundation.design.components.CashflowTypeBadge
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.FinancialDocumentDto
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * Data class representing a financial document table row.
 * This maps from FinancialDocumentDto domain model to UI-specific structure.
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
 * Converts a FinancialDocumentDto to a FinancialDocumentRow for display.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Composable
fun FinancialDocumentDto.toTableRow(): FinancialDocumentRow {
    val cashflowType = when (this) {
        is FinancialDocumentDto.InvoiceDto -> CashflowType.CashIn
        is FinancialDocumentDto.ExpenseDto -> CashflowType.CashOut
        is FinancialDocumentDto.BillDto -> CashflowType.CashOut
    }

    val contactName = when (this) {
        is FinancialDocumentDto.InvoiceDto -> ""
        is FinancialDocumentDto.ExpenseDto -> this.merchant
        is FinancialDocumentDto.BillDto -> this.supplierName
    }

    val contactEmail = when (this) {
        is FinancialDocumentDto.InvoiceDto -> ""
        is FinancialDocumentDto.ExpenseDto -> ""
        is FinancialDocumentDto.BillDto -> ""
    }

    val documentNumber = when (this) {
        is FinancialDocumentDto.InvoiceDto -> invoiceNumber.toString()
        is FinancialDocumentDto.ExpenseDto -> stringResource(
            Res.string.cashflow_document_number_expense,
            id.value
        )
        is FinancialDocumentDto.BillDto -> invoiceNumber ?: stringResource(
            Res.string.cashflow_document_number_bill,
            id.value
        )
    }

    val hasAlert = when (this) {
        is FinancialDocumentDto.InvoiceDto -> status == InvoiceStatus.Sent || status == InvoiceStatus.Overdue
        is FinancialDocumentDto.ExpenseDto -> false // Expenses don't have a status requiring confirmation
        is FinancialDocumentDto.BillDto -> false // Bills don't have a status requiring confirmation (yet)
    }

    // Format amount with comma separator
    val formattedAmount = try {
        val amountValue = amount.toDouble()
        val intAmount = amountValue.toInt()
        val formattedNumber = intAmount.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")
        stringResource(
            Res.string.cashflow_amount_with_currency,
            currency.displaySign,
            formattedNumber
        )
    } catch (e: Exception) {
        stringResource(
            Res.string.cashflow_amount_with_currency,
            currency.displaySign,
            amount.toDisplayString()
        )
    }

    return FinancialDocumentRow(
        id = documentId,
        invoiceNumber = documentNumber,
        contactName = contactName,
        contactEmail = contactEmail,
        amount = formattedAmount,
        date = formatDate(date),
        cashflowType = cashflowType,
        hasAlert = hasAlert
    )
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
    documents: List<FinancialDocumentDto>,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    onMoreClick: (FinancialDocumentDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            // Header Row
            FinancialDocumentTableHeader()

            // Document Rows
            documents.forEachIndexed { index, document ->
                key(document.documentId) {
                    FinancialDocumentTableRow(
                        row = document.toTableRow(),
                        onClick = { onDocumentClick(document) },
                        onMoreClick = { onMoreClick(document) }
                    )

                    // Add divider between rows (not after the last item)
                    if (index < documents.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Invoice column
        Text(
            text = stringResource(Res.string.document_table_invoice),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
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
            modifier = Modifier.width(100.dp)
        )

        // Date column
        Text(
            text = stringResource(Res.string.document_table_date),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )

        // Type column (moved to second line based on Figma)
        Spacer(modifier = Modifier.width(100.dp))

        // Actions column
        Spacer(modifier = Modifier.width(72.dp))
    }
}

/**
 * Data row for the financial document table.
 */
@Composable
private fun FinancialDocumentTableRow(
    row: FinancialDocumentRow,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Invoice column with alert indicator
        Row(
            modifier = Modifier.width(140.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Alert dot if needed
            if (row.hasAlert) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error) // Red alert indicator
                )
            }

            Text(
                text = row.invoiceNumber,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Contact column with avatar and info
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer), // Light background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer, // Icon color
                    modifier = Modifier.size(20.dp)
                )
            }

            // Name and email
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                text = row.contactName.ifBlank {
                    stringResource(Res.string.common_unknown)
                },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (row.contactEmail.isNotEmpty()) {
                    Text(
                        text = row.contactEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Amount column
        Text(
            text = row.amount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp)
        )

        // Date column
        Column(
            modifier = Modifier.width(120.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = row.date,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Type badge
        Box(
            modifier = Modifier.width(100.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            CashflowTypeBadge(type = row.cashflowType)
        }

        // Actions column
        Row(
            modifier = Modifier.width(72.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // More menu button
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(Res.string.document_table_more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron right button
            IconButton(
                onClick = onClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(Res.string.document_table_view_details),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Mobile-friendly list component for displaying financial documents.
 * Shows simplified rows with Invoice #, Amount, and Type.
 *
 * @param documents List of financial documents to display
 * @param onDocumentClick Callback when a document row is clicked
 * @param modifier Optional modifier for the list
 */
@Composable
fun FinancialDocumentList(
    documents: List<FinancialDocumentDto>,
    onDocumentClick: (FinancialDocumentDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            documents.forEachIndexed { index, document ->
                key(document.documentId) {
                    FinancialDocumentListItem(
                        row = document.toTableRow(),
                        onClick = { onDocumentClick(document) }
                    )

                    if (index < documents.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mobile-friendly list item showing simplified document info.
 * Displays: Invoice # | Amount | Type badge
 */
@Composable
private fun FinancialDocumentListItem(
    row: FinancialDocumentRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side: Invoice number with optional alert
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (row.hasAlert) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                )
            }

            Text(
                text = row.invoiceNumber,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Amount
        Text(
            text = row.amount,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Type badge
        CashflowTypeBadge(type = row.cashflowType)

        Spacer(modifier = Modifier.width(8.dp))

        // Chevron
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = stringResource(Res.string.document_table_view_details),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
