package ai.dokus.app.cashflow.components

import ai.dokus.foundation.design.components.CashflowType
import ai.dokus.foundation.design.components.CashflowTypeBadge
import ai.dokus.foundation.domain.model.FinancialDocument
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate

/**
 * Data class representing a financial document table row.
 * This maps from FinancialDocument domain model to UI-specific structure.
 */
data class FinancialDocumentRow(
    val id: String,
    val invoiceNumber: String,
    val contactName: String,
    val contactEmail: String,
    val amount: String,
    val date: String,
    val cashflowType: CashflowType,
    val hasAlert: Boolean = false
)

/**
 * Converts a FinancialDocument to a FinancialDocumentRow for display.
 */
fun FinancialDocument.toTableRow(): FinancialDocumentRow {
    val cashflowType = when (this) {
        is FinancialDocument.InvoiceDocument -> CashflowType.CashIn
        is FinancialDocument.ExpenseDocument -> CashflowType.CashOut
    }

    val contactName = when (this) {
        is FinancialDocument.InvoiceDocument -> "Name Surname" // TODO: Get from client
        is FinancialDocument.ExpenseDocument -> this.merchant
    }

    val contactEmail = when (this) {
        is FinancialDocument.InvoiceDocument -> "mailname@email.com" // TODO: Get from client
        is FinancialDocument.ExpenseDocument -> ""
    }

    // Format amount with comma separator
    val formattedAmount = try {
        val amountValue = amount.value.toDoubleOrNull() ?: 0.0
        val intAmount = amountValue.toInt()
        "€${intAmount.toString().replace(Regex("(\\d)(?=(\\d{3})+$)"), "$1,")}"
    } catch (e: Exception) {
        "€${amount.value}"
    }

    return FinancialDocumentRow(
        id = documentId,
        invoiceNumber = documentNumber,
        contactName = contactName,
        contactEmail = contactEmail,
        amount = formattedAmount,
        date = formatDate(date),
        cashflowType = cashflowType,
        hasAlert = status == ai.dokus.foundation.domain.model.FinancialDocumentStatus.PendingApproval
    )
}

/**
 * Formats a LocalDate to display format (e.g., "May 25, 2024").
 */
private fun formatDate(date: LocalDate): String {
    val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    return "${months[date.month.ordinal]} ${date.dayOfMonth}, ${date.year}"
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
    documents: List<FinancialDocument>,
    onDocumentClick: (FinancialDocument) -> Unit,
    onMoreClick: (FinancialDocument) -> Unit,
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
            text = "Invoice",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )

        // Contact column (grows to fill space)
        Text(
            text = "Contact",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        // Amount column
        Text(
            text = "Amount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )

        // Date column
        Text(
            text = "Date",
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
                        .background(Color(0xFFEF5350)) // Red alert indicator
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
                    .background(Color(0xFFE3F2FD)), // Light blue background
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF2196F3), // Blue icon
                    modifier = Modifier.size(20.dp)
                )
            }

            // Name and email
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = row.contactName,
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
                    contentDescription = "More options",
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
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
