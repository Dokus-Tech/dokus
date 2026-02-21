package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_due_prefix
import tech.dokus.aura.resources.invoice_status_cancelled
import tech.dokus.aura.resources.invoice_status_draft
import tech.dokus.aura.resources.invoice_status_overdue
import tech.dokus.aura.resources.invoice_status_paid
import tech.dokus.aura.resources.invoice_status_sent
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.InvoiceNumber
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun InvoiceCard(
    invoice: FinancialDocumentDto.InvoiceDto,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier,
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
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
        InvoiceStatus.Cancelled ->
            MaterialTheme.colorScheme.onSurfaceVariant to stringResource(Res.string.invoice_status_cancelled)
        else -> MaterialTheme.colorScheme.onSurfaceVariant to status.name
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@Preview
@Composable
private fun InvoiceCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val now = LocalDateTime(2024, 12, 13, 12, 0)
    TestWrapper(parameters) {
        InvoiceCard(
            invoice = FinancialDocumentDto.InvoiceDto(
                id = InvoiceId.generate(),
                tenantId = TenantId.generate(),
                contactId = ContactId.generate(),
                invoiceNumber = InvoiceNumber("INV-001"),
                issueDate = kotlinx.datetime.LocalDate(2024, 12, 13),
                dueDate = kotlinx.datetime.LocalDate(2025, 1, 13),
                subtotalAmount = Money(340000),
                vatAmount = Money(71400),
                totalAmount = Money(411400),
                status = InvoiceStatus.Draft,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
