package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

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
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceUiState
import tech.dokus.foundation.aura.components.DokusCardSurface
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

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

@Preview
@Composable
private fun InteractiveInvoiceDocumentPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        InteractiveInvoiceDocument(
            formState = Mocks.sampleFormState,
            uiState = Mocks.sampleUiState,
            onClientClick = {},
            onIssueDateClick = {},
            onDueDateClick = {},
            onItemClick = {},
            onItemCollapse = {},
            onAddItem = {},
            onRemoveItem = {},
            onUpdateItemDescription = { _, _ -> },
            onUpdateItemQuantity = { _, _ -> },
            onUpdateItemUnitPrice = { _, _ -> },
            onUpdateItemVatRate = { _, _ -> }
        )
    }
}
