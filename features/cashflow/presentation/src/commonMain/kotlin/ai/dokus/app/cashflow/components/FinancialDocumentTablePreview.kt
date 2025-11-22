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
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.model.FinancialDocumentDto
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
fun getSampleFinancialDocuments(): List<FinancialDocumentDto> {
    val now = LocalDateTime(2024, 5, 25, 12, 0)
    val date = LocalDate(2024, 5, 25)

    return listOf(
        // Invoice with alert (Sent status)
        FinancialDocumentDto.InvoiceDto(
            id = InvoiceId.generate(),
            organizationId = OrganizationId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4400"),
            issueDate = date,
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            totalAmount = Money("1500.00"),
            paidAmount = Money.ZERO,
            status = InvoiceStatus.Sent,
            currency = Currency.Eur,
            notes = null,
            termsAndConditions = null,
            items = emptyList(),
            createdAt = now,
            updatedAt = now
        ),
        // Invoice without alert (Paid status)
        FinancialDocumentDto.InvoiceDto(
            id = InvoiceId.generate(),
            organizationId = OrganizationId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4401"),
            issueDate = date,
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            totalAmount = Money("1500.00"),
            paidAmount = Money("1500.00"),
            status = InvoiceStatus.Paid,
            currency = Currency.Eur,
            notes = null,
            termsAndConditions = null,
            items = emptyList(),
            createdAt = now,
            updatedAt = now
        ),
        // Invoice with Overdue status
        FinancialDocumentDto.InvoiceDto(
            id = InvoiceId.generate(),
            organizationId = OrganizationId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4402"),
            issueDate = date,
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            totalAmount = Money("1500.00"),
            paidAmount = Money.ZERO,
            status = InvoiceStatus.Overdue,
            currency = Currency.Eur,
            notes = null,
            termsAndConditions = null,
            items = emptyList(),
            createdAt = now,
            updatedAt = now
        ),
        // Invoice with Draft status
        FinancialDocumentDto.InvoiceDto(
            id = InvoiceId.generate(),
            organizationId = OrganizationId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4403"),
            issueDate = date,
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            totalAmount = Money("1500.00"),
            paidAmount = Money.ZERO,
            status = InvoiceStatus.Draft,
            currency = Currency.Eur,
            notes = null,
            termsAndConditions = null,
            items = emptyList(),
            createdAt = now,
            updatedAt = now
        ),
        // Invoice with Viewed status
        FinancialDocumentDto.InvoiceDto(
            id = InvoiceId.generate(),
            organizationId = OrganizationId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4404"),
            issueDate = date,
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            totalAmount = Money("1500.00"),
            paidAmount = Money.ZERO,
            status = InvoiceStatus.Viewed,
            currency = Currency.Eur,
            notes = null,
            termsAndConditions = null,
            items = emptyList(),
            createdAt = now,
            updatedAt = now
        ),
        // Invoice with PartiallyPaid status
        FinancialDocumentDto.InvoiceDto(
            id = InvoiceId.generate(),
            organizationId = OrganizationId.generate(),
            clientId = ClientId.generate(),
            invoiceNumber = InvoiceNumber("INV-3006-4405"),
            issueDate = date,
            dueDate = date,
            subtotalAmount = Money("1240.00"),
            vatAmount = Money("260.00"),
            totalAmount = Money("1500.00"),
            paidAmount = Money("750.00"),
            status = InvoiceStatus.PartiallyPaid,
            currency = Currency.Eur,
            notes = null,
            termsAndConditions = null,
            items = emptyList(),
            createdAt = now,
            updatedAt = now
        )
    )
}
