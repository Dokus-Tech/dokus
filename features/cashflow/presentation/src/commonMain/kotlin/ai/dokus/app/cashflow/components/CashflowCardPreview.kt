package ai.dokus.app.cashflow.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Preview/example implementation of the CashflowCard component.
 * This demonstrates the component with sample data matching the Figma design.
 */
@Composable
fun CashflowCardPreview() {
    val sampleItems = listOf(
        CashflowItemData(
            invoiceNumber = "INVOICE 011",
            statusText = "Need confirmation"
        ),
        CashflowItemData(
            invoiceNumber = "INVOICE 011X",
            statusText = "Need confirmation"
        ),
        CashflowItemData(
            invoiceNumber = "INVOICE 01121CV",
            statusText = "Need confirmation"
        ),
        CashflowItemData(
            invoiceNumber = "INVOICE 881120-12",
            statusText = "Need confirmation"
        )
    )

    CashflowCard(
        items = sampleItems,
        onPreviousClick = { /* Handle previous page */ },
        onNextClick = { /* Handle next page */ },
        modifier = Modifier.padding(16.dp)
    )
}
