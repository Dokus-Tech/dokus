package tech.dokus.features.ai.models

import kotlin.test.Test
import kotlin.test.assertTrue

class ExtractionToolDescriptionsTest {

    @Test
    fun `all local date descriptions require ISO output format`() {
        val dateDescriptions = listOf(
            ExtractionToolDescriptions.IssueDate,
            ExtractionToolDescriptions.DueDate,
            ExtractionToolDescriptions.ValidUntil,
            ExtractionToolDescriptions.OrderDate,
            ExtractionToolDescriptions.ExpectedDeliveryDate,
            ExtractionToolDescriptions.ReceiptDate,
            ExtractionToolDescriptions.BankTransactionDate,
            ExtractionToolDescriptions.BankPeriodStart,
            ExtractionToolDescriptions.BankPeriodEnd,
        )

        dateDescriptions.forEach { description ->
            assertTrue(description.contains("YYYY-MM-DD"), description)
            assertTrue(description.contains("2026-01-20"), description)
        }
    }

    @Test
    fun `shared date guidance clarifies document formats versus tool output`() {
        assertTrue(ExtractionToolDescriptions.LocalDateToolOutputGuidance.contains("tool"))
        assertTrue(ExtractionToolDescriptions.LocalDateToolOutputGuidance.contains("20/01/2026"))
        assertTrue(ExtractionToolDescriptions.LocalDateToolOutputGuidance.contains("01/20/2026"))

        assertTrue(ExtractionToolDescriptions.DocumentDateFormatClarification.contains("document"))
        assertTrue(ExtractionToolDescriptions.DocumentDateFormatClarification.contains("tool output"))
        assertTrue(ExtractionToolDescriptions.DocumentDateFormatClarification.contains("YYYY-MM-DD"))
    }
}
