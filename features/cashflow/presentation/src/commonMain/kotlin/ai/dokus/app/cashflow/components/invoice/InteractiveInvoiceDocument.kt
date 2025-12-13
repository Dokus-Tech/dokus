package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceUiState
import ai.dokus.app.cashflow.viewmodel.InvoiceLineItem
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate

/**
 * Interactive invoice document that looks like a real invoice.
 * All elements are clickable and editable inline.
 *
 * This is a WYSIWYG editor - what you see is what the final invoice looks like.
 */
@Composable
fun InteractiveInvoiceDocument(
    formState: CreateInvoiceFormState,
    uiState: CreateInvoiceUiState,
    onClientClick: () -> Unit,
    onIssueDateClick: () -> Unit,
    onDueDateClick: () -> Unit,
    onItemClick: (String) -> Unit,
    onItemCollapse: () -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (String) -> Unit,
    onUpdateItemDescription: (String, String) -> Unit,
    onUpdateItemQuantity: (String, Double) -> Unit,
    onUpdateItemUnitPrice: (String, String) -> Unit,
    onUpdateItemVatRate: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Invoice header
            InvoiceDocumentHeader()

            // Invoice content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Client section - clickable
                InvoiceClientSection(
                    client = formState.selectedClient,
                    showPeppolWarning = formState.showPeppolWarning,
                    onClick = onClientClick
                )

                // Dates section - clickable
                InvoiceDatesSection(
                    issueDate = formState.issueDate,
                    dueDate = formState.dueDate,
                    onIssueDateClick = onIssueDateClick,
                    onDueDateClick = onDueDateClick
                )

                // Divider before items
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Line items table with expandable rows
                InvoiceLineItemsTable(
                    items = formState.items,
                    expandedItemId = uiState.expandedItemId,
                    onItemClick = onItemClick,
                    onItemCollapse = onItemCollapse,
                    onAddItem = onAddItem,
                    onRemoveItem = onRemoveItem,
                    onUpdateDescription = onUpdateItemDescription,
                    onUpdateQuantity = onUpdateItemQuantity,
                    onUpdateUnitPrice = onUpdateItemUnitPrice,
                    onUpdateVatRate = onUpdateItemVatRate
                )

                // Divider after items
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Totals section
                InvoiceTotalsSection(
                    subtotal = formState.subtotal,
                    vatAmount = formState.vatAmount,
                    total = formState.total
                )
            }
        }
    }
}

/**
 * Invoice header with INVOICE title and styling.
 */
@Composable
private fun InvoiceDocumentHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "INVOICE",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                letterSpacing = 2.sp
            )
            Text(
                text = "DRAFT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Totals section showing subtotal, VAT, and total.
 */
@Composable
private fun InvoiceTotalsSection(
    subtotal: String,
    vatAmount: String,
    total: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TotalRow(label = "Subtotal", value = subtotal)
        TotalRow(label = "VAT", value = vatAmount)

        Spacer(modifier = Modifier.height(4.dp))

        // Dashed divider
        DashedDivider()

        Spacer(modifier = Modifier.height(4.dp))

        // Total amount - highlighted
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TOTAL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 1.sp
            )
            Text(
                text = total,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DashedDivider(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(30) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}
