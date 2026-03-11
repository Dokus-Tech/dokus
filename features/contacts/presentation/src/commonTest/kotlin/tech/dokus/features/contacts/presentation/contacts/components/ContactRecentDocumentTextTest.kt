package tech.dokus.features.contacts.presentation.contacts.components

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.features.contacts.usecases.ContactRecentInvoice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContactRecentDocumentTextTest {

    @Test
    fun `summary becomes primary and reference becomes secondary`() {
        val text = resolveRecentDocumentText(
            document = recentInvoice(
                summary = "Cloud credits for February",
                reference = "INV-001"
            ),
            unknownLabel = "Unknown"
        )

        assertEquals("Cloud credits for February", text.primary)
        assertEquals("INV-001", text.secondary)
    }

    @Test
    fun `reference becomes primary when summary is missing`() {
        val text = resolveRecentDocumentText(
            document = recentInvoice(
                summary = null,
                reference = "INV-002"
            ),
            unknownLabel = "Unknown"
        )

        assertEquals("INV-002", text.primary)
        assertNull(text.secondary)
    }

    @Test
    fun `secondary line is omitted when summary and reference match`() {
        val text = resolveRecentDocumentText(
            document = recentInvoice(
                summary = "Invoice 003",
                reference = "invoice 003"
            ),
            unknownLabel = "Unknown"
        )

        assertEquals("Invoice 003", text.primary)
        assertNull(text.secondary)
    }
}

private fun recentInvoice(
    summary: String?,
    reference: String?,
): ContactRecentInvoice {
    return ContactRecentInvoice(
        invoiceId = InvoiceId.parse("00000000-0000-0000-0000-000000000099"),
        documentId = null,
        issueDate = LocalDate(2026, 2, 1),
        updatedAt = LocalDateTime(2026, 2, 1, 10, 0),
        direction = DocumentDirection.Inbound,
        status = InvoiceStatus.Draft,
        totalAmount = Money(12100),
        outstandingAmount = Money(12100),
        summary = summary,
        reference = reference
    )
}
