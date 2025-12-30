package ai.dokus.app.cashflow.components

import ai.dokus.app.resources.generated.Res
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.model.FinancialDocumentDto
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun InvoiceCard(
    invoice: FinancialDocumentDto.InvoiceDto,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Invoice number and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = invoice.invoiceNumber.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Status badge
                InvoiceStatusBadge(status = invoice.status)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Amount
            Text(
                text = invoice.totalAmount.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Due date
            Text(
                text = stringResource(Res.string.invoice_due_prefix) + " " + invoice.dueDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InvoiceStatusBadge(
    status: InvoiceStatus,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (status) {
        InvoiceStatus.Draft -> MaterialTheme.colorScheme.outline to stringResource(Res.string.invoice_status_draft)
        InvoiceStatus.Sent -> MaterialTheme.colorScheme.primary to stringResource(Res.string.invoice_status_sent)
        InvoiceStatus.Paid -> MaterialTheme.colorScheme.tertiary to stringResource(Res.string.invoice_status_paid)
        InvoiceStatus.Overdue -> MaterialTheme.colorScheme.error to stringResource(Res.string.invoice_status_overdue)
        InvoiceStatus.Cancelled -> MaterialTheme.colorScheme.onSurfaceVariant to stringResource(Res.string.invoice_status_cancelled)
        else -> MaterialTheme.colorScheme.onSurfaceVariant to status.name
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}
