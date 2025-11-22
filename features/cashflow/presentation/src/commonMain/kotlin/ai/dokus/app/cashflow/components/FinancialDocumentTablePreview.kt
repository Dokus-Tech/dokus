package ai.dokus.app.cashflow.components

import ai.dokus.foundation.design.tooling.PreviewParameters
import ai.dokus.foundation.design.tooling.PreviewParametersProvider
import ai.dokus.foundation.design.tooling.TestWrapper
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.ids.InvoiceId
import ai.dokus.foundation.domain.ids.InvoiceNumber
import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.model.FinancialDocument
import ai.dokus.foundation.domain.model.FinancialDocumentStatus
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.ui.tooling.preview.PreviewParameter

/**
 * Preview for FinancialDocumentTable component.
 */
@Preview
@Composable
fun FinancialDocumentTablePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        FinancialDocumentTable(
            documents = getSampleFinancialDocuments(),
            onDocumentClick = {},
            onMoreClick = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        )
    }
}

/**
 * Generates sample financial documents for preview.
 */
@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
fun getSampleFinancialDocuments(): List<FinancialDocument> {
    val now = LocalDateTime(2024, 5, 25, 12, 0)
    val date = LocalDate(2024, 5, 25)

    return listOf(
        // Invoice with alert
        FinancialDocument.InvoiceDocument(
            documentId = "1",
            organizationId = OrganizationId.generate(),
            documentNumber = "INV-3006-4400",
            date = date,
            amount = Money("1500.00"),
            currency = Currency.Eur,
            status = FinancialDocumentStatus.PendingApproval,
            description = null,
            createdAt = now,
            updatedAt = now,
            invoiceId = InvoiceId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4400"),
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            paidAmount = Money.ZERO,
            items = emptyList()
        ),
        // Invoice without alert (Cash-Out)
        FinancialDocument.InvoiceDocument(
            documentId = "2",
            organizationId = OrganizationId.generate(),
            documentNumber = "INV-3006-4400",
            date = date,
            amount = Money("1500.00"),
            currency = Currency.Eur,
            status = FinancialDocumentStatus.Approved,
            description = null,
            createdAt = now,
            updatedAt = now,
            invoiceId = InvoiceId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4400"),
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            paidAmount = Money.ZERO,
            items = emptyList()
        ),
        // More sample documents...
        FinancialDocument.InvoiceDocument(
            documentId = "3",
            organizationId = OrganizationId.generate(),
            documentNumber = "INV-3006-4400",
            date = date,
            amount = Money("1500.00"),
            currency = Currency.Eur,
            status = FinancialDocumentStatus.PendingApproval,
            description = null,
            createdAt = now,
            updatedAt = now,
            invoiceId = InvoiceId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4400"),
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            paidAmount = Money.ZERO,
            items = emptyList()
        ),
        FinancialDocument.InvoiceDocument(
            documentId = "4",
            organizationId = OrganizationId.generate(),
            documentNumber = "INV-3006-4400",
            date = date,
            amount = Money("1500.00"),
            currency = Currency.Eur,
            status = FinancialDocumentStatus.Approved,
            description = null,
            createdAt = now,
            updatedAt = now,
            invoiceId = InvoiceId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4400"),
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            paidAmount = Money.ZERO,
            items = emptyList()
        ),
        FinancialDocument.InvoiceDocument(
            documentId = "5",
            organizationId = OrganizationId.generate(),
            documentNumber = "INV-3006-4400",
            date = date,
            amount = Money("1500.00"),
            currency = Currency.Eur,
            status = FinancialDocumentStatus.Approved,
            description = null,
            createdAt = now,
            updatedAt = now,
            invoiceId = InvoiceId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4400"),
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            paidAmount = Money.ZERO,
            items = emptyList()
        ),
        FinancialDocument.InvoiceDocument(
            documentId = "6",
            organizationId = OrganizationId.generate(),
            documentNumber = "INV-3006-4400",
            date = date,
            amount = Money("1500.00"),
            currency = Currency.Eur,
            status = FinancialDocumentStatus.PendingApproval,
            description = null,
            createdAt = now,
            updatedAt = now,
            invoiceId = InvoiceId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4400"),
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            paidAmount = Money.ZERO,
            items = emptyList()
        )
    )
}
