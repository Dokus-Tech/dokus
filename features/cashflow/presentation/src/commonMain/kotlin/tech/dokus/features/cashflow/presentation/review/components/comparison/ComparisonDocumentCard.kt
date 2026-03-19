package tech.dokus.features.cashflow.presentation.review.components.comparison

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.dokus.features.cashflow.presentation.review.components.CanonicalInvoiceDocumentCard
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData

/**
 * Renders the appropriate canonical card for a document in comparison view.
 * Reuses existing canonical card components (Invoice, CreditNote).
 */
@Composable
internal fun ComparisonDocumentCard(
    uiData: DocumentUiData,
    counterpartyName: String,
    modifier: Modifier = Modifier,
) {
    when (uiData) {
        is DocumentUiData.Invoice -> {
            CanonicalInvoiceDocumentCard(
                data = uiData,
                counterpartyName = counterpartyName,
                counterpartyAddress = null,
                modifier = modifier,
            )
        }
        // Other canonical types can be added here as they get canonical cards
        else -> {
            // Non-canonical types can't be rendered in comparison view
        }
    }
}
