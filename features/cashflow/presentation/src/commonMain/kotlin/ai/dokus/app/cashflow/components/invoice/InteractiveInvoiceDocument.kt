package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceUiState
import tech.dokus.foundation.aura.components.DokusCardSurface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// A4 aspect ratio: 210mm × 297mm = 1:√2 ≈ 0.707
private const val A4_ASPECT_RATIO = 0.707f

// A4-like proportions with slightly wider width for better readability
private val MAX_PAPER_WIDTH = 680.dp

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
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        // Calculate paper width - use available width but cap at A4 max
        val paperWidth = minOf(maxWidth, MAX_PAPER_WIDTH)
        // Calculate minimum height based on A4 ratio (content can exceed this)
        val minPaperHeight = paperWidth / A4_ASPECT_RATIO

        DokusCardSurface(
            modifier = Modifier
                .widthIn(max = paperWidth)
                .padding(vertical = 16.dp),
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
                        .padding(16.dp),
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
}
