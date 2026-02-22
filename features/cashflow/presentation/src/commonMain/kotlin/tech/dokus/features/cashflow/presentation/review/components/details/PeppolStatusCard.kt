package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState

@Composable
internal fun PeppolStatusCard(
    state: DocumentReviewState.Content,
    modifier: Modifier = Modifier
) {
    val invoice = state.document.confirmedEntity as? FinancialDocumentDto.InvoiceDto ?: return
    val shouldShow = invoice.peppolStatus != null ||
        invoice.peppolSentAt != null ||
        invoice.peppolId != null
    if (!shouldShow) return

    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = "PEPPOL Status")

        FactField(
            label = "Transmission",
            value = invoice.peppolStatus?.displayLabel() ?: "Not sent"
        )

        invoice.peppolId?.let { peppolId ->
            FactField(
                label = "Recipient PEPPOL ID",
                value = peppolId.toString()
            )
        }

        invoice.peppolSentAt?.let { sentAt ->
            FactField(
                label = "Last update",
                value = sentAt.toString()
            )
        }
    }
}

private fun PeppolStatus.displayLabel(): String = when (this) {
    PeppolStatus.Pending -> "Pending"
    PeppolStatus.Sent -> "Sent"
    PeppolStatus.Delivered -> "Delivered"
    PeppolStatus.Failed -> "Failed"
    PeppolStatus.Rejected -> "Rejected"
}
